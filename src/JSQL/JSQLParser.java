package src.JSQL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JSQLParser {
	
	private static final String KEYWORD_REGEX = "(?<!\\\\)\\(|(?<!\\S)select|from|where|order by|limit(?!\\S)";
	private static final String QUOTATIONS = "``(?!`)|''(?!')|(?<!\\\\)`|(?<!\\\\)'";
	private static final String BRACKETS = "\\[|]";
	private static final String BRACKETS_REGEX = "\\[]";
	private static final String PLACEHOLDER_REGEX = "``(?!`)";
	private static final String PATHQUOTES = "``(?!`)";
	private static final String STRINGQUOTES = "''(?!')";
	private static final String EXPRESSIONQUOTES  = "``(?!`)|(?<!\\\\)\\[|(?<!\\\\)]|''(?!')";
	private static final String SELECT_CLAUSE_REGEX= "``(?!`)|''(?!')|\\[|]|\\)|\\(|,";
	private static final String ASREGEX = "\\[|]|(?<!\\S)AS(?!\\S)";
	
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
	private static final String AS = "AS";
	
	private static final String EMPTY = "";
	private static final String SELECTOR_DELIMITER = ",";
	private static final String STRING_QUOTE = "'";
	private static final String STRING_DOUBLE_QUOTE = "''";
	private static final String KEY_QUOTE = "`";
	private static final String KEY_DOUBLE_QUOTE = "``";
	private static final String PATH_DELIMITER = ".";
	
	private static final String SELECTOR_LEFT_BRACKET = "[";
	private static final String SELECTOR_RIGHT_BRACKET = "]";
	private static final String BRACKETS_PLACEHOLDER = "[]";
	
	private static final String KEY_PLACEHOLDER = "``";
	private static final String KEY_PLACEHOLDER2 = "```";
	
	private static final int PATH= 1;
	private static final int STRING = 0;
	
	private static final String NOT_OPERATOR = "!";
	private static final String ALL_OPERATOR = "*";
	private static final String ANY_OPERATOR = "?";
	private static final String CHILD_OPERATOR = ":";
	private static final String CLAUSE_DELIMITER = "'";
	private static final String SPACE = " ";
	private static final String NOT_BACKSLASH = "(?<!\\\\)";
	private static final String PATH_DELIMITER_REGEX = "[.]";

	private static final String LEFT_PARENS = "(";
	private static final String RIGHT_PARENS = ")";
	private static final String COMMA = ",";
	private static final String RIGHT_PARENS_ESCAPE = "\\)";
	
	private static final String DOUBLE_BACKSLASH = "\\";
	private static final String QUADRUPLE_BACKSLASH = "\\\\";
	private static final String DOLLARSIGN = "$";
	private static final String DOLLARSIGN_BACKSLASH = "\\$";
	
	
	private static final String[] ESCAPE_CHARS = {"`"};  // separator
	
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
		Pattern pattern = Pattern.compile(NOT_BACKSLASH+RIGHT_PARENS_ESCAPE, Pattern.CASE_INSENSITIVE);
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
		        // Check for where with no from in select query
		        out(kw[0]+" " + precedence + " " +new_precedence);
		        if(SELECT.equals((String)keywords[0][0])&&precedence==0&&new_precedence==2){
		        	err("Jsql syntax error. WHERE without FROM.");
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
	
	private JSQLTokenMap<Integer,String> splitOnQuotes(String queryString){
		Pattern pattern = Pattern.compile(QUOTATIONS);
	    Matcher matcher = pattern.matcher(queryString);
	     
	    JSQLTokenMap<Integer,String> tokenMap = new JSQLTokenMap<Integer,String>();
	    
	    Stack<String> stack = new Stack<String>();
	    int lastIndex=0;
	    String clause = EMPTY;
	    String lastToken = "";
	    String groupStr = "";
	    
	    // matching `` '' ' `
	    
	    out("Parsing the expression:\n");
	    while (matcher.find()) {
	    	
	    	groupStr = matcher.group(0);
	    	out("Captured: " + groupStr + " at index: " + matcher.start());
	    	
	    	if(!stack.empty()){
	    		 lastToken = stack.peek();
	    	}else{
	    		lastToken = "";
	    	}
	    	
	    	if(groupStr.equals(KEY_QUOTE)){
	    		if(stack.empty()){
		    		out("Push:`");
    				stack.push(KEY_QUOTE);
    				tokenMap.tokens.add(queryString.substring(lastIndex,matcher.start()));
			    	tokenMap.type.add(STRING);
			    	lastIndex=matcher.end();
		    	}else if(lastToken.equals(KEY_QUOTE)){
	    			out("Pop:`");
	    			stack.pop();
			    	tokenMap.tokens.add(queryString.substring(lastIndex,matcher.start()));
			    	tokenMap.type.add(PATH);
			    	lastIndex=matcher.end();
			    }
		    }else if(groupStr.equals(STRING_QUOTE)){
		    	if(stack.empty()){
		    		out("Push:'");
    				stack.push(STRING_QUOTE);
    				tokenMap.tokens.add(queryString.substring(lastIndex,matcher.start()));
			    	tokenMap.type.add(STRING);
			    	lastIndex=matcher.end();
		    	}else if(lastToken.equals(STRING_QUOTE)){
		    		out("Pop:'");
	    			stack.pop();
			    	tokenMap.tokens.add(queryString.substring(lastIndex,matcher.start()));
			    	tokenMap.type.add(STRING);
			    	lastIndex=matcher.end();
		    	}
		    }else if(groupStr.equals(STRING_DOUBLE_QUOTE)){
		    	if(stack.empty()){
		    		out("Push:''");
    				stack.push(STRING_DOUBLE_QUOTE);
    				tokenMap.tokens.add(queryString.substring(lastIndex,matcher.start()));
			    	tokenMap.type.add(STRING);
			    	lastIndex=matcher.end();
		    	}else if(lastToken.equals(STRING_QUOTE)||lastToken.equals(STRING_DOUBLE_QUOTE)){
		    		out("Pop:''");
		    		stack.pop();
		    		int offset=0;
		    		if(lastToken.equals(STRING_QUOTE)){
		    			offset=1;
		    		}
		    		tokenMap.tokens.add(queryString.substring(lastIndex,matcher.start()+offset));
			    	tokenMap.type.add(STRING);
			    	lastIndex=matcher.end();
		    	}
	    	}else if(groupStr.equals(KEY_DOUBLE_QUOTE)){
	    		if(stack.empty()){
    				stack.push(KEY_DOUBLE_QUOTE);
    				out("Push:``");
    				tokenMap.tokens.add(queryString.substring(lastIndex,matcher.start()));
			    	tokenMap.type.add(STRING);
			    	lastIndex=matcher.end();
	    		}else if(lastToken.equals(KEY_QUOTE)||lastToken.equals(KEY_DOUBLE_QUOTE)){
	    			out("Pop:``");
	    			stack.pop();
	    			int offset=0;
		    		if(lastToken.equals(KEY_QUOTE)){
		    			offset=1;
		    		}
		    		tokenMap.tokens.add(queryString.substring(lastIndex,matcher.start()+offset));
			    	tokenMap.type.add(PATH);
			    	lastIndex=matcher.end();
	    		}	
	    	}
	    }
	    
	    if(!stack.isEmpty()){
	    	err("Jsql syntax error. delimeter missing");
	    	return null;
	    }
	    
	    if(lastIndex<queryString.length()){
	    	clause = queryString.substring(lastIndex,queryString.length());
	    }else{
	    	clause=EMPTY;
	    }
	    tokenMap.tokens.add(clause);
	    tokenMap.type.add(0);
	 
	    listIt(tokenMap.tokens,"Parse out ` ' delimeters: \n----------------------------- ", "token: ");
	    listIt(tokenMap.type,"Parse out ` ' delimeters: \n----------------------------", "Token type: ");
		return tokenMap;
	}
	
	private List<String> splitOnBrackets(String queryString){
		Pattern pattern = Pattern.compile(BRACKETS);
	    Matcher matcher = pattern.matcher(queryString);
	     
	    List<String> tokenList = new ArrayList<String>();
	    
	    Stack<String> stack = new Stack<String>();
	    int lastIndex=0;
	    String clause = EMPTY;
	    String groupStr = "";
	    
	    // matching []
	    
	    out("Parsing the expression:\n");
	    while (matcher.find()) {
	    	
	    	groupStr = matcher.group(0);
	    	out("Captured: " + groupStr + " at index: " + matcher.start());
	    	
	    	if(groupStr.equals(SELECTOR_LEFT_BRACKET)){
	    			stack.push(SELECTOR_LEFT_BRACKET);
    				tokenList.add(queryString.substring(lastIndex,matcher.start()));
			    	lastIndex=matcher.end();
		    	
		    }else if(groupStr.equals(SELECTOR_RIGHT_BRACKET)){
		    		stack.pop();
    				tokenList.add(queryString.substring(lastIndex,matcher.start()));
			    	lastIndex=matcher.end();
	    	}
	    }
	    
	    if(!stack.isEmpty()){
	    	err("Jsql syntax error. delimeter missing");
	    	return null;
	    }
	    
	    if(lastIndex<queryString.length()){
	    	clause = queryString.substring(lastIndex,queryString.length());
	    }else{
	    	clause=EMPTY;
	    }
	    tokenList.add(clause);
	 
	    listIt(tokenList,"Parse out [] delimeters: \n----------------------------- ", "token: ");
	    return tokenList;
	}
	
	private List<String> splitOnAs(String selector){
		Pattern pattern = Pattern.compile(ASREGEX, Pattern.CASE_INSENSITIVE);
	    Matcher matcher = pattern.matcher(selector);
	     
	    List<String> tokenList = new ArrayList<String>();
	    
	    Stack<String> stack = new Stack<String>();
	    int lastIndex=0;
	    String clause = EMPTY;
	    String groupStr = "";
	    
	    // matching []
	    
	    out("Parsing the expression:\n");
	    while (matcher.find()) {
	    	
	    	groupStr = matcher.group(0);
	    	out("Captured: " + groupStr + " at index: " + matcher.start());
	    	
	    	if(groupStr.equals(SELECTOR_LEFT_BRACKET)){
	    			stack.push(SELECTOR_LEFT_BRACKET);
		    	
		    }else if(groupStr.equals(SELECTOR_RIGHT_BRACKET)){
		    		stack.pop();
	    	}else if(groupStr.equalsIgnoreCase(AS)){
	    		if(stack.empty()){
	    			tokenList.add(selector.substring(lastIndex,matcher.start()).trim());
	    			lastIndex=matcher.end();
	    		}
	    	}
	    }
	    
	    if(!stack.isEmpty()){
	    	err("Jsql syntax error. delimeter missing");
	    	return null;
	    }
	    
	    if(lastIndex<selector.length()){
	    	clause = selector.substring(lastIndex,selector.length()).trim();
	    }else{
	    	clause=EMPTY;
	    }
	    tokenList.add(clause);
	 
	    return tokenList;
	}
	
	private String quotes(int i , JSQLTokenMap<Integer,String> queryStringMap){
		if(queryStringMap.type.get(i)==PATH){
			return KEY_DOUBLE_QUOTE;
		}else{
			return STRING_DOUBLE_QUOTE;
		}
	}
	
	public HashMap<String,String> parseQueryString(String queryString){
		out("Parse queryString:" + queryString+"\n");
		
		// Splice out parts in back ticks and single quotes
		
		JSQLTokenMap<Integer,String> queryStringMap = splitOnQuotes(queryString);
		
		List<String> tokenList = queryStringMap.tokens;
		
		if(tokenList==null)return null;
		
		// Get the command structure based on the first keyword
		
		Object[][] keywords = getJsqlCommandStructure(tokenList.get(0).trim().toLowerCase());
		
		// querystring must start with an acceptable verb
		
		if(keywords==null){
			err("Jsql syntax error. Invalid verb: " + tokenList.get(0).trim());
			return null;
		}
		
		// reassemble
		
		String[] tokens=tokenList.toArray(new String[tokenList.size()]);
		
		StringBuilder str = new StringBuilder(EMPTY);
		for (int i = 0; i < tokens.length; i+=2) {
		    str.append(tokens[i]);
		    if(i+2<tokens.length)
		    str.append(KEY_PLACEHOLDER);
			//out(i+": "+tokenList.get(i));
		}
		
		// Splice out bracketed parts
		
		
		
		List<String>tokenList2 = splitOnBrackets(str.toString());
		
		// reassemble
		
		tokens=tokenList2.toArray(new String[tokenList2.size()]);

		str = new StringBuilder(EMPTY);
		for (int i = 0; i < tokens.length; i+=2) {
		    str.append(tokens[i]);
		    if(i+2<tokens.length)
		    str.append(BRACKETS_PLACEHOLDER);
			//out(i+": "+tokenList.get(i));
		}
		
		out("Simplified query:\n-----------------------------------");
		out(str.toString());
		out(EMPTY);
		
	    Pattern pattern = Pattern.compile(KEYWORD_REGEX, Pattern.CASE_INSENSITIVE);
	    Matcher matcher = pattern.matcher(str);
	    
	    HashMap<String,String> tokenMap = new HashMap<String,String>();
	    
	    String lastKey = EMPTY;
	    String clause = EMPTY;
	    int beginClause = 0;
	    int ignore_index = 0;
	    int precedence = 0;
	   
	    while (matcher.find()) {
	    	String key = matcher.group(0).trim().toLowerCase();
	    	if(matcher.start()>=ignore_index){
		    	if(key.equals(LEFT_PARENS)){
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
	    	clause=EMPTY;
	    }
	    if(!clause.equals(EMPTY)||contains(UNITARYKEYS,lastKey)){
			tokenMap.put(lastKey, clause);
	    }else{
	    	err("You have an error in your JSQL syntax near "+lastKey);
	    	return null;
	    }
	    
	    // Reinsert the bracketed text
		
 		int i = 1;
 		for (Entry<String, String> entry : tokenMap.entrySet()) {
	    	
    		clause = entry.getValue();
     		
     		pattern = Pattern.compile(BRACKETS_REGEX);
     	    matcher = pattern.matcher(clause);
     	    StringBuilder newClause = new StringBuilder(EMPTY);
     	    int index = 0;
     	    while (matcher.find()) {
     	    	index = matcher.end();
     	    	StringBuilder replacement = new StringBuilder();
     	    	replacement.append(SELECTOR_LEFT_BRACKET);
     	    	replacement.append(tokenList2.get(i).
     	    			replace(DOUBLE_BACKSLASH, QUADRUPLE_BACKSLASH).
 						replace(DOLLARSIGN, DOLLARSIGN_BACKSLASH));
     	    	replacement.append(SELECTOR_RIGHT_BRACKET);
     	    	newClause.append(matcher.replaceFirst(replacement.toString()).
     	    			substring(0,index+tokenList2.get(i).length()));
     	    	clause = clause.substring(index);
     	    	matcher = pattern.matcher(clause);
     	    	i+=2;
     	    }
 	    	
 	        //out(entry.getKey());
 	        newClause.append(clause);
 	       // out(newClause);
 	        tokenMap.put(entry.getKey(), newClause.toString());
 	    }
	    
		// Reinsert the quoted text
		
		i = 1;
	    for (Entry<String, String> entry : tokenMap.entrySet()) {
	    	
    		clause = entry.getValue();
    		
    		pattern = Pattern.compile(PLACEHOLDER_REGEX);
    	    matcher = pattern.matcher(clause);
    	    StringBuilder newClause = new StringBuilder(EMPTY);
    	    int index = 0;
    	    while (matcher.find()) {
    	    	index = matcher.end();
    	    	StringBuilder replacement = new StringBuilder();
    	    	replacement.append(quotes(i,queryStringMap));
    	    	replacement.append(tokenList.get(i).
    	    			replace(DOUBLE_BACKSLASH, QUADRUPLE_BACKSLASH).
						replace(DOLLARSIGN, DOLLARSIGN_BACKSLASH));
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
	    
		out("Finished parsing query.\n");

		return tokenMap;
	}
	
	public JSQLTokenMap<Integer,Object> parseExpression(String expression, JSQLExpression evaluator){
		
		Pattern pattern = Pattern.compile(EXPRESSIONQUOTES);
	    Matcher matcher = pattern.matcher(expression);
	    
	    JSQLTokenMap<Integer,String> tokenMap = new JSQLTokenMap<Integer,String>();
	    
	    Stack<String> stack = new Stack<String>();
	    int lastIndex=0;
	    String clause = EMPTY;
	    String lastToken = "";
	    String groupStr = "";
	    
	    // matching `` '' [ ]
	    
	    out("Parsing the expression:\n");
	    while (matcher.find()) {
	    	
	    	groupStr = matcher.group(0);
	    	out("Captured: " + groupStr + " at index: " + matcher.start());
	    	
	    	if(!stack.empty()){
	    		 lastToken = stack.peek();
	    	}else{
	    		lastToken = "";
	    	}
	    	
	    	if(groupStr.equals(SELECTOR_RIGHT_BRACKET)){
	    		if(lastToken.equals(SELECTOR_LEFT_BRACKET)){
	    			out("Pop:[");
	    			stack.pop();
			    	tokenMap.tokens.add(expression.substring(lastIndex,matcher.start()));
			    	tokenMap.type.add(PATH);
			    	lastIndex=matcher.end();
			    }
		    }else if(groupStr.equals(SELECTOR_LEFT_BRACKET)){
		    	if(stack.empty()){
		    		out("Push:[");
    				stack.push(SELECTOR_LEFT_BRACKET);
    				tokenMap.tokens.add(expression.substring(lastIndex,matcher.start()));
			    	tokenMap.type.add(STRING);
    				lastIndex=matcher.end();
		    	}
		    }else if(groupStr.equals(STRING_DOUBLE_QUOTE)){
		    	if(stack.empty()){
		    		out("Push:''");
    				stack.push(STRING_DOUBLE_QUOTE);
    				tokenMap.tokens.add(expression.substring(lastIndex,matcher.start()));
			    	tokenMap.type.add(STRING);
    				lastIndex=matcher.end();
		    	}else if(lastToken.equals(STRING_DOUBLE_QUOTE)){
		    		out("Pop:''");
		    		stack.pop();
		    		tokenMap.tokens.add(expression.substring(lastIndex,matcher.start()));
			    	tokenMap.type.add(STRING);
			    	lastIndex=matcher.end();
		    	}
	    	}else if(groupStr.equals(KEY_DOUBLE_QUOTE)){
	    		if(stack.empty()||lastToken.equals(SELECTOR_LEFT_BRACKET)){
	    				stack.push(KEY_DOUBLE_QUOTE);
	    				out("Push:``");
	    		}else if(lastToken.equals(KEY_DOUBLE_QUOTE)){
	    			out("Pop:``");
	    			stack.pop();
	    		}	
	    	}
	    }
	    
	    if(!stack.isEmpty()){
	    	err("Jsql syntax error. delimeter missing");
	    	return null;
	    }
	    
	    
	    if(lastIndex<expression.length()){
	    	clause = expression.substring(lastIndex,expression.length());
	    }else{
	    	clause=EMPTY;
	    }
	    tokenMap.tokens.add(clause);
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
		
		StringBuilder simplified_expression = new StringBuilder(EMPTY);
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
	
	public ArrayList<JSQLSelector> parseSelector(String selection,ArrayList<String> identifiers){
		out("start querySelector:"+selection);
		
		ArrayList<JSQLSelector> queryList = new ArrayList<JSQLSelector>();
		
		String[] tokensStr=selection.split(PATHQUOTES,-1);
		
		StringBuilder str = new StringBuilder(EMPTY);
		for (int i = 0; i < tokensStr.length; i+=2) {
		    str.append(tokensStr[i]);
		    if(i+2<tokensStr.length)
		    str.append(KEY_PLACEHOLDER2);
			//out(i+": "+tokenList.get(i));
		}
		
		for(String tokens:tokensStr){
			out("token:"+tokens);
		}
		
		selection=str.toString();
		
		out("Simplified selector:" + selection);
		
		
		for(int i = 0;i<FORBIDDEN_SEQUENCES.length;i++){
			if(selection.contains(FORBIDDEN_SEQUENCES[i])){
				err("Jsql syntax error. Forbidden sequence: " + FORBIDDEN_SEQUENCES[i] + ".");
				return queryList;
			}
		}
		
		int replacementCount=1;
		String[] path_selectors = selection.split(SELECTOR_DELIMITER);
		for(String path_selector:path_selectors){
			List<String> path_components = splitOnAs(path_selector);
			path_selector = path_components.get(0);
			String identifier = "";
			if(path_components.size()>1){
				identifier=path_components.get(1);
			}
			out("ps: " + path_selector+ " identifier: " + identifier);
			if(path_selector.startsWith(SELECTOR_LEFT_BRACKET)){
				path_selector = path_selector.substring(1, path_selector.length());
			}
			if(path_selector.endsWith(SELECTOR_RIGHT_BRACKET)){
				path_selector = path_selector.substring(0, path_selector.length()-1);
			}
			JSQLSelector selector = new JSQLSelector();
			selector.target=EMPTY;
			selector.id=path_selector;
			String[] paths = path_selector.split(NOT_OPERATOR);
			int j = 0;
			for(String path:paths){
				out("getTokens: path:" +path);
				path = path.
						replaceAll(NOT_BACKSLASH+CHILD_OPERATOR,PATH_DELIMITER+CHILD_OPERATOR+PATH_DELIMITER);
				if(path.startsWith(PATH_DELIMITER+CHILD_OPERATOR))path = path.substring(1);
				if(path.endsWith(CHILD_OPERATOR+PATH_DELIMITER))path = path.substring(0,path.length()-1);
				
				String[] keys = path.split(NOT_BACKSLASH+PATH_DELIMITER_REGEX,-1);
				if(j==0&&!keys[0].equals(FROM)&&identifiers.contains(keys[0])){
					selector.target=keys[0];
					if(path.indexOf(".")==-1||path.indexOf(".")+1==path.length()){
						err("Jsql syntax error. Selector: " + keys[0]);
						return queryList;
					}
					keys=path.substring(path.indexOf(".")+1).split(NOT_BACKSLASH+PATH_DELIMITER_REGEX,-1);
				}
				
				int k = 0;
				for(String key:keys){
					out("getTokens: key:" +key);
					while(key.contains(KEY_PLACEHOLDER2)){
						key = key.replaceFirst(KEY_PLACEHOLDER2,KEY_QUOTE+tokensStr[replacementCount].
								replace(DOUBLE_BACKSLASH, QUADRUPLE_BACKSLASH).
								replace(DOLLARSIGN, DOLLARSIGN_BACKSLASH)+KEY_QUOTE);
						replacementCount+=2;
					}
					
					keys[k]=key;
					k++;
					
					out(key);
				}
				if(j==0){
					selector.identifier=identifier;
					selector.path=keys;
				}else{
					if(selector.exceptionPaths==null)selector.exceptionPaths = new ArrayList<String[]>();
					selector.exceptionPaths.add(keys);
				}
				j++;
			}
			queryList.add(selector);
		}
		return queryList;
	}
	
	public List<String> parseSelectClause(String expression){
		
		Pattern pattern = Pattern.compile(SELECT_CLAUSE_REGEX);
	    Matcher matcher = pattern.matcher(expression);
	    

	    List<String> queryList = new ArrayList<String>();
	    Stack<String> stack = new Stack<String>();
	    StringBuilder str = new StringBuilder(expression);
	    int lastIndex=0;
	    String clause = EMPTY;
	    String lastToken = "";
	    String groupStr = "";
	    
	    // matching `` '' [ ] ( ) ,
	    
	    out("Parsing the expression:\n");
	    while (matcher.find()) {
	    	
	    	groupStr = matcher.group(0);
	    	out("Captured: " + groupStr + " at index: " + matcher.start());
	    	
	    	if(!stack.empty()){
	    		 lastToken = stack.peek();
	    	}else{
	    		lastToken = "";
	    	}
	    	
	    	if(groupStr.equals(SELECTOR_RIGHT_BRACKET)){
	    		if(lastToken.equals(SELECTOR_LEFT_BRACKET)){
	    			out("Pop:[");
	    			stack.pop();
			    }
		    }else if(groupStr.equals(SELECTOR_LEFT_BRACKET)){
		    	if(stack.empty()||lastToken.equals(LEFT_PARENS)){
		    		out("Push:[");
    				stack.push(SELECTOR_LEFT_BRACKET);
		    	}
		    }else if(groupStr.equals(STRING_DOUBLE_QUOTE)){
		    	if(stack.empty()||lastToken.equals(LEFT_PARENS)){
		    		out("Push:''");
    				stack.push(STRING_DOUBLE_QUOTE);
		    	}else if(lastToken.equals(STRING_DOUBLE_QUOTE)){
		    		out("Pop:''");
		    		stack.pop();
		    	}
	    	}else if(groupStr.equals(KEY_DOUBLE_QUOTE)){
	    		if(stack.empty()||lastToken.equals(SELECTOR_LEFT_BRACKET)||lastToken.equals(LEFT_PARENS)){
	    				stack.push(KEY_DOUBLE_QUOTE);
	    				out("Push:``");
	    		}else if(lastToken.equals(KEY_DOUBLE_QUOTE)){
	    			out("Pop:``");
	    			stack.pop();
	    		}	
	    	}else if(groupStr.equals(LEFT_PARENS)){
	    		if(stack.empty()||lastToken.equals(LEFT_PARENS)){
	    			out("Push:(");
    				stack.push(LEFT_PARENS);
		    	}
	    	}else if(groupStr.equals(RIGHT_PARENS)){
	    		if(lastToken.equals(LEFT_PARENS)){
	    			out("Pop:(");
    				stack.pop();
		    	}
	    	}else if(groupStr.equals(COMMA)){
	    		if(stack.empty()){
	    			clause = str.substring(lastIndex,matcher.start());
	    			if(!clause.equals(EMPTY)){
		    			queryList.add(clause);
		    		}
	    			out("add comma");
			    	lastIndex =  matcher.end();
	    		}
	    	}
	    }
	    
	    if(!stack.isEmpty()){
	    	err("Jsql syntax error. delimeter missing");
	    	return null;
	    }
	   
	    if(lastIndex<str.length()){
	    	clause = str.substring(lastIndex,str.length());
	    }else{
	    	clause=EMPTY;
	    }
	    if(!clause.equals(EMPTY)){
			queryList.add(clause);
	    }
		return queryList;
	}
	
	public String unescape(String key){
		if(key.startsWith(KEY_QUOTE)){
			key = key.substring(1, key.length()-1);
		}
		for(int count=0;count<ESCAPE_CHARS.length;count++){
			key=key.replace(DOUBLE_BACKSLASH+ESCAPE_CHARS[count], ESCAPE_CHARS[count]); // path
		}
		return key;
	}
	
	public String formatForOutput(String expr){
		return expr.replaceAll(STRINGQUOTES, "'");
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
		//System.out.println(msg);
	}
	
	private void out(Object msg, boolean inline){
		//System.out.print(msg);
	}
	private void listIt(List list,String start,String loopStr){
		out(start);
		for (Object elem : list) {
			out(loopStr+ " "+ elem.toString());
		}
		out("---------------------------------");
		out(EMPTY);
	}
}
