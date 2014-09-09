package src.JSQL;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	private static final String SUBSELECT = "subselect";


	private static final String EMPTY = "";
	private static final String ALL_OPERATOR = "*";
	private static final String ANY_OPERATOR = "?";
	private static final String DESCENDENT_OPERATOR = ":";
	private static final String[] SETCLAUSE_OPERATORS = {"="};

	private static final int SELECTOR = 1;

	private boolean allow_duplicates=false;
	
	private JSQLNode rootNode;

	JSQLParser jsqlParser = new JSQLParser();

	JSQLResultSetManager resultManager;
	
	private Optimizer optimizer;

	private JSQLExpression evaluator;

	private JQLContext cntx;

	private JSQLEngine(){
		this.cntx = new JQLContext();
		this.resultManager = new JSQLResultSetManager();
		this.optimizer = new Optimizer();
	}

	public static final JSQLEngine getJQL(){
		return new JSQLEngine();
	}

	private class JQLContext{
		public boolean is_select=false;
		public int index=0;
		public boolean include=true;
		public boolean check=true;
	}
	
	private class Pair<K,T> {
	    private K x;
	    private T y;
	    public Pair(K x, T y){
	        this.x = x;
	        this.y = y;
	    }
	    public K getX(){ return x; }
	    public T getY(){ return y; }
	    public void setX(K x){ this.x = x; }
	    public void setY(T y){ this.y = y; }
	}
	
	private class Optimizer{
		
		private Stack<List<String>> rpnStack =  new Stack<List<String>>();
		private Stack<String> opcodes = new Stack<String>();
		private String opcode;
		
		List<JSQLToken<Integer,Object>> operandList;
		Map<Integer,ArrayList<JSQLSelector>> selectorsMap;
		//ArrayList<ArrayList<JSQLNode>> filteredResultSet;
		Map<ArrayList<ArrayList<JSQLNode>>,Integer> carryOver;
		public ArrayList<ArrayList<JSQLNode>> complementResultSet;
		
		public static final String NOOP = "NOOP";
		private static final String AND = "AND";
		private static final String OR = "OR";
	
		private boolean done = false;
		public boolean firstRun = true;
		
		public void initialize(
				List<JSQLToken<Integer,Object>> operandList,
				Map<Integer,ArrayList<JSQLSelector>> selectorsMap){
			this.operandList=operandList;
			this.selectorsMap=selectorsMap;
			this.carryOver = new HashMap<ArrayList<ArrayList<JSQLNode>>,Integer>();
			rpnStack.push(evaluator.rpn);
			firstRun=true;
		}

		public void optimizeExpression(){
			
			out2("Optimizing");
			
			out2("Initial RPN:");
			for (String token : rpnStack.peek()) {
				out2(" | " + token);
				out2("");
			}
			
			
			opcodes.push(NOOP);
			while(!done){
				int pos = findNextLogicalOperator();
				if(pos==-1){
					evaluator.rpn = rpnStack.pop();
					opcode = opcodes.pop();
					if(opcode==AND){
						doAndOptimization();
					}else if(opcode==OR){
						doOrOptimization();
					}else{
						doNoOptimization();
					}
				}else{
					out2("RPN for: "+opcode);
					getFirstOperand(pos);
				}
				if(rpnStack.empty()){
					done=true;
				}
			}
			//Push final result
			if(!resultManager.filteredResultSet.isEmpty()){
				resultManager.initializeVectorGenerator(null,resultManager.filteredResultSet);
				ArrayList<ArrayList<JSQLNode>> resultSet = resultManager.filteredResultSet;
				resultManager.filteredResultSet = new ArrayList<ArrayList<JSQLNode>>();	
				for(ArrayList<JSQLNode> inputVector: resultSet){
					if(inputVector.contains(null)){
						ArrayList<JSQLNode> resultVector;
						while ((resultVector = resultManager.generateVector(inputVector))!=null) {
							resultManager.filteredResultSet.add(resultVector);
						}
					}else{
						resultManager.filteredResultSet.add(inputVector);
					}
				}
			}
		}
		
		public int findNextLogicalOperator(){
			List<String> rpn = rpnStack.peek();
			for (int pos=rpn.size()-1 ; pos>=0 ; pos--) {
	            if (rpn.get(pos).equalsIgnoreCase(AND)){
	            	opcodes.push(AND);
	            	return pos;
				}
	            if (rpn.get(pos).equalsIgnoreCase(OR)){
	            	opcodes.push(OR);
	            	return pos;
				}
	        }
			return -1;
		}
		
		private void getFirstOperand(int pos){
			
			List<String> rpn = rpnStack.peek();
			List<String> next_rpn = new ArrayList<String>();
			rpnStack.push(next_rpn);
			
			ListIterator<String> it = rpn.listIterator(pos+1);
			int opCount=0;
			boolean targetArgumentFound = false;
			while(it.hasPrevious()){
				String token =it.previous();
				if(opCount==0||targetArgumentFound){
					it.remove();
				}
				if(targetArgumentFound){
					next_rpn.add(0,token);
					
				}
				opCount+=-evaluator.checkArity(token)+1;
				if(opCount==0){
					targetArgumentFound=true;
				}
				if(opCount==1){
					break;
				}
			}
			
			for (String token : next_rpn) {
				out2(" | " + token);
				out2("");
			}
		}
		
		private void doAndOptimization(){
			out2("do and optimization");
			
			executeOperation();
		}
		
		public void doOrOptimization(){
			out2("do or optimization");
			
			executeOperation();
		}
		
		private void doNoOptimization(){
			out2("No Op");
			
			executeOperation();
		}
		
		
		private void executeOperation(){
			List<JSQLToken<Integer,Object>> subOperandList = new ArrayList<JSQLToken<Integer,Object>>();
			Map<Integer,ArrayList<JSQLSelector>> subSelectorsMap = new HashMap<Integer,ArrayList<JSQLSelector>>();
			List<String> targets = new ArrayList<String>();
			complementResultSet = new ArrayList<ArrayList<JSQLNode>>();
			for (String token : evaluator.rpn) {
				if(token.startsWith("ARG")){
					int index = Integer.parseInt(token.substring(3));
					int listCount=0;
					for(JSQLToken<Integer,Object> operand:operandList){
						if(operand.index==index){
							break;
						}
						listCount++;
					}
					subOperandList.add(operandList.remove(listCount));
					if(selectorsMap.containsKey(index)){
						 for (JSQLSelector selector: selectorsMap.get(index)) {
							 String target = (selector.target.equals(EMPTY)?FROM:selector.target);
							 if(!targets.contains(target)){
								targets.add(target);
							 }
						 }
						subSelectorsMap.put(index, selectorsMap.remove(index));
					}
				}
			}
			for(JSQLToken<Integer,Object> operand:subOperandList){
				out2(opcode+": subOperandList: var: " + operand.value + " type: " + operand.type + " index: " + operand.index);
			}
			out2("RPN for: "+opcode);
			for (String token : evaluator.rpn) {
				out2(" | " + token);
				out2("");
			}
			
			filterResultStage1(subOperandList,subSelectorsMap,targets);
			
			// If a complementary result set exists, store it.
			if(!complementResultSet.isEmpty()){
				if(opcode.equals(AND)){
					out2("set carry over for index: "+opcodes.indexOf(OR));
					carryOver.put(complementResultSet,opcodes.indexOf(OR));
				}else if(opcode.equals(OR)){
					int index = opcodes.indexOf(AND);
					if(index==-1){
						index=0;
					}
					out2("set carry over for index: "+index);
					carryOver.put(complementResultSet,index);
				}
			}
			
			out2(carryOver.size());
			// Add back carry over
			if(carryOver.containsValue(opcodes.size())){
				Iterator<Entry<ArrayList<ArrayList<JSQLNode>>, Integer>> it = carryOver.entrySet().iterator();
				while(it.hasNext()){
					Entry<ArrayList<ArrayList<JSQLNode>>, Integer> entry = it.next();
					if(entry.getValue()==opcodes.size()){
						out2("add in carry over");
						resultManager.filteredResultSet.addAll(entry.getKey());
						it.remove();
					}
				}
			}
		}
		
		public void storeResult(ArrayList<JSQLNode> resultVector,boolean pass){
			if(opcode.equals(OR)){
				if(pass){
					out2("adding or pass vector");
					complementResultSet.add(resultVector);
				}else{
					out2("adding or failure vector");
					resultManager.filteredResultSet.add(resultVector);
				}
			}else{
				if(pass){
					out2("adding not or pass vector");
					resultManager.filteredResultSet.add(resultVector);
				}else if(opcodes.contains(OR)){
					out2("adding not or fail vector");
					complementResultSet.add(resultVector);
				}
			}
		}
	}

	public JSQLResultSet<JSQLNode> execute(JSQLNode node,String queryString){
		this.rootNode = node;
		HashMap<String,String> clauses = jsqlParser.parseQueryString(queryString);
		if(clauses==null)return new JSQLResultSet<JSQLNode>();
		return executeJQL(node, clauses);
	}

	private JSQLResultSet<JSQLNode> executeJQL(JSQLNode node, HashMap<String,String> clauses){

		out("FROM clause");

		String fromClause=ALL_OPERATOR;
		if(clauses.containsKey(FROM))fromClause=clauses.get(FROM);
		ArrayList<JSQLNode> resultVector = new ArrayList<JSQLNode>();
		resultVector.add(node);
		ArrayList<JSQLSelector> selectors = jsqlParser.parseSelection(fromClause,resultManager.dataSetIdentifiers);
		executeSelection(resultVector, FROM, selectors);

		out("UPDATE clause");

		if(clauses.containsKey(UPDATE)){
			resultManager.getResultSet(FROM).clear();
			resultVector = new ArrayList<JSQLNode>();
			resultVector.add(node);
			selectors = jsqlParser.parseSelection(clauses.get(UPDATE),resultManager.dataSetIdentifiers);
			executeSelection(resultVector,FROM,selectors);
		}

		out("WHERE clause");

		if(clauses.containsKey(WHERE)){
			executeWhereClause(clauses.get(WHERE));
		}else{
			resultManager.buildUnfilteredResultSet();
		}

		out("SELECT clause");

		JSQLResultSet<JSQLNode> result = new JSQLResultSet<JSQLNode>();

		if(clauses.containsKey(SELECT)){
			String selectClause=clauses.get(SELECT);
			if(selectClause.equals(ALL_OPERATOR)){
				result=resultManager.getFinalResultSet();
			}else{
				result = executeSelectClause(selectClause);
			}
		}

		out("SET VALUE clause");

		if(clauses.containsKey(SET_VALUE)){
			executeSetValueClause(resultManager.getResultSet(FROM),clauses.get(SET_VALUE));
			result=resultManager.getResultSet(FROM);
		}

		out("SET clause");

		if(clauses.containsKey(SET)){
			executeSetClause(resultManager.getResultSet(FROM),clauses.get(SET));
			result=resultManager.getResultSet(FROM);
		}

		resultManager.clear();

		out("Finished");
		return result;
	}

	private void executeWhereClause(String whereClause){
		out("ExecuteWhereClause: "+whereClause+"\n");

		if(evaluator==null)
			evaluator = new JSQLExpression();

		List<JSQLToken<Integer,Object>> operandList = jsqlParser.parseExpression(whereClause,evaluator);
		Map<Integer,ArrayList<JSQLSelector>> selectorsMap = new HashMap<Integer,ArrayList<JSQLSelector>>();

		if(operandList==null)return;
		
		for(JSQLToken<Integer,Object> operand:operandList){
			if(operand.type==SELECTOR){
				ArrayList<JSQLSelector> selectors = jsqlParser.parseSelection((String)operand.value,resultManager.dataSetIdentifiers);
				selectorsMap.put(operand.index,selectors);
			}
		}
		optimizer.initialize(operandList, selectorsMap);
		optimizer.optimizeExpression();
	}
	
	private void filterResultStage1(
			List<JSQLToken<Integer,Object>> operandList,
			Map<Integer,ArrayList<JSQLSelector>> selectorsMap,
			List<String> targets){
		ArrayList<ArrayList<JSQLNode>> filteredResultSet;
		Map<Integer,Object> valuesMap = new HashMap<Integer,Object>();
		
		for(JSQLToken<Integer,Object> operand:operandList){
			if(operand.type!=SELECTOR){
				valuesMap.put(operand.index,operand.value);
			}
		}

		resultManager.initializeVectorGenerator(targets,resultManager.filteredResultSet);
		if(optimizer.firstRun){
			optimizer.firstRun=false;
			filterResultStage2(operandList,selectorsMap,valuesMap,resultManager.initVector);
		}else{
			filteredResultSet = resultManager.filteredResultSet;
			resultManager.filteredResultSet = new ArrayList<ArrayList<JSQLNode>>();	
			for(ArrayList<JSQLNode> inputVector: filteredResultSet){
				filterResultStage2(operandList,selectorsMap,valuesMap,inputVector);
			}
		}
	}
	
	private void filterResultStage2(
			List<JSQLToken<Integer,Object>> operandList,
			Map<Integer,ArrayList<JSQLSelector>> selectorsMap,
			Map<Integer,Object> valuesMap,
			ArrayList<JSQLNode> inputVector){
		
		Map<Integer,JSQLResultSet<JSQLNode>> subsetMap = new HashMap<Integer,JSQLResultSet<JSQLNode>>();
		
		ArrayList<JSQLNode> resultVector;
		if(inputVector!=null&&!inputVector.isEmpty()&&inputVector.get(0)!=null)
		out2(((JsonQueryObject)inputVector.get(0).getElement()).get("name").str());
		while ((resultVector = resultManager.generateVector(inputVector))!=null) {
			out2(((JsonQueryObject)resultVector.get(0).getElement()).get("name").str());
			boolean pass = false;
			 for (Entry<Integer, ArrayList<JSQLSelector>> entry : selectorsMap.entrySet()) {
				resultManager.removeResultSet(SUBSELECT);
				executeSelection(resultVector, SUBSELECT, entry.getValue());
				JSQLResultSet<JSQLNode> resultSet = resultManager.getResultSet(SUBSELECT );
				if(resultSet.isEmpty()){
					resultSet.add(rootNode.createNewNode(null,null));
				}
				subsetMap.put(entry.getKey(),resultSet);
				pass=true;
			}
			 
			if(pass){
				pass=iterateOverSubsets(operandList,valuesMap,subsetMap,0,pass,null);
			}
				
			optimizer.storeResult(resultVector,pass);
		}
	}

	private JSQLResultSet<JSQLNode> executeSelectClause(String selectClause){
		out("ExecuteSelectClause: "+selectClause+"\n");
		// get the selectors
		List<String> expressions = jsqlParser.parseSelectClause(selectClause);
		if(expressions==null){
			return new JSQLResultSet<JSQLNode>();
		}
		List<List<JSQLToken<Integer,Object>>> operandLists = new ArrayList<List<JSQLToken<Integer,Object>>>();
		List<Boolean> isExpressionList = new ArrayList<Boolean>();
		List<List<String>> tokensList = new ArrayList<List<String>>();

		for(String expr:expressions){

			if(evaluator==null)
				evaluator = new JSQLExpression();

			List<JSQLToken<Integer,Object>> operandList = jsqlParser.parseExpression(expr,evaluator);

			if(operandList==null)new JSQLResultSet<JSQLNode>();

			List<String> tokens = evaluator.getContext();

			operandLists.add(operandList);
			tokensList.add(tokens);

			boolean isExpression = false;
			if(tokens.size()>1){
				isExpression = true;

			}
			isExpressionList.add(isExpression);
		}

		int row=1; 
		for (ArrayList<JSQLNode> resultVector:resultManager.filteredResultSet) {
			out("\nExec SelectClause: iterating row "+ row +" for select clause\n");
			JSQLResultSet<JSQLNode> result = resultManager.getResultSet(SELECT);
			result.rowMarkers.add(result.size());
			int i = 0;
			for(String expr:expressions){
				boolean isExpression = isExpressionList.get(i);
				if(isExpression==false){
					out("Simple selection");
					ArrayList<JSQLSelector> selectors = jsqlParser.parseSelection(expr,resultManager.dataSetIdentifiers);
					executeSelection(resultVector, SELECT, selectors);
				}else{
					List<JSQLToken<Integer,Object>> operandList = operandLists.get(i);
					evaluator.setContext(tokensList.get(i));
					
					boolean pass = false;
					Map<Integer,JSQLResultSet<JSQLNode>> subsetMap = new HashMap<Integer,JSQLResultSet<JSQLNode>>();
					Map<Integer,Object> valsList = new HashMap<Integer,Object>();
					int j=0;
					for(JSQLToken<Integer,Object> operand:operandList){
						if(operand.type==SELECTOR){
							//JSQLResultSet<JSQLNode> resultSet = new JSQLResultSet<JSQLNode>();
							resultManager.removeResultSet(SUBSELECT );
							ArrayList<JSQLSelector> selectors = jsqlParser.parseSelection((String)operand.value,resultManager.dataSetIdentifiers);
							executeSelection(resultVector, SUBSELECT, selectors);
							JSQLResultSet<JSQLNode> resultSet = resultManager.getResultSet(SUBSELECT );
							out("Exec Selectlause: Got resultset");
							if(resultSet.isEmpty()){
								resultSet.add(rootNode.createNewNode(null,null));
							}
							subsetMap.put(operand.index,resultSet);
							pass=true;
						}else{
							valsList.put(operand.index,operand.value);
						}
						j++;
					}
					if(pass){
						iterateOverSubsets(operandList,valsList,subsetMap,0,pass,expr);
					}
				}
				i++;
			}
		}
		return resultManager.getResultSet(SELECT);
	}

	private void executeSetValueClause(JSQLResultSet<JSQLNode> array,String value){

		out("ExecuteSetValueClause: begin");

		Iterator<JSQLNode> iterator = ((JSQLResultSet<JSQLNode>)array).iterator();
		while (iterator.hasNext()) {

			JSQLNode result = iterator.next();
			out(result.getKey(),true);

			if(result.isLeaf()){
				out("ExecuteSetValueClause: Setting value: "+value);
				result.setElement(value);
			}
		}
	}

	// TODO rework this function
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

				JSQLNode result = iterator.next();
				out(result.getKey(),true);
				//TODO this is not correct anymore
				for(JSQLNode subResult:(JSQLResultSet<JSQLNode>)execute(result,"Select * From "+operands[0]+"")){

					JSQLNode subNode  = subResult;
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
	
	private boolean iterateOverSubsets(List<JSQLToken<Integer,Object>> operandList,
			Map<Integer,Object> valsMap,
			Map<Integer,JSQLResultSet<JSQLNode>> subsetMap,
			int i,
			boolean pass,
			String expr){
		if(!pass)return false;
		out(operandList.get(i).type==SELECTOR);
		out("iterateResultSets: variable iteration "+i);
		if(operandList.get(i).type==SELECTOR){ // Path
			int index = operandList.get(i).index;
			out("variable name: " + operandList.get(i).value);
			JSQLResultSet<JSQLNode> resultSet=subsetMap.get(index);
			out("Pulling resultset");
			if(!resultSet.isEmpty()){
				out("Resultset is not empty. Iterating...");
				for(int j = 0;j<resultSet.size();j++){
					out("iterateResultSets: resultset iteration "+j);
					JSQLNode subnode = resultSet.get(j);
					if(subnode.isLeaf()){
						out("Adding node to valslist: "+subnode.getElement());
						valsMap.put(index,subnode.getElement());
					}else{
						out("Subnode is not a leaf");
						pass=false;
						break;
					}
					//out("evaluates to : "+evaluator.eval(valsMap));
					if(i!=operandList.size()-1){
						pass=iterateOverSubsets(operandList,valsMap,subsetMap,i+1,pass,expr);
					}else{
						if(expr!=null){
							JSQLResultSet<JSQLNode> selectResultSet = resultManager.getResultSet(SELECT);
							if(!selectResultSet.identifiers.contains(jsqlParser.formatForOutput(expr))){
								selectResultSet.identifiers.add(jsqlParser.formatForOutput(expr));
							}
							cntx.index = selectResultSet.identifiers.indexOf(jsqlParser.formatForOutput(expr));
							selectResultSet.add(rootNode.createNewNode(evaluator.eval(valsMap),jsqlParser.formatForOutput(expr)));
							selectResultSet.index.add(cntx.index); 
						}else{
							if(evaluator.eval(valsMap)!=BigDecimal.ONE){
								out2("eval produced false");
								pass=false;
								break;
							}
							out2("eval produced true");
						}

					}
				}
			}else{
				pass=false;
			}
		}else{
			if(i!=operandList.size()-1){
				pass=iterateOverSubsets(operandList,valsMap,subsetMap,i+1,pass,expr);
			}else{
				if(expr!=null){
					JSQLResultSet<JSQLNode> selectResultSet = resultManager.getResultSet(SELECT);
					if(!selectResultSet.identifiers.contains(jsqlParser.formatForOutput(expr))){
						selectResultSet.identifiers.add(jsqlParser.formatForOutput(expr));
					}
					cntx.index = selectResultSet.identifiers.indexOf(jsqlParser.formatForOutput(expr));
					selectResultSet.add(rootNode.createNewNode(evaluator.eval(valsMap),jsqlParser.formatForOutput(expr)));
					selectResultSet.index.add(cntx.index); 
				}else{
					if(evaluator.eval(valsMap)!=BigDecimal.ONE){
						out2("eval produced false");
						pass=false;
					}
					out2("eval produced true");
				}
			}
		}
		return pass;
	}

	private void executeSelection(ArrayList<JSQLNode> resultVector, 
			String identifier,
			ArrayList<JSQLSelector> selectors
			){
		for(JSQLSelector selector:selectors){
			JSQLNode result;
			if(!selector.target.equals(EMPTY)){
				int index = resultManager.dataSetIdentifiers.indexOf(selector.target);
				if(index!=-1){
					result = resultVector.get(index);
				}else{
					return;
				}
			}else{
				result = resultVector.get(0);
			}
			executeSelector(result,identifier,selector);
		}
	}
	
	private void executeSelector(JSQLNode result,
			String identifier,
			JSQLSelector selector){
		String resultset_identifier=identifier;
		if(identifier.equals(FROM)){
			if(!selector.identifier.equals(EMPTY))
				resultset_identifier=selector.identifier;
			if(!resultManager.dataSetIdentifiers.contains(resultset_identifier))
				if(resultset_identifier.equals(FROM)){
					resultManager.dataSetIdentifiers.add(0,resultset_identifier);
				}else{
					resultManager.dataSetIdentifiers.add(resultset_identifier);
				}
		}
		JSQLResultSet<JSQLNode> resultSet = resultManager.getResultSet(resultset_identifier);
		if(identifier.equals(SELECT)){
			cntx.is_select=true;
			out("setting selector id..........."+selector.id);
			if(!resultSet.identifiers.contains(selector.id)){
				resultSet.identifiers.add(selector.id);
			}
			cntx.index = resultSet.identifiers.indexOf(selector.id);
		}else{
			cntx.is_select=false;
		}
		if(selector.path[0].equals(ALL_OPERATOR)&&selector.path.length==1){
			out(cntx.is_select);
			if(cntx.is_select){
				resultSet.index.add(cntx.index);
			}
			resultSet.add(result);
			return;
		}
	
		int branches_up=0;
		int i = selector.path.length-1;
		while(i>0&&(selector.path[i].equals(EMPTY))){
			branches_up++;
			i--;
		}
		buildResultSet(result,resultSet,selector,0,branches_up,(selector.exceptionPaths==null||selector.exceptionPaths.size()==0?false:true));
	}

	private void buildResultSet(
			JSQLNode node,
			JSQLResultSet<JSQLNode> resultSet,
			JSQLSelector tokens,
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
			}else if(tokens.path[index].equals(DESCENDENT_OPERATOR)){
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
						addToResultSet(nextNode,resultSet,branches_up);
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
						addToResultSet(nextNode,resultSet,branches_up);
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
					addToResultSet(node,resultSet,branches_up);
					returnAll(node,resultSet,tokens,index,branches_up,cntx.check);
				}
			}
		}
	}

	private void addToResultSet(JSQLNode node,JSQLResultSet<JSQLNode> resultSet,int branches_up){
		if(cntx.include){
			for(int i = 0;i<branches_up;i++){
				node=node.getAntenode();
			}
			if(!allow_duplicates&&!cntx.is_select){
				if(resultSet.contains(node))return;
			}
			if(cntx.is_select){
				resultSet.index.add(cntx.index);
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
			JSQLSelector tokens,
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
							addToResultSet(nextNode,resultSet,branches_up);
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
			JSQLSelector tokens,
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
					addToResultSet(nextNode,resultSet,branches_up);
					returnAll(nextNode,resultSet,tokens,currentIndex,branches_up,cntx.check);
				}
			}
		}
	}

	private boolean checkNode(JSQLNode _node,JSQLSelector tokens,int branches_up, boolean check){

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

					}else if(exceptions[index].equals(DESCENDENT_OPERATOR)){

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
	
	private void out2(Object msg){
		System.out.println(msg);
	}

	private void out(Object msg, boolean inline){
		//System.out.print(msg);
	}

	private void listIt(List list,String start,String loopStr){
		out("");
		out(start);
		int count=0;
		for (Object elem : list) {
			out(loopStr+count+ " "+ elem.toString());
			count++;
		}
		out("");
	}
}
