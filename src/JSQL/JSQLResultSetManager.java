package src.JSQL;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class JSQLResultSetManager {
	
	private static final String SELECT = "select";
	
	public Map<String,JSQLResultSet<JSQLNode>> resultSetMap = new HashMap<String,JSQLResultSet<JSQLNode>>();
	public ArrayList<String> fromClauseResultSetIdentifiers = new ArrayList<String>();
	public ArrayList<ArrayList<JSQLNode>> filteredResultSet = new ArrayList<ArrayList<JSQLNode>>();
	
	private Integer[] fromSetCounts;
	private Integer[] counters;
	
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
		initializeFromSetCounts();
		while ((resultVector = getResultVector())!=null) {
			filteredResultSet.add(resultVector);
		}
	}
	
	public JSQLResultSet<JSQLNode> getFinalResultSet(){
		JSQLResultSet<JSQLNode> finalResultSet = getResultSet(SELECT);
		for (ArrayList<JSQLNode> resultVector:filteredResultSet) {
			for (JSQLNode result:resultVector) {
				finalResultSet.add(result);
			}
		}
		return finalResultSet;
	}
	
	public void initializeFromSetCounts(){
		ArrayList<Integer> LfromListCounts = new ArrayList<Integer>();
		ArrayList<Integer> Lcounters = new ArrayList<Integer>();
		for(String result:fromClauseResultSetIdentifiers){
			LfromListCounts.add(getResultSet(result).size());
			Lcounters.add(0);
		}
		fromSetCounts=(Integer[])LfromListCounts.toArray(new Integer[0]);
		counters=(Integer[])Lcounters.toArray(new Integer[0]);
	}
	
	public ArrayList<JSQLNode> getResultVector(){
		//System.out.println(pos + " " + counters[pos]+ " " + fromSetCounts[pos]);
		ArrayList<JSQLNode> resultVector = new ArrayList<JSQLNode>();
		if(counters[0]==fromSetCounts[0]){
			counters[0]=0;
			if(1==counters.length){
				return null;
			}
			boolean stop = incrementNextCounter(1);
			if(stop){
				System.out.println("\nReturning null\n");
				return null;
			}
		}
		System.out.print("\ncombo");
		for(int i = 0;i<counters.length;i++){
			System.out.print(counters[i]);
			if(fromSetCounts[i]!=0){
				resultVector.add(getResultSet(fromClauseResultSetIdentifiers.get(i)).get(counters[i]));
			}else{
				resultVector.add(null);
			}
		}
		counters[0]++;
		return resultVector;
		
	}
	private boolean incrementNextCounter(int pos){
		counters[pos]++;
		if(counters[pos]==fromSetCounts[pos]){
			counters[pos]=0;
			if(pos+1==counters.length){
				return true;
			}
			return incrementNextCounter(pos+1);
		}
		return false;
	}
	

}
