package src.JSQL;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class JSQLResultSet<T> extends ArrayList<T>{
	public ArrayList<String> identifiers = new ArrayList<String>();;
	public ArrayList<Integer> rowMarkers = new ArrayList<Integer>();
	public ArrayList<Integer> index = new ArrayList<Integer>();;
	public boolean success(){
		return !this.isEmpty();
	}

}
