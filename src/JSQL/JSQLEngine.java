package src.JSQL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;


//import src.JsonQueryUtil;

//import src.JsonQueryObject;


public class JSQLEngine {
	
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
	
	private JQLContext cntx;
	
	private JSQLEngine(){
		this.cntx = new JQLContext();
	}
	
	public static final JSQLEngine getJQL(){
		return new JSQLEngine();
	}
	
	private class JQLContext{
		public boolean include=true;
		public boolean check=true;
	}
	
	public JSQLResultSet<JSQLNode> execute(JSQLNode node,String queryString){
		HashMap<String,String> clauses = parseQueryString(queryString);
		if(clauses==null)return new JSQLResultSet<JSQLNode>();
		return executeJQL(node, clauses);
	}
	
	private JSQLResultSet<JSQLNode> executeJQL(JSQLNode node, HashMap<String,String> clauses){
			
			out("FROM clause");
			
			JSQLResultSet<JSQLNode> fromResultSet = new JSQLResultSet<JSQLNode>();
			node.setKey(ROOT_KEY);
			
			String fromClause=ALL_OPERATOR;
			if(clauses.containsKey(FROM))fromClause=clauses.get(FROM);
			if(fromClause.equals(ALL_OPERATOR)){
				fromResultSet.add(node);
			}else{
				ArrayList<JSQLTokens> queries = getTokens(fromClause);
				executeJQLClause(node,fromResultSet,queries);
			}
			
			out("WHERE clause");
			
			if(clauses.containsKey(WHERE)){
				String whereClause=clauses.get(WHERE);
				executeWhereClause(fromResultSet,whereClause);
			}
			
			out("SELECT clause");
			
			JSQLResultSet<JSQLNode> selectResultSet = new JSQLResultSet<JSQLNode>();
			
			if(clauses.containsKey(SELECT)){
				String selectClause=clauses.get(SELECT);
				if(selectClause.equals(ALL_OPERATOR)){
					selectResultSet=fromResultSet;
				}else{
					ArrayList<JSQLTokens> queries = getTokens(selectClause);
					for(JSQLNode subNode: (JSQLResultSet<JSQLNode>)fromResultSet){
							executeJQLClause(subNode,selectResultSet,queries);
					}
				}
			}
			out("finished");
			return selectResultSet;
		}
		
		private void executeJQLClause(JSQLNode node, 
				JSQLResultSet<JSQLNode> resultSet,
				ArrayList<JSQLTokens> queries
				){
			for(JSQLTokens tokens:queries){
				int branches_up=0;
				int i = tokens.path.length-1;
				while(i>0&&(tokens.path[i].equals(EMPTY))){
					branches_up++;
					i--;
				}
				buildResultSet(node,resultSet,tokens,0,branches_up,(tokens.exceptionPaths==null||tokens.exceptionPaths.size()==0?false:true));
			}
		}
		
		private void executeWhereClause(JSQLResultSet<JSQLNode> array,String whereClause){
			
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
			
			out("In where clause: operator index:"+operator_index + " " + operator_length);
			
			if(operator_index!=-1&&operator_index+operator_length<whereClause.length()){
				
				String[] operands = new String[2];
				operands[0] = whereClause.substring(0,operator_index);
				operands[1] = whereClause.substring(operator_index+operator_length,whereClause.length());
				
				out("where clause op1:"+operands[0]);
				out("where clause op2:"+operands[1]);
				Iterator<JSQLNode> iterator = ((JSQLResultSet<JSQLNode>)array).iterator();
				while (iterator.hasNext()) {
				   JSQLNode node = iterator.next();
					boolean pass = false;
					for(JSQLNode subNode:(JSQLResultSet<JSQLNode>)execute(node,"Select '*' From '"+operands[0]+"'")){
					    Object val = ((JSQLNode)subNode).getElement();
						switch (operator_array_index){
							case 0: // equals
								out("=: "+ val + " " +operands[1]);
								if(val instanceof JSQLNumber){
									if(((JSQLNumber)val).doubleValue()==Double.valueOf(operands[1]))pass=true;
								}else if(val instanceof Boolean){
									if((Boolean)val.equals(Boolean.valueOf(operands[1])))pass=true;
								}else if(val instanceof String){
									if(val.equals(operands[1]))pass=true;
								}
								break;
							case 1: // not equals
								out("!=: "+ val + " " +operands[1]);
								if(val instanceof JSQLNumber){
									if(((JSQLNumber)val).doubleValue()!=Double.valueOf(operands[1]))pass=true;
								}else if(val instanceof Boolean){
									if(!(Boolean)val.equals(Boolean.valueOf(operands[1])))pass=true;
								}else if(val instanceof String){
									if(!val.equals(operands[1]))pass=true;
								}
								break;
							case 2: // less than
								out("<: "+ val + " " +operands[1]);
								if(val instanceof JSQLNumber){
									if(((JSQLNumber)val).doubleValue()<Double.valueOf(operands[1]))pass=true;
								}
								break;
							case 3: // less than equal
								out("<=: "+ val + " " +operands[1]);
								if(val instanceof JSQLNumber){
									if(((JSQLNumber)val).doubleValue()<=Double.valueOf(operands[1]))pass=true;
								}
								break;
							case 4: // greater than
								out(">: "+ val + " " +operands[1]);
								if(val instanceof JSQLNumber){
									if(((JSQLNumber)val).doubleValue()>Double.valueOf(operands[1]))pass=true;
								}
								break;
							case 5: // greater than equal
								out(">=: "+ val + " " +operands[1]);
								if(val instanceof JSQLNumber){
									if(((JSQLNumber)val).doubleValue()>=Double.valueOf(operands[1]))pass=true;
								}
								break;
							
						}
						out("where clause pass: "+ pass);
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
				JSQLNode node,
				JSQLResultSet<JSQLNode> resultSet,
				JSQLTokens tokens,
				int currentIndex,
				int branches_up,
				boolean check){
			out("Fetching resultSet:");
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
				out("build result: grabing path");
				JSQLNode nextNode = path(node,searchPath.toArray(),true);
				if(checkNode(nextNode,tokens,branches_up,check)){
					if(nextNode.getElement()!=null){
						if(index == endOfPath){
							out("build result: adding from path: "+ nextNode.getKey());
							addToResultSet(nextNode,resultSet,branches_up,cntx.include);
						}else{
							out("build result: continue");
							buildResultSet(nextNode,resultSet,tokens,index,branches_up,cntx.check);
						}
					}
				}
				return;
			}
			if(next){
				out("build result: next");
				index++;
				int keyIndex = 0;
				for (Object objNode:node.getChildNodes()){
					JSQLNode nextNode = (JSQLNode)objNode;
					if(node instanceof JSQLArray){
						nextNode.setKey(String.valueOf(keyIndex++));
					}
					nextNode.setAntenode(node);
					if(checkNode(nextNode,tokens,branches_up,check)){
						if(index == endOfPath){
							out("build result: adding from next");
							addToResultSet(nextNode,resultSet,branches_up,cntx.include);
						}else{
							out("build result: continue");
							buildResultSet(nextNode,resultSet,tokens,index,branches_up,cntx.check);
						}
					}
				}
				
				return;
			}
			if(child){
				index++;
				if(index<endOfPath){
					if(!tokens.path[index].equals(ALL_OPERATOR)){
						out("build result: child search:"+index);
						keySearch(node,resultSet,tokens,index,branches_up,check);
					}else{
						returnAll(node,resultSet,tokens,index,branches_up,check);
					}
				}else{
					if(checkNode(node,tokens,branches_up,check)){
						out("build result: adding from retern all + node");
						addToResultSet(node,resultSet,branches_up,cntx.include);
						returnAll(node,resultSet,tokens,index,branches_up,cntx.check);
					}
				}
			}
		}
		
		private void addToResultSet(JSQLNode node,JSQLResultSet<JSQLNode> resultSet,int branches_up,boolean include){
			if(include){
				for(int i = 0;i<branches_up;i++){
					node=node.getAntenode();
				}
				resultSet.add(node);
			}
		}
		
		private JSQLNode path(JSQLNode node, Object[] keys, boolean addTreeInfo){
			try{
				int i=0;
				for(i=0;i<keys.length;i++){
					JSQLNode nextNode=null;
					if(JSQLUtil.isInteger((String)keys[i])){
						nextNode = (JSQLNode) node.getNextNode(Integer.parseInt((String)keys[i]));
					}else{
						nextNode = (JSQLNode) node.getNextNode((String)keys[i]);
					}
					if(nextNode!=null&&addTreeInfo){
							nextNode.setKey((String)keys[i]);
						nextNode.setAntenode(node);
					}
					node=nextNode;
					if(node==null)break;
				}
				if(node!=null)return node;
			}catch(Throwable e){
				handleException(e);
			}
			return node.createNewNode(null,null);
		}
		
		private void keySearch(
				JSQLNode node,
				JSQLResultSet<JSQLNode> resultSet,
				JSQLTokens tokens,
				int currentIndex,
				int branches_up,
				boolean check){
			
			int endOfPath = tokens.path.length-branches_up;
			String key = tokens.path[currentIndex];

			if(node.getElement() instanceof JSQLObject||node.getElement() instanceof JSQLArray){
				int keyIndex = 0;
				out("instance of array: " +(node.getElement() instanceof JSQLArray));
				for (Object objNode:node.getChildNodes()){
					out("iterating");
					JSQLNode nextNode = (JSQLNode)objNode;
					if((Object)node instanceof JSQLArray){
						nextNode.setKey(String.valueOf(keyIndex++));
					}
					nextNode.setAntenode(node);
					if(checkNode(nextNode,tokens,branches_up,check)){
						if(nextNode.getKey().equals(key)){
							out("keysearch: match found");
							if(currentIndex+1 == endOfPath){
								out("keysearch: :adding node");
								addToResultSet(nextNode,resultSet,branches_up,cntx.include);
							}else{
								out("keysearch: continue looking");
								buildResultSet(nextNode,resultSet,tokens,currentIndex+1,branches_up,cntx.check);
							}
						}else{
							keySearch(nextNode,resultSet,tokens,currentIndex,branches_up,cntx.check);
						}
					}
				}
			}
		}
		
		private void returnAll(
				JSQLNode node,
				JSQLResultSet<JSQLNode> resultSet,
				JSQLTokens tokens,
				int currentIndex,
				int branches_up,
				boolean check){
			
			if(node.getElement() instanceof JSQLObject||node.getElement() instanceof JSQLArray){
				int keyIndex = 0;
				for (Object objNode:node.getChildNodes()){
					JSQLNode nextNode = (JSQLNode)objNode;
					if((Object)node instanceof JSQLArray){
						nextNode.setKey(String.valueOf(keyIndex++));
					}
					nextNode.setAntenode(node);
					if(checkNode(nextNode,tokens,branches_up,check)){
							out("returnAll: match found");
							out("returnAll: :adding node");
							addToResultSet(nextNode,resultSet,branches_up,cntx.include);
							returnAll(nextNode,resultSet,tokens,currentIndex,branches_up,cntx.check);
					}
				}
			}
		}
		
		private boolean checkNode(JSQLNode _node,JSQLTokens tokens,int branches_up, boolean check){
			
			boolean resume = true;
			cntx.include = true;
			cntx.check=check;
			
			if(check){
				
				out("checknode:checking node");
				
				
				ArrayList<String> array = new ArrayList<String>();
				
				cntx.include = true;
				int checkCount=0;
				
				for(String[] exceptions:tokens.exceptionPaths){
					array.clear();
					JSQLNode node = _node;
					checkCount++;
					while(node.getAntenode()!=null){
						array.add(node.getKey());
						node=node.getAntenode();
					}
					Collections.reverse(array);
					array.add(null);
					for(int i = 0;i<branches_up;i++){
						array.remove(0);
					}
					// debug
					out2("checknode: ");
					for(int i = 0;i<array.size();i++){
						out2(array.get(i)+" ");
					}
					boolean childrenMustMatch = false;
					int arrayIndex = 0;
					int index;
					for(index =0;index<exceptions.length;index++){
						out("checknode:iterating exception:"+1+" execption-index: "+index+ " value:" + exceptions[index]+" against path vlaue:"+array.get(arrayIndex));
						
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
									if(exceptions[index+1].equals(ALL_OPERATOR)){ //Must be end
										childrenMustMatch = true;
									}else{
										int newIndex = array.indexOf(exceptions[index+1]);
										if(newIndex==-1){
											out("checknode:did not found child");
											break;
										}else{
											for(int j = arrayIndex;j<newIndex;j++){
												array.set(j,null);
											}
											arrayIndex=newIndex;
											out("checknode:found child at :" +arrayIndex);
										}
									}
									index++;
								}
							}else{
								childrenMustMatch = true;
							}
							
						}else{
							if(!exceptions[index].equals(array.get(arrayIndex))){
								if(array.get(arrayIndex)!=null){
									checkCount--;
								}
								break;
							}
						}
						array.set(arrayIndex, null);
						arrayIndex++;
					}
					if(index==exceptions.length){
						cntx.include=false;
						if(childrenMustMatch){
							out("checknode: set stop iterating");
							resume = false;
						}else{
							if(array.get(arrayIndex)!=null){
								cntx.include=true;
							}
						}
					}
					if(!cntx.include){
						out("checknode: excluding this node");
						break;
					}else{
						out("checknode:including this node");
					}
				}
				if(checkCount==0){
					out("checknode:set stop checking");
					cntx.check = false;
				}
				out("checknode:end check");
			}
			return resume;
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
					out("parseQuery: " +KEYWORDS[precedence]+" "+tokens[1]);
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
					out("parseQuery: " +KEYWORDS[precedence]+" "+tokens[i+1]);
					tokenMap.put(KEYWORDS[precedence],tokens[i+1]);
				}
			}
			if(!selectFound){
				return null;
			}
			return tokenMap;
		}
		
		private ArrayList<JSQLTokens> getTokens(String queryString){
			out("start getTokens");
			ArrayList<JSQLTokens> queryList = new ArrayList<JSQLTokens>();
			String[] queries = queryString.split(QUERY_SEPARATOR);
			for(String query:queries){
				JSQLTokens tokens = new JSQLTokens();
				String[] paths = query.split(NOT_OPERATOR);
				int j = 0;
				for(String path:paths){
					out("getTokens: path:" +path);
					path = path.
							replaceAll(NOT_BACKSLASH+CHILD_OPERATOR,PATH_DELIMETER+CHILD_OPERATOR+PATH_DELIMETER).
							replaceAll(NOT_BACKSLASH+LEFT_BRACKET_ESCAPE,PATH_DELIMETER).
							replaceAll(NOT_BACKSLASH+RIGHT_BRACKET_ESCAPE,EMPTY);
					if(path.startsWith(PATH_DELIMETER+CHILD_OPERATOR))path = path.substring(1);
					if(path.endsWith(CHILD_OPERATOR+PATH_DELIMETER))path = path.substring(0,path.length()-1);
					
					/*
					 *  Forbidden sequences
					 *  
					 *  
					 */
					
					
					String[] keys = path.split(NOT_BACKSLASH+PATH_DELIMETER_REGEX,-1);
					int k = 0;
					for(String key:keys){
						out("getTokens: key:" +key);
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
					j++;
				}
				queryList.add(tokens);
			}
			return queryList;
		}
		
		private void handleException(Throwable e){
			System.out.println("error");
			e.printStackTrace();
		}
		
		//TODO delete
		private void out(Object msg){
			System.out.println(msg);
		}
		private void out2(Object msg){
			System.out.print(msg);
		}
}
