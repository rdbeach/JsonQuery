package src.JSQL;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import src.JsonQueryNode;
import src.JsonQueryObject;


//import src.JsonQueryUtil;

//import src.JsonQueryObject;


public class JSQLEngine {
	
	
	private static final String SELECT = "select";
	private static final String UPDATE = "update";
	private static final String INSERT = "insert into";
	private static final String FROM = "from";
	private static final String SET_VALUE = "set value";
	private static final String SET = "set";
	private static final String WHERE = "where";
	private static final String ORDER_BY = "order by";
	private static final String LIMIT = "limit";
	
	
	private static final String EMPTY = "";
	private static final String ALL_OPERATOR = "*";
	private static final String ANY_OPERATOR = "?";
	private static final String CHILD_OPERATOR = ":";
	private static final String[] SETCLAUSE_OPERATORS = {"="};
	
	private static final int SELECTOR = 1;
	
	private boolean allow_duplicates=false;
	
	JSQLParser jsqlParser = new JSQLParser();
	
	private JSQLExpression evaluator;
	
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
		HashMap<String,String> clauses = jsqlParser.parseQueryString(queryString);
		if(clauses==null)return new JSQLResultSet<JSQLNode>();
		return executeJQL(node, clauses);
	}
	
	private JSQLResultSet<JSQLNode> executeJQL(JSQLNode node, HashMap<String,String> clauses){
			
			out("FROM clause");

			JSQLResultSet<JSQLNode> fromResultSet = new JSQLResultSet<JSQLNode>();
			
			String fromClause=ALL_OPERATOR;
			if(clauses.containsKey(FROM))fromClause=clauses.get(FROM);
			if(fromClause.equals(ALL_OPERATOR)){
				fromResultSet.add(node);
			}else{
				ArrayList<JSQLTokens> queries = jsqlParser.parseSelector(fromClause);
				executeJSQLClause(node,fromResultSet,queries);
			}
			
			out("UPDATE clause");
			
			if(clauses.containsKey(UPDATE)){
				String updateClause=clauses.get(UPDATE);
				ArrayList<JSQLTokens> queries = jsqlParser.parseSelector(updateClause);
				fromResultSet.clear();
				executeJSQLClause(node,fromResultSet,queries);
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
					//ArrayList<JSQLTokens> queries = jsqlParser.parseSelector(selectClause);
					//for(JSQLNode subNode: (JSQLResultSet<JSQLNode>)fromResultSet){
							executeSelectClause(selectResultSet,fromResultSet,selectClause);
					//}
				}
			}
			
			out("SET VALUE clause");
			
			if(clauses.containsKey(SET_VALUE)){
				String value=clauses.get(SET_VALUE);
				executeSetValueClause(fromResultSet,value);
				selectResultSet=fromResultSet;
			}
			
			out("SET clause");
			
			if(clauses.containsKey(SET)){
				String setClause=clauses.get(SET);
				executeSetClause(fromResultSet,setClause);
				selectResultSet=fromResultSet;
			}
			
			out("Finished");
			return selectResultSet;
		}
		
		private void executeJSQLClause(JSQLNode node, 
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
		
		private void executeSelectClause(JSQLResultSet<JSQLNode> selectResultSet,JSQLResultSet<JSQLNode> fromResultSet,String selectClause){
			out2("ExecuteSelectClause: "+selectClause+"\n");
			
			// get the selectors
			List<String> expressions = jsqlParser.parseSelectClause(selectClause);
			if(expressions==null){
				return;
			}
			List<JSQLTokenMap<Integer,Object>> variableMaps = new ArrayList<JSQLTokenMap<Integer,Object>>();
			List<Boolean> isExpressionList = new ArrayList<Boolean>();
			List<List<String>> tokensList = new ArrayList<List<String>>();
			
			for(String expr:expressions){
				
				if(evaluator==null)
					evaluator = new JSQLExpression();
				
				JSQLTokenMap<Integer,Object> variableMap = jsqlParser.parseExpression(expr,evaluator);
				
				if(variableMap==null)return;
				
				List<String> tokens = evaluator.getContext();
				
				listIt(variableMap.tokens,"SelectClause: List of variable values","value: ");
				listIt(variableMap.type,"SelectClause: List of variable types","type: ");
			
				variableMaps.add(variableMap);
				tokensList.add(tokens);
				
				boolean isExpression = false;
				if(tokens.size()>1){
					isExpression = true;
					
				}
				isExpressionList.add(isExpression);
			}
			
			int count=0; //TODO test only
			for (JSQLNode node:fromResultSet) {
				out2("\nExec SelectClause: iterating nodes "+ count++ +" for select clause\n");
				int i = 0;
				for(String expr:expressions){
					boolean isExpression = isExpressionList.get(i);
					if(isExpression==false){
						out2("Simple selection");
						ArrayList<JSQLTokens> query = jsqlParser.parseSelector(expr);
							executeJSQLClause(node,selectResultSet,query);
					}else{
						JSQLTokenMap<Integer,Object> variableMap = variableMaps.get(i);
						evaluator.setContext(expr,tokensList.get(i));
						boolean pass = false;
						List<JSQLResultSet<JSQLNode>> subsetList = new ArrayList<JSQLResultSet<JSQLNode>>();
						List<Object> valsList = new ArrayList<Object>();
						int j=0;
						for(Object variable:variableMap.tokens){
							if(variableMap.type.get(j)==SELECTOR){
								JSQLResultSet<JSQLNode> resultSet = (JSQLResultSet<JSQLNode>)execute(node,"Select * From "+variable+"");
								out2("Exec Selectlause: Got resultset");
								if(!resultSet.isEmpty()){
									pass=true;
									valsList.add(0);
									subsetList.add(resultSet);
								}else{
									out2("Resultset is empty!!");
									pass=false;
									break;
								}
							}else{
								valsList.add(variable);
								subsetList.add(null);
							}
							j++;
						}
						listIt(valsList,"Exec SelectClause: ValsList:\n------------------------------","value: ");
						if(pass){
							iterateResultSets(variableMap,valsList,subsetList,0,pass,node,expr,selectResultSet);
						}
					}
					i++;
				}
			}
			
		}
		
		private void executeWhereClause(JSQLResultSet<JSQLNode> array,String whereClause){
			out2("ExecuteWhereClause: "+whereClause+"\n");

			if(evaluator==null)
				evaluator = new JSQLExpression();
			
			JSQLTokenMap<Integer,Object> variableMap = jsqlParser.parseExpression(whereClause,evaluator);
			

			if(variableMap==null)return;
			
			listIt(variableMap.tokens,"List of variable values","value: ");
			listIt(variableMap.type,"List of variable types","type: ");
			
			Iterator<JSQLNode> iterator = ((JSQLResultSet<JSQLNode>)array).iterator();
			int count=0; //TODO test only
			while (iterator.hasNext()) {
				out2("\nExec WhereClause: iterating nodes "+count+++" for where clause\n");
			    JSQLNode node = iterator.next();
				boolean pass = false;
				List<JSQLResultSet<JSQLNode>> subsetList = new ArrayList<JSQLResultSet<JSQLNode>>();
				List<Object> valsList = new ArrayList<Object>();
				int i=0;
				for(Object variable:variableMap.tokens){
					if(variableMap.type.get(i)==SELECTOR){
						JSQLResultSet<JSQLNode> resultSet = (JSQLResultSet<JSQLNode>)execute(node,"Select * From "+variable+"");
						out2("Exec WhereClause: Got resultset");
						if(!resultSet.isEmpty()){
							pass=true;
							valsList.add(0);
							subsetList.add(resultSet);
						}else{
							out2("Resultset is empty!!");
							pass=false;
							break;
						}
					}else{
						valsList.add(variable);
						subsetList.add(null);
					}
					i++;
				}
				listIt(valsList,"Exec WhereClause: ValsList:\n------------------------------","value: ");
				if(pass){
					pass=iterateResultSets(variableMap,valsList,subsetList,0,pass,null,null,null);
				}
				if(!pass){
					out2("Pass failed: Removing subnode");
					iterator.remove();
				}
			}
		}
		
		private boolean iterateResultSets(JSQLTokenMap<Integer,Object> variableMap,
				List<Object> valsList,
				List<JSQLResultSet<JSQLNode>> subsetList,
				int i,
				boolean pass,
				JSQLNode node,
				String expr,
				JSQLResultSet<JSQLNode> selectResultSet){
			if(!pass)return false;
			out2("iterateResultSets: variable iteration "+i);
			if(variableMap.type.get(i)==SELECTOR){ // Path
				out2("variable name: " + variableMap.tokens.get(i));
				JSQLResultSet<JSQLNode> resultSet=subsetList.get(i);
				out2("Pulling resultset");
				if(!resultSet.isEmpty()){
					out2("Resultset is not empty. Iterating...");
				    for(int j = 0;j<resultSet.size();j++){
				    	out2("iterateResultSets: resultset iteration "+j);
				    	JSQLNode subnode = resultSet.get(j);
				    	if(subnode.isLeaf()){
				    		out2("Adding node to valslist: "+subnode.getElement());
							valsList.set(i,subnode.getElement());
						}else{
							out2("Subnode is not a leaf");
							pass=false;
							break;
						}
				    	out2("evaluates to : "+evaluator.eval(valsList));
				    	if(i!=variableMap.tokens.size()-1){
				    		pass=iterateResultSets(variableMap,valsList,subsetList,i+1,pass,node,expr,selectResultSet);
				    	}else{
				    		if(expr!=null){
				    			selectResultSet.add(node.createNewNode(evaluator.eval(valsList),expr)); 
				    		}else{
				    			if(evaluator.eval(valsList)!=BigDecimal.ONE){
								   out2("eval produced false");
								   pass=false;
								   break;
							    }
				    		}
						    
				    	}
				    }
				}else{
					pass=false;
				}
			}else{
				if(i!=variableMap.tokens.size()-1){
					pass=iterateResultSets(variableMap,valsList,subsetList,i+1,pass,node,expr,selectResultSet);
				}else{
					if(expr!=null){
						selectResultSet.add(node.createNewNode(evaluator.eval(valsList),expr));
		    		}else{
						if(evaluator.eval(valsList)!=BigDecimal.ONE){
						   out2("eval produced false");
						   pass=false;
					    }
		    		}
				}
			}
			return pass;
		}
			
		
		private void executeSetValueClause(JSQLResultSet<JSQLNode> array,String value){
			
			out("ExecuteSetValueClause: begin");
			
			Iterator<JSQLNode> iterator = ((JSQLResultSet<JSQLNode>)array).iterator();
			while (iterator.hasNext()) {

				JSQLNode node = iterator.next();
				out(node.getKey(),true);

				if(node.isLeaf()){
					out("ExecuteSetValueClause: Setting value: "+value);
					node.setElement(value);
				}
			}
		}
		
		private void executeSetClause(JSQLResultSet<JSQLNode> array,String setClause){
			
			out("ExecuteSetClause: begin");
			
			int operator_array_index=0;
			int operator_index=-1;
			int operator_length=1;
			
			for(int i = 0;i<SETCLAUSE_OPERATORS.length;i++){
				int length = SETCLAUSE_OPERATORS[i].length();
				int index = setClause.lastIndexOf(SETCLAUSE_OPERATORS[i]);
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
			
			out("In set clause: operator index:"+operator_index + " " + operator_length);
			
			if(operator_index!=-1&&operator_index+operator_length<setClause.length()){
				
				String[] operands = new String[2];
				operands[0] = setClause.substring(0,operator_index);
				operands[1] = setClause.substring(operator_index+operator_length,setClause.length());
				
				out("set clause op1:"+operands[0]);
				out("set clause op2:"+operands[1]);

				Iterator<JSQLNode> iterator = ((JSQLResultSet<JSQLNode>)array).iterator();
				while (iterator.hasNext()) {

				   JSQLNode node = iterator.next();
				   out(node.getKey(),true);

					for(JSQLNode subNode:(JSQLResultSet<JSQLNode>)execute(node,"Select * From "+operands[0]+"")){

						subNode  = (JSQLNode)subNode;
					    String value = operands[1];
					    
						switch (operator_array_index){
							case 0: // equals
								if(subNode.isLeaf()){
									out("ExecuteSetClause: Setting value: "+value);
									subNode.setElement(value);
								}
								break;
						}
						
					}
				}
			}else{
				err("Jsql Syntax error: invalid select clause.");
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
				out("Iterating path-index: "+index+ " value:" + tokens.path[index]);
				if(tokens.path[index].equals(ANY_OPERATOR)){
					next = true;
					break;
				}else if(tokens.path[index].equals(CHILD_OPERATOR)){
					child=true;
					break;
				}else{
					searchPath.add(jsqlParser.unescape(tokens.path[index]));
				}
			}
			if(!searchPath.isEmpty()){
				out("Build result: grabing path");
				JSQLNode nextNode = path(node,searchPath.toArray(),true);
				if(checkNode(nextNode,tokens,branches_up,check)){
					if(nextNode.getElement()!=null){
						if(index == endOfPath){
							out("Build result: adding from path: "+ nextNode.getKey());
							addToResultSet(nextNode,resultSet,branches_up,cntx.include);
						}else{
							out("Build result: continue");
							buildResultSet(nextNode,resultSet,tokens,index,branches_up,cntx.check);
						}
					}
				}
				return;
			}
			if(next){
				out("Build result: next");
				index++;
				int keyIndex = 0;
				for (Object objNode:node.getChildNodes()){
					JSQLNode nextNode = (JSQLNode)objNode;
					if(node.getElement() instanceof JSQLArray){
						nextNode.setKey(String.valueOf(keyIndex++));
					}
					nextNode.setAntenode(node);
					if(checkNode(nextNode,tokens,branches_up,check)){
						if(index == endOfPath){
							out("Build result: adding from next");
							addToResultSet(nextNode,resultSet,branches_up,cntx.include);
						}else{
							out("Build result: continue");
							buildResultSet(nextNode,resultSet,tokens,index,branches_up,cntx.check);
						}
					}
				}
				
				return;
			}
			if(child){
				index++;
				if(index<endOfPath){
					if(!tokens.path[index].equals(ANY_OPERATOR)){
						out("Build result: child search:"+index);
						keySearch(node,resultSet,tokens,index,branches_up,check);
					}else{
						returnAll(node,resultSet,tokens,index,branches_up,check);
					}
				}else{
					if(checkNode(node,tokens,branches_up,check)){
						out("Build result: adding from retern all + node");
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
				if(!allow_duplicates){
					if(resultSet.contains(node))return;
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
			out("In keySearch");
			int endOfPath = tokens.path.length-branches_up;
			String key = tokens.path[currentIndex];

			if(node.getElement() instanceof JSQLObject||node.getElement() instanceof JSQLArray){
				int keyIndex = 0;
				out("Instance of array: " +(node.getElement() instanceof JSQLArray));
				for (Object objNode:node.getChildNodes()){
					out("Iterating child nodes");
					JSQLNode nextNode = (JSQLNode)objNode;
					if(node.getElement() instanceof JSQLArray){
						nextNode.setKey(String.valueOf(keyIndex++));
					}
					nextNode.setAntenode(node);
					if(checkNode(nextNode,tokens,branches_up,check)){
						if(nextNode.getKey().equals(key)){
							out("keySearch: match found");
							if(currentIndex+1 == endOfPath){
								out("keySearch: :adding node");
								addToResultSet(nextNode,resultSet,branches_up,cntx.include);
							}else{
								out("keySearch: continue looking");
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
					if(node.getElement() instanceof JSQLArray){
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
				
				out("checknode: checking node");
				
				
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
					
					// DEBUG 
					out("checknode: ",true);
					for(int i = 0;i<array.size();i++){
						out(array.get(i)+" ",true);
					}
					
					boolean childrenMustMatch = false;
					int arrayIndex = 0;
					int index;
					for(index =0;index<exceptions.length;index++){
						out("checknode: iterating exception: "+1+" execption-index: "+index+ " value:" + exceptions[index]+" against path value:"+array.get(arrayIndex));
						
						if(exceptions[index].equals(ANY_OPERATOR)){
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
									if(exceptions[index+1].equals(ANY_OPERATOR)){ //Must be end
										childrenMustMatch = true;
									}else{
										int newIndex = array.indexOf(exceptions[index+1]);
										if(newIndex==-1){
											out("checknode: did not found child");
											break;
										}else{
											for(int j = arrayIndex;j<newIndex;j++){
												array.set(j,null);
											}
											arrayIndex=newIndex;
											out("checknode: found child at :" +arrayIndex);
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
					out("checknode: set stop checking");
					cntx.check = false;
				}
				out("checknode: end check");
			}
			return resume;
		}
		
		private void handleException(Throwable e){
			System.out.println("error");
			e.printStackTrace();
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
		
		private void out2(Object msg){
			System.out.println(msg);
		}
		
		private void listIt(List list,String start,String loopStr){
			out2("");
			out2(start);
			int count=0;
			for (Object elem : list) {
				out2(loopStr+count+ " "+ elem.toString());
				count++;
			}
			out2("");
		}
}
