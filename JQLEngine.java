import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;


public class JQLEngine {
	
	private static final String[] KEYWORDS = {"select",
		"from",
		"where",
		"order by",
		"limit"};
	
	private static final String SELECT = "select";
	private static final Object FROM = "from";
	private static final Object WHERE = "where";
	private static final Object ORDER_BY = "order by";
	private static final String LIMIT = "limit";
	
	private static final String ROOT_KEY = "root";
	private static final String EMPTY = "";
	private static final String QUERY_SEPARATOR = ",";
	private static final String NOT_OPERATOR = "!";
	private static final String ALL_OPERATOR = "*";
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
	
	private static final String[] SPECIAL_CHARS = {".", "*",":","!",",","'"};  // separator
	
	private static final String[] WHERECLAUSE_OPERATORS = {"=" ,"!=","<","<=",">",">="};
	
	public JQLContext cntx;
	
	public JQLEngine(){
		this.cntx = new JQLContext();
	}
	
	//TODO delete
	private void out(Object msg){
		System.out.println(msg);
	}
	private void out2(Object msg){
		System.out.print(msg);
	}
	
	public static final JQLEngine getJQL(){
		return new JQLEngine();
	}
	
	public class JQLContext{
		public boolean include=true;
		public boolean check=true;
	}
	
	public JsonQueryArray execute(JsonQueryNode node,String queryString){
		HashMap<String,String> clauses = parseQueryString(queryString);
		if(clauses==null)return new JsonQueryArray();
		return executeJQL(node, clauses);
	}
	
	public JsonQueryArray executeJQL(JsonQueryNode node, HashMap<String,String> clauses){
			
			out("FROM clause");
			
			JsonQueryArray fromResultSet = new JsonQueryArray();
			
			String fromClause=ALL_OPERATOR;
			if(clauses.containsKey(FROM))fromClause=clauses.get(FROM);
			if(fromClause.equals(ALL_OPERATOR)){
				node.key=ROOT_KEY;
				fromResultSet.add(node);
			}else{
				ArrayList<JsonQueryTokens> queries = getTokens(fromClause);
				executeJQLClause(node,fromResultSet,queries);
			}
			
			out("WHERE clause");
			
			if(clauses.containsKey(WHERE)){
				String whereClause=clauses.get(WHERE);
				executeWhereClause(fromResultSet,whereClause);
			}
			
			out("SELECT clause");
			
			JsonQueryArray selectResultSet = new JsonQueryArray();
			
			if(clauses.containsKey(SELECT)){
				String selectClause=clauses.get(SELECT);
				if(selectClause.equals(ALL_OPERATOR)){
					selectResultSet=fromResultSet;
				}else{
					ArrayList<JsonQueryTokens> queries = getTokens(selectClause);
					for(JsonQueryNode subNode: fromResultSet){
							executeJQLClause(subNode,selectResultSet,queries);
					}
				}
			}
			
			return selectResultSet;
		}
		
		private void executeJQLClause(JsonQueryNode node, 
				JsonQueryArray resultSet,
				ArrayList<JsonQueryTokens> queries
				){
			for(JsonQueryTokens tokens:queries){
				int branches_up=0;
				int i = tokens.path.length-1;
				while(i>0&&(tokens.path[i].equals(EMPTY))){
					branches_up++;
					i--;
				}
				buildResultSet(node,resultSet,tokens,0,branches_up,(tokens.exceptionPaths.size()==0?false:true));
			}
		}
		
		private void executeWhereClause(JsonQueryArray array,String whereClause){
			
			int operator_array_index=0;
			int operator_index=-1;
			int operator_length=1;
			
			for(int i = 0;i<WHERECLAUSE_OPERATORS.length;i++){
				int length = WHERECLAUSE_OPERATORS[i].length();
				int index = whereClause.lastIndexOf(WHERECLAUSE_OPERATORS[i]);
				if(index>0){
					int oldLastIndex = operator_index+operator_length;
					int newLastIndex = index + length;
					if(newLastIndex>oldLastIndex||(newLastIndex==oldLastIndex&&length>operator_length)){
						operator_index = index;
						operator_length = length;
						operator_array_index = i;
					}
				}
			}
			
			out("in while clause: operator index:"+operator_index + " " + operator_length);
			
			if(operator_index!=-1&&operator_index+operator_length<whereClause.length()){
				
				String[] operands = new String[2];
				operands[0] = whereClause.substring(0,operator_index);
				operands[1] = whereClause.substring(operator_index+operator_length,whereClause.length());
				
				out(operands[0]);
				out(operands[1]);
				Iterator<JsonQueryNode> iterator = array.iterator();
				while (iterator.hasNext()) {
				   JsonQueryNode node = iterator.next();
					boolean pass = false;
					for(JsonQueryNode subNode:node.jql("Select '*' From '"+operands[0]+"'")){
					    Object val = subNode.val();
						switch (operator_array_index){
							case 0: // equals
								out("=: "+ val + " " +operands[1]);
								if(val instanceof JsonQueryNumber){
									if(((JsonQueryNumber)val).doubleValue()==Double.valueOf(operands[1]))pass=true;
								}else if(val instanceof Boolean){
									if((Boolean)val.equals(Boolean.valueOf(operands[1])))pass=true;
								}else if(val instanceof String){
									if(val.equals(operands[1]))pass=true;
								}
								break;
							case 1: // not equals
								out("!=: "+ val + " " +operands[1]);
								if(val instanceof JsonQueryNumber){
									if(((JsonQueryNumber)val).doubleValue()!=Double.valueOf(operands[1]))pass=true;
								}else if(val instanceof Boolean){
									if(!(Boolean)val.equals(Boolean.valueOf(operands[1])))pass=true;
								}else if(val instanceof String){
									if(!val.equals(operands[1]))pass=true;
								}
								break;
							case 2: // less than
								out("<: "+ val + " " +operands[1]);
								if(val instanceof JsonQueryNumber){
									if(((JsonQueryNumber)val).doubleValue()<Double.valueOf(operands[1]))pass=true;
								}
								break;
							case 3: // less than equal
								out("<=: "+ val + " " +operands[1]);
								if(val instanceof JsonQueryNumber){
									if(((JsonQueryNumber)val).doubleValue()<=Double.valueOf(operands[1]))pass=true;
								}
								break;
							case 4: // greater than
								out(">: "+ val + " " +operands[1]);
								if(val instanceof JsonQueryNumber){
									if(((JsonQueryNumber)val).doubleValue()>Double.valueOf(operands[1]))pass=true;
								}
								break;
							case 5: // greater than equal
								out(">=: "+ val + " " +operands[1]);
								if(val instanceof JsonQueryNumber){
									if(((JsonQueryNumber)val).doubleValue()>=Double.valueOf(operands[1]))pass=true;
								}
								break;
							
						}
						out("pass: "+ pass);
						if(!pass){
							iterator.remove();
							break;
						}
					}
				}
			}else{
				array.clear();
			}
		}
		
		private void buildResultSet(
				JsonQueryNode node,
				JsonQueryArray resultSet,
				JsonQueryTokens tokens,
				int currentIndex,
				int branches_up,
				boolean check){
			out("Starting resultSet process");
			ArrayList<String> searchPath= new ArrayList<String>();
			boolean next = false;
			boolean child = false;
			int index;
			int endOfPath = tokens.path.length-branches_up;
			for(index = currentIndex;index<endOfPath;index++){
				out("iterating path-index: "+index+ " value:" + tokens.path[index]);
				if(tokens.path[index].equals(ALL_OPERATOR)||tokens.path[index].equals(EMPTY)){
					next = true;
					break;
				}else if(tokens.path[index].equals(CHILD_OPERATOR)){
					child=true;
					break;
				}else{
					searchPath.add(tokens.path[index]);
				}
			}
			if(!searchPath.isEmpty()){
				out("grabing path");
				JsonQueryNode nextNode = node.path(node,searchPath.toArray(),true);
				if(checkNode(nextNode,tokens,branches_up,check)){
					out(nextNode.element);
					if(nextNode.element!=null){
						if(index == endOfPath){
							out("adding from path: "+ nextNode.key);
							addToResultSet(nextNode,resultSet,branches_up,cntx.include);
						}else{
							out("continue");
							buildResultSet(nextNode,resultSet,tokens,index,branches_up,cntx.check);
						}
					}
				}
				return;
			}
			if(next){
				out("next");
				index++;
				int keyIndex = 0;
				for (JsonQueryNode nextNode:node.each()){
					if((Object)node instanceof JsonQueryArray){
						nextNode.key = String.valueOf(keyIndex++);
					}
					nextNode.setAntenode(node);
					if(checkNode(nextNode,tokens,branches_up,check)){
						if(index == endOfPath){
							out("adding from next: "+ branches_up);
							addToResultSet(nextNode,resultSet,branches_up,cntx.include);
						}else{
							out("continue");
							buildResultSet(nextNode,resultSet,tokens,index,branches_up,cntx.check);
						}
					}
				}
				
				return;
			}
			if(child){
				index++;
				if(index<endOfPath&&!tokens.path[index].equals(EMPTY)&&!tokens.path[index].equals(EMPTY)){
					out("child search:"+index);
					keySearch(node,resultSet,tokens,index,branches_up,check);
				}
			}
		}
		
		private void addToResultSet(JsonQueryNode node,JsonQueryArray resultSet,int branches_up,boolean include){
			if(include){
				for(int i = 0;i<branches_up;i++){
					node=node.getAntenode();
				}
				resultSet.add(node);
			}
		}
		
		private void keySearch(
				JsonQueryNode node,
				JsonQueryArray resultSet,
				JsonQueryTokens tokens,
				int currentIndex,
				int branches_up,
				boolean check){
			
			int endOfPath = tokens.path.length-branches_up;
			String key = tokens.path[currentIndex];
			if(node.element instanceof JsonQueryObject||node.element instanceof JsonQueryArray){
				int keyIndex = 0;
				for (JsonQueryNode nextNode:node.each()){
					if((Object)node instanceof JsonQueryArray){
						nextNode.key = String.valueOf(keyIndex++);
					}
					nextNode.setAntenode(node);
					if(checkNode(nextNode,tokens,branches_up,check)){
						if(nextNode.key.equals(key)){
							out("match found");
							if(currentIndex+1 == endOfPath){
								out("adding from keysearch");
								addToResultSet(nextNode,resultSet,branches_up,cntx.include);
							}else{
								out("continue looking");
								buildResultSet(nextNode,resultSet,tokens,currentIndex+1,branches_up,cntx.check);
							}
						}else{
							keySearch(nextNode,resultSet,tokens,currentIndex,branches_up,cntx.check);
						}
					}
				}
			}
		}
		
		private HashMap<String,String> parseQueryString(String queryString){
			
			String[] tokens = queryString.trim().split(NOT_BACKSLASH+CLAUSE_DELIMETER+EMPTY);
			if(tokens.length<2){
				return null;
			}
			HashMap<String,String> tokenMap = new HashMap<String,String>();
			int precedence=0;
			boolean selectFound=false;
			for(int i = 0;i<tokens.length;i+=2){
				if(tokens[i].trim().toLowerCase().startsWith(LIMIT)){
					tokens = tokens[i].split(SPACE);
					if(tokens.length>1){
						tokens[1].trim();
					}
					if(tokens.length!=2){
						return null;
					}
					out(KEYWORDS[precedence]+" "+tokens[1]);
					tokenMap.put(LIMIT,tokens[1]);
				}else{
					String keyword = tokens[i].trim();
					boolean found = false;
					int count = 0;
					for (String kw : KEYWORDS) {
					    if (keyword.equalsIgnoreCase(kw)) {
					        if(precedence > count){
					        	return null;
					        }
					        precedence=count;
					        found = true;
					        break;
					    }
					    count++;
					}
					if (!found||i+1>=tokens.length) {
						return null;
					}
					if(keyword.equalsIgnoreCase(SELECT))selectFound=true;
					out(KEYWORDS[precedence]+" "+tokens[i+1]);
					tokenMap.put(KEYWORDS[precedence],tokens[i+1]);
				}
			}
			if(!selectFound){
				return null;
			}
			return tokenMap;
		}
		
		private ArrayList<JsonQueryTokens> getTokens(String queryString){
			out("in get tokens");
			ArrayList<JsonQueryTokens> queryList = new ArrayList<JsonQueryTokens>();
			String[] queries = queryString.split(QUERY_SEPARATOR);
			for(String query:queries){
				JsonQueryTokens tokens = new JsonQueryTokens();
				String[] paths = query.split(NOT_OPERATOR);
				int j = 0;
				for(String path:paths){
					out(path);
					path = path.
							replaceAll(NOT_BACKSLASH+CHILD_OPERATOR,PATH_DELIMETER+CHILD_OPERATOR+PATH_DELIMETER).
							replaceAll(NOT_BACKSLASH+LEFT_BRACKET_ESCAPE,PATH_DELIMETER).
							replaceAll(NOT_BACKSLASH+RIGHT_BRACKET_ESCAPE,EMPTY);
					if(path.startsWith(PATH_DELIMETER+CHILD_OPERATOR))path = path.substring(1);
					String[] keys = path.split(NOT_BACKSLASH+PATH_DELIMETER_REGEX,-1);
					int k = 0;
					for(String key:keys){
						out(key);
						for(int count=0;count<SPECIAL_CHARS.length;count++){
							key=key.replace(DOUBLE_BACKSLASH+SPECIAL_CHARS[count], SPECIAL_CHARS[count]); // path
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
				}
				queryList.add(tokens);
			}
			return queryList;
		}
		public boolean checkNode(JsonQueryNode _node,JsonQueryTokens tokens,int branches_up, boolean check){
			
	
			
			boolean resume = true;
			cntx.include = true;
			cntx.check=check;
			
			if(check){
				
				out("checking node");
				
				JsonQueryNode node = _node;
				ArrayList<String> array = new ArrayList<String>();
				
				cntx.include = true;
				
				for(String[] exceptions:tokens.exceptionPaths){
					array.clear();
					while(node.getAntenode()!=null){
						array.add(node.key);
						node=node.getAntenode();
					}
					Collections.reverse(array);
					array.add(null);
					for(int i = 0;i<branches_up;i++){
						array.remove(0);
					}
					// debug
					for(int i = 0;i<array.size();i++){
						out2(array.get(i)+" ");
					}
					
					int arrayIndex = 0;
					int index;
					for(index =0;index<exceptions.length;index++){
						out("iterating exception:"+1+" execption-index: "+index+ " value:" + exceptions[index]+" against path vlaue:"+array.get(arrayIndex));
						
						if(exceptions[index].equals(ALL_OPERATOR)||exceptions[index].equals(EMPTY)){
							if(array.get(arrayIndex)==null){
								break;
							}
							
						}else if(exceptions[index].equals(CHILD_OPERATOR)){
							
							// If its the last token, then cntx.match is true
							// If it is followed by another token
							if(index+1!=exceptions.length){
								if(array.get(arrayIndex)==null){
									break;
								}else{
									int newIndex = array.indexOf(exceptions[index+1]);
									if(newIndex==-1){
										out("did not found child");
										break;
									}else{
										for(int j = arrayIndex;j<newIndex;j++){
											array.set(j,null);
										}
										arrayIndex=newIndex;
										out("found child at :" +arrayIndex);
									}
									index++;
								}
							}
							break;
						}else{
							if(!exceptions[index].equals(array.get(arrayIndex))){
								out("stop checking");
								cntx.check = false;
								break;
							}
						}
						array.set(arrayIndex, null);
						arrayIndex++;
					}
					if(index==exceptions.length){
						cntx.include=false;
					}
					if(!cntx.include){
						if(exceptions[index-1]==CHILD_OPERATOR){
							out("stop iterating");
							resume = false;
						}else{
							if(array.get(arrayIndex)!=null){
								cntx.include=true;
							}
						}
					}
					if(!cntx.include){
						out("exclude this node");
						break;
					}else{
						out("include this node");
					}
				}
				out("end check");
			}
			return resume;
		}
}
