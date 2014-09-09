package src.JSQL;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class JSQLResultSetManager {
	
	private static final String SELECT = "select";
	private static final String ALL_OPERATOR = "*";
	
	public Map<String,JSQLResultSet<JSQLNode>> resultSetMap = new HashMap<String,JSQLResultSet<JSQLNode>>();
	public ArrayList<String> dataSetIdentifiers = new ArrayList<String>();
	public ArrayList<ArrayList<JSQLNode>> filteredResultSet = new ArrayList<ArrayList<JSQLNode>>();
	//public ArrayList<ArrayList<JSQLNode>> ORResultSet;
	public ArrayList<JSQLNode> initVector;
	
	private Integer[] dataSetCounts;
	private Integer[] iterationCounts;
	private Integer[] comboCounters;
	private Integer[] targets;
	
	public void createResultSet(String identifier){
		JSQLResultSet<JSQLNode> resultSet = new JSQLResultSet<JSQLNode>();
		resultSetMap.put(identifier, resultSet);
	}
	
	public JSQLResultSet<JSQLNode> getResultSet(String identifier){
		if(!resultSetMap.containsKey(identifier)){
			createResultSet(identifier);
		}
		return resultSetMap.get(identifier);
	}
	
	public void clearResultSet(String identifier){
		JSQLResultSet<JSQLNode> resultSet = getResultSet(identifier);
		resultSet.clear();
	}
	
	public void removeResultSet(String identifier){
		resultSetMap.remove(identifier);
	}
	
	public void clear(){
		resultSetMap.clear();
	}
	
	public void buildUnfilteredResultSet(){
		ArrayList<JSQLNode> resultVector;
		initializeVectorGenerator(null,filteredResultSet);
		while ((resultVector = generateVector(initVector))!=null) {
			filteredResultSet.add(resultVector);
		}
	}
	
	public JSQLResultSet<JSQLNode> getFinalResultSet(){
		JSQLResultSet<JSQLNode> finalResultSet = getResultSet(SELECT);
		finalResultSet.rowMarkers.add(finalResultSet.size());
		finalResultSet.identifiers.add(ALL_OPERATOR);
		for (ArrayList<JSQLNode> resultVector:filteredResultSet) {
			for (JSQLNode result:resultVector) {
				finalResultSet.add(result);
				finalResultSet.index.add(0);
			}
		}
		return finalResultSet;
	}
	
	public void initializeVectorGenerator(
			List<String> targetSets,
			ArrayList<ArrayList<JSQLNode>> resultSet){
		dataSetCounts=new Integer[dataSetIdentifiers.size()];
		iterationCounts=new Integer[dataSetIdentifiers.size()];
		comboCounters=new Integer[dataSetIdentifiers.size()];
		targets=new Integer[dataSetIdentifiers.size()];
		
		if(!resultSet.isEmpty()){
			initVector = resultSet.get(0);
		}else{
			initVector = new ArrayList<JSQLNode>();
		}
		
		int resultSetCount = 0;
		int i=0;
		for(String target:dataSetIdentifiers){
			int size = getResultSet(target).size();
			resultSetCount += size;
			if(targetSets!=null&&!targetSets.contains(target)){
				targets[i]=0;
			}else{
				targets[i]=1;
			}
			if(resultSet.isEmpty()){
				initVector.add(null);
			}
			dataSetCounts[i]=size;
			
			if(size==0){
				size=1;
			}
			iterationCounts[i]=size;
			comboCounters[i]=0;
			i++;
		}
		if(resultSetCount==0){
			iterationCounts[0]=0;
		}
	}
	
	public ArrayList<JSQLNode> generateVector(ArrayList<JSQLNode> inputVector){
		ArrayList<JSQLNode> outputVector = new ArrayList<JSQLNode>();
		if(comboCounters[0]==(targets[0]==1&&inputVector.get(0)==null?iterationCounts[0]:1)){
			comboCounters[0]=0;
			if(1==comboCounters.length){
				return null;
			}
			boolean stop = incrementNextCounter(1,inputVector);
			if(stop){
				System.out.println("\nReturning null\n");
				return null;
			}
		}
		System.out.print("\ncombo ");
		for(int i = 0;i<comboCounters.length;i++){
			System.out.print(comboCounters[i]);
			JSQLNode node = inputVector.get(i);
			if(targets[i]==1&&node==null&&dataSetCounts[i]!=0){
				outputVector.add(getResultSet(dataSetIdentifiers.get(i)).get(comboCounters[i]));
			}else{
				outputVector.add(node);
			}
		}
		comboCounters[0]++;
		return outputVector;
		
	}
	private boolean incrementNextCounter(int pos,ArrayList<JSQLNode> inputVector){
		comboCounters[pos]++;
		if(comboCounters[pos]==(targets[pos]==1&&inputVector.get(pos)==null?iterationCounts[pos]:1)){
			comboCounters[pos]=0;
			if(pos+1==comboCounters.length){
				return true;
			}
			return incrementNextCounter(pos+1,inputVector);
		}
		return false;
	}
}
