package src.JSQL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSQLParser {
	
	private static final String KEYWORD_REGEX = "(?<!\\\\)\\(|(?<!\\S)select|from|where|order by|limit(?!\\S)";
	
	// keyword , precedence
	private static final Object[][] SELECT_STRUCTURE = {
		{"select",0},
		{"from",1},
		{"where",2},
		{"order by",3},
		{"limit",3}
	};
	
	private static final Object[][] UPDATE_STRUCTURE = {
		{"update",0},
		{"set value",1},
		{"set",1},
		{"where",2},
		{"order by",3},
		{"limit",3}
	};
	
	private static final Object[][] INSERT_STRUCTURE = {
		{"insert into",0},
		{"set value",1},
		{"set",1},
		{"where",2},
		{"order by",3},
		{"limit",3}
	};
	
	private static final String[] UNITARYKEYS = {};
	
	private static final String SELECT = "select";
	private static final String UPDATE = "update";
	private static final String INSERT = "insert into";
	private static final String FROM = "from";
	private static final String SET_VALUE = "set value";
	private static final String SET = "set";
	private static final String WHERE = "where";
	private static final String ORDER_BY = "order by";
	private static final String LIMIT = "limit";
	
	private static final String ROOT_KEY = "root";
	private static final String EMPTY = "";
	private static final String QUERY_SEPARATOR = ",";
	private static final String NOT_OPERATOR = "!";
	private static final String ALL_OPERATOR = "*";
	private static final String ANY_OPERATOR = "?";
	private static final String CHILD_OPERATOR = ":";
	private static final String PATH_DELIMETER = ".";
	private static final String CLAUSE_DELIMETER = "'";
	private static final String SPACE = " ";
	private static final String NOT_BACKSLASH = "(?<!\\\\)";
	private static final String PATH_DELIMETER_REGEX = "[.]";
	private static final String DOUBLE_BACKSLASH = "\\";
	private static final String LEFT_BRACKET_ESCAPE = "\\[";
	private static final String RIGHT_BRACKET_ESCAPE = "\\]";
	private static final CharSequence LEFT_BRACKET = "[";
	private static final CharSequence RIGHT_BRACKET = "]";
	private static final String DOUBLE_BACKTICK = "``";
	private static final String DOUBLE_SINGLE_QUOTE = "''";
	
	private static final String[] ESCAPE_CHARS = {".","`"};  // separator
	
	private static final String[] FORBIDDEN_SEQUENCES = {"::","??",":.",".:","!!",",,"};
	
	
	private Object[][] getJsqlCommandStructure(String verb){
		if(verb.startsWith(SELECT)){
			return SELECT_STRUCTURE;
		}else if(verb.startsWith(UPDATE)){
			return UPDATE_STRUCTURE;
		}else if(verb.startsWith(INSERT)){
			return INSERT_STRUCTURE;
		}
		return null;
	}
	
	public int findMatchingParens(StringBuilder str,int index){
		
		String s = str.substring(index);
		Pattern pattern = Pattern.compile(NOT_BACKSLASH+"\\)", Pattern.CASE_INSENSITIVE);
	    Matcher matcher = pattern.matcher(s);
	    
	    while (matcher.find()) {
	    	return index+matcher.start();
	    }
	    return -1;
	}
	
	public int validateKey(int precedence,Object[][] keywords,String key){
		boolean found = false;
		for (Object[] kw : keywords) {
			int kwIndex = key.indexOf((String)kw[0]);
		    if (kwIndex>=0) {
		    	int new_precedence =(Integer)kw[1];
		        if(precedence > new_precedence){
		        	err("Jsql syntax error. Invalid keyword placement: " + key);
		        	return -1;
		        }
		        precedence=new_precedence;
		        found = true;
		        break;
		    }
		}
		if (!found) {
			err("Jsql syntax error. You have an error in your JSQL syntax near: "+ key);
			return -1;
		}
		return precedence;
	}
	
	private JSQLTokenMap<Integer,String> fragmentOnDelimeters(String queryString){
		Pattern pattern = Pattern.compile("``(?!`)|''(?!')|(?<!\\\\)`|(?<!\\\\)'");
	    Matcher matcher = pattern.matcher(queryString);
	    
	    JSQLTokenMap<Integer,String> tokens = new JSQLTokenMap<Integer,String>();
	    
	    String lastFragment = "";
	    int index=0;
	    int lastIndex=0;
	    int group=0;
	    int capture_group=0;
	    int capture_type;
	    boolean release=false;
	    
	    while (matcher.find()) {
	    	String groupStr = matcher.group(0);
	    	//out("captured: " + groupStr + " at index: " + matcher.start());
	    	if(groupStr.equals("''")){
	    		group=1;
	    	}else if(groupStr.equals("``")){
	    		group=2;
	    	}else if(groupStr.equals("'")){
	    		group=3;
	    	}else{
	    		group=4;
	    	}
	    	capture_type=(group%2==0?1:0);  // - 1 path - 0 string variable or other
	    	if(!release||group==capture_group){
		    	index=matcher.start();
		    	String token = queryString.substring(lastIndex,index);
		    	if(capture_type==0){
		    		token.replaceAll(DOUBLE_BACKSLASH+"`", "`");
		    	}
		    	tokens.tokens.add(token);
		    	tokens.type.add(capture_type);
		    	lastIndex=matcher.end();
		    	capture_group=group;
		    	release = !release;
		    // special case 1 (captured ` found \``) or (captured ' found \'')
	    	}else if((group==1&&capture_group==3)||(group==2&&capture_group==4)){  
	    		index=matcher.start()+1;
	    		tokens.tokens.add(queryString.substring(lastIndex,index));
		    	tokens.type.add(capture_type);
		    	lastIndex=index+1;
		    	release = !release;
	    	}
	    }
	    if(release){
	    	err("Jsql syntax error. Delimeter missing");
	    	return null;
	    }
	    if(lastIndex<queryString.length()){
	    	lastFragment = queryString.substring(lastIndex,queryString.length());
	    }else{
	    	lastFragment="";
	    }
	    tokens.tokens.add(lastFragment);
	    tokens.type.add(0);
	    listIt(tokens.tokens,"Parse out delimeters: \n----------------------------- ", "token: ");
	    listIt(tokens.type,"Parse out delimeters: \n----------------------------", "Token type: ");
		return tokens;
	}
	
	private String quotes(int i , JSQLTokenMap<Integer,String> queryStringMap){
		if(queryStringMap.type.get(i)==1){
			return DOUBLE_BACKTICK;
		}else{
			return DOUBLE_SINGLE_QUOTE;
		}
	}
	
	public HashMap<String,String> parseQueryString(String queryString){
		out("Parse queryString:" + queryString+"\n");
		// Split string on double single backtick/quote
		JSQLTokenMap<Integer,String> queryStringMap = fragmentOnDelimeters(queryString);
		
		List<String> tokenList = queryStringMap.tokens;
		
		if(tokenList==null)return null;
		
		// Get the command strucure based on the first keyword
		Object[][] keywords = getJsqlCommandStructure(tokenList.get(0).trim().toLowerCase());
		
		// querystring must start with an acceptable verb
		if(keywords==null){
			err("Jsql syntax error. Invalid verb: " + tokenList.get(0).trim());
			return null;
		}
		
		// reassemble
		
		String[] tokens=tokenList.toArray(new String[tokenList.size()]);
		
		StringBuilder str = new StringBuilder("");
		for (int i = 0; i < tokens.length; i+=2) {
		    str.append(tokens[i]);
		    if(i+2<tokens.length)
		    str.append("``");
			//out(i+": "+tokenList.get(i));
		}
		
		out("Simplified query:\n-----------------------------------");
		out(str.toString());
		out("");
		
	    Pattern pattern = Pattern.compile(KEYWORD_REGEX, Pattern.CASE_INSENSITIVE);
	    Matcher matcher = pattern.matcher(str);
	    
	    HashMap<String,String> tokenMap = new HashMap<String,String>();
	    
	    String lastKey="";
	    String clause = "";
	    int beginClause=0;
	    int ignore_index=0;
	    int precedence = 0;
	   
	    while (matcher.find()) {
	    	String key = matcher.group(0).trim().toLowerCase();
	    	if(matcher.start()>=ignore_index){
		    	if(key.equals("(")){
		    		ignore_index=findMatchingParens(str,matcher.start());
		    	}else{
		    		precedence=validateKey(precedence,keywords,key);
			    	if(precedence==-1){
			    		return null;
			    	}
			    	
			    	if(beginClause!=0){
			    		clause = str.substring(beginClause,matcher.start()).trim();
			    		if(!clause.equals(EMPTY)||contains(UNITARYKEYS,lastKey)){
			    			tokenMap.put(lastKey, clause);
			    		}else{
			    	    	err("You have an error in your JSQL syntax near "+lastKey);
			    	    	return null;
			    	    }
			    	}
			    	
			    	lastKey=key;
			    	beginClause =  matcher.end();
		    	}
	    	}
	    }
	    if(beginClause<str.length()){
	    	clause = str.substring(beginClause,str.length()).trim();
	    }else{
	    	clause="";
	    }
	    if(!clause.equals(EMPTY)||contains(UNITARYKEYS,lastKey)){
			tokenMap.put(lastKey, clause);
	    }else{
	    	err("You have an error in your JSQL syntax near "+lastKey);
	    	return null;
	    }
		// Reinsert the quoted text
		
		int i = 1;
	    for (Entry<String, String> entry : tokenMap.entrySet()) {
	    	
    		clause = entry.getValue();
    		
    		pattern = Pattern.compile("``(?!`)");
    	    matcher = pattern.matcher(clause);
    	    StringBuilder newClause = new StringBuilder("");
    	    int index = 0;
    	    while (matcher.find()) {
    	    	index = matcher.end();
    	    	StringBuilder replacement = new StringBuilder();
    	    	replacement.append(quotes(i,queryStringMap));
    	    	replacement.append(tokenList.get(i).replace("\\", "\\\\").replace("$", "\\$"));
    	    	replacement.append(quotes(i,queryStringMap));
    	    	newClause.append(matcher.replaceFirst(replacement.toString()).
    	    			substring(0,index+tokenList.get(i).length()+2));
    	    	clause = clause.substring(index);
    	    	matcher = pattern.matcher(clause);
    	    	i+=2;
    	    }
	    	
	        //out(entry.getKey());
	        newClause.append(clause);
	       // out(newClause);
	        tokenMap.put(entry.getKey(), newClause.toString());
	    }
		out("Finished parsing query.");
		out("");
		//if(1==1)return null;
		return tokenMap;
	}
	
	public ArrayList<JSQLTokens> parseSelector(String queryString){
		//out("start getTokens");
		ArrayList<JSQLTokens> queryList = new ArrayList<JSQLTokens>();
		
		for(int i = 0;i<FORBIDDEN_SEQUENCES.length;i++){
			if(queryString.contains(FORBIDDEN_SEQUENCES[i])){
				err("Jsql syntax error. Forbidden sequence: " + FORBIDDEN_SEQUENCES[i] + ".");
				return queryList;
			}
		}
		
		String[] queries = queryString.split(QUERY_SEPARATOR);
		for(String query:queries){
			JSQLTokens tokens = new JSQLTokens();
			String[] paths = query.split(NOT_OPERATOR);
			int j = 0;
			for(String path:paths){
				//out("getTokens: path:" +path);
				path = path.
						replaceAll(NOT_BACKSLASH+CHILD_OPERATOR,PATH_DELIMETER+CHILD_OPERATOR+PATH_DELIMETER);
				if(path.startsWith(PATH_DELIMETER+CHILD_OPERATOR))path = path.substring(1);
				if(path.endsWith(CHILD_OPERATOR+PATH_DELIMETER))path = path.substring(0,path.length()-1);
				
				
				String[] keys = path.split(NOT_BACKSLASH+PATH_DELIMETER_REGEX,-1);
				int k = 0;
				for(String key:keys){
					//out("getTokens: key:" +key);
					if(key.startsWith("``"));
					key = key.replaceAll("``(?!`)","");
					for(int count=0;count<ESCAPE_CHARS.length;count++){
						key=key.replace(DOUBLE_BACKSLASH+ESCAPE_CHARS[count], ESCAPE_CHARS[count]); // path
					}
					keys[k]=key;
					k++;
				}
				if(j==0){
					tokens.path=keys;
				}else{
					if(tokens.exceptionPaths==null)tokens.exceptionPaths = new ArrayList<String[]>();
					tokens.exceptionPaths.add(keys);
				}
				j++;
			}
			queryList.add(tokens);
		}
		return queryList;
	}
	
	public JSQLTokenMap<Integer,Object> parseExpression(String expression, JSQLExpression evaluator){
		
		Pattern pattern = Pattern.compile("``(?!`)|''(?!')");
	    Matcher matcher = pattern.matcher(expression);
	    
	    JSQLTokenMap<Integer,String> tokenMap = new JSQLTokenMap<Integer,String>();
	    
	    String lastFragment = "";
	    int index=0;
	    int lastIndex=0;
	    int group=0;
	    int capture_group=0;
	    int capture_type;
	    boolean release=false;
	    out("Parsing the expression:\n");
	    while (matcher.find()) {
	    	String groupStr = matcher.group(0);
	    	out("Captured: " + groupStr + " at index: " + matcher.start());
	    	if(groupStr.equals("``")){
	    		group=1;
	    	}else if(groupStr.equals("''")){
	    		group=2;
	    	}
	    	capture_type=(group%2==0?1:0);  // - 1 path - 0 string variable or other
	    	if(!release||group==capture_group){
		    	index=matcher.start();
		    	tokenMap.tokens.add(expression.substring(lastIndex,index));
		    	tokenMap.type.add(capture_type);
		    	lastIndex=matcher.end();
		    	capture_group=group;
		    	release = !release;
	    	}
	    }
	    
	    if(lastIndex<expression.length()){
	    	lastFragment = expression.substring(lastIndex,expression.length());
	    }else{
	    	lastFragment="";
	    }
	    tokenMap.tokens.add(lastFragment);
	    tokenMap.type.add(0);
		
		List<String> tokenList = tokenMap.tokens;
		
		// Get the command strucure based on the first keyword
		
		Object[][] keywords = getJsqlCommandStructure(tokenList.get(0).trim().toLowerCase());
		
		// TODO: nested select
		if(keywords!=null){
			//err("Jsql syntax error. Invalid verb: " + tokenList.get(0).trim());
			//return null;
		}
		
		 JSQLTokenMap<Integer,Object> variablesMap = new JSQLTokenMap<Integer,Object>();
		
		// reassemble simplified expression
		
		String[] tokens=tokenList.toArray(new String[tokenList.size()]);
		
		StringBuilder simplified_expression = new StringBuilder("");
		int count=0;
		for (int i = 0; i < tokens.length; i++) {
			if(i%2==0){
				// Other Parts
			    simplified_expression.append(tokens[i]);
			    if(i+1<tokens.length){
			    	simplified_expression.append(evaluator.dummyVar(count));
					count++;
			    }
			}else{ // Paths and String variables
				variablesMap.tokens.add(tokens[i]);
				variablesMap.type.add(tokenMap.type.get(i));
			}
		}
		
		out("\nSimplified expression: " + simplified_expression);
		
		// Pass it to the evaluator to prepare for evaluation
		
		evaluator.tokenize(simplified_expression.toString(),variablesMap,count);
		
		return variablesMap;

	}
	
	public List<String> parseSelectClause(String expression){
		
		Pattern pattern = Pattern.compile("\\(|,");
	    Matcher matcher = pattern.matcher(expression);
	    
	    List<String> queryList = new ArrayList<String>();
	    
	    String clause = "";
	    int beginClause=0;
	    int ignore_index=0;
	    StringBuilder str = new StringBuilder(expression);
	    
	    while (matcher.find()) {
	    	String key = matcher.group(0).trim().toLowerCase();
	    	if(matcher.start()>=ignore_index){
		    	if(key.equals("(")){
		    		ignore_index=findMatchingParens(str,matcher.start());
		    	}else{
		    		clause = str.substring(beginClause,matcher.start()).trim();
		    		if(!clause.equals(EMPTY)){
		    			queryList.add(clause);
		    		}
			    	beginClause =  matcher.end();
		    	}
	    	}
	    }
	    if(beginClause<str.length()){
	    	clause = str.substring(beginClause,str.length()).trim();
	    }else{
	    	clause="";
	    }
	    if(!clause.equals(EMPTY)){
			queryList.add(clause);
	    }
		return queryList;
	}
	
	private <T> boolean contains(T[] array, T lastKey){
		boolean contains=false;
		for (int i = 0; i < array.length; i++) {
			if(lastKey.equals(array[i])){
				contains=true;
				break;
			}
		}
		return contains;
	}
	
	private void err(Object msg){
		System.out.println(msg);
	}
	
	//TODO delete
	private void out(Object msg){
		System.out.println(msg);
	}
	
	private void out(Object msg, boolean inline){
		System.out.print(msg);
	}
	private void listIt(List list,String start,String loopStr){
		out(start);
		for (Object elem : list) {
			out(loopStr+ " "+ elem.toString());
		}
		out("---------------------------------");
		out("");
	}
}
