package src.JSQL;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class JSQLResultSet<T> extends ArrayList<T>{
	
	public boolean success(){
		return !this.isEmpty();
	}

}
