package src;
import java.util.HashMap;

import src.JSQL.JSQLObject;

import com.google.gson.Gson;



public class JsonQueryObject extends HashMap<String,JsonQueryNode> implements JSQLObject{

	/**
	 * 
	 */
	
	private static final long serialVersionUID = 1L;
	
	public void jset(String key, String value, Gson gson){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		JsonQueryNode node = gson.fromJson(json,JsonQueryNode.class);
		this.put(key,(JsonQueryNode)node._("obj"));
	}
	
	public void set(String key, Object value){
		if(value instanceof JsonQueryNode){
			this.put(key,(JsonQueryNode) value);
		}else{
			this.put(key,new JsonQueryNode(value));
		}
	}
}
