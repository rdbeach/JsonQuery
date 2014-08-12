import java.util.ArrayList;
import java.util.Iterator;

import com.google.gson.Gson;



public class JsonQueryArray extends ArrayList<JsonQuery>{

	/**
	 * 
	 */
	
	private static final long serialVersionUID = 1L;
	
	public void jadd(int i,String value, Gson gson){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		JsonQuery node = gson.fromJson(json,JsonQuery.class);
		this.add(i,(JsonQuery)node._("obj"));
	}
	
	public void jadd(String value, Gson gson){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		JsonQuery node = gson.fromJson(json,JsonQuery.class);
		this.add((JsonQuery)node._("obj"));
	}
	
	public void jset(int i,String value, Gson gson){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		JsonQuery node = gson.fromJson(json,JsonQuery.class);
		this.set(i,(JsonQuery)node._("obj"));
	}
	
	public Object jsonQueryArraySet(int i,Object value){
		if(value instanceof JsonQuery){
			return super.set(i,(JsonQuery) value);
		}else{
			return super.set(i,new JsonQuery(value));
		}
	}
	
	public JsonQueryArray whereKeyEquals(String key){
		JsonQueryArray array = new JsonQueryArray();
		for(JsonQuery node:this){
			if(node.key.equals(key))
				array.add(node);
		}
		return array;
	}
}
