import java.util.ArrayList;

import com.google.gson.Gson;


@SuppressWarnings("rawtypes")
public class JsonQueryArray extends ArrayList<JsonQuery>{

	/**
	 * 
	 */
	
	private static final long serialVersionUID = 1L;
	
	public void jadd(int i,String value, Gson gson){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		JsonQuery<?> j = gson.fromJson(json,JsonQuery.class);
		this.add(i,(JsonQuery<?>)j._("obj"));
	}
	
	public void jadd(String value, Gson gson){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		JsonQuery<?> j = gson.fromJson(json,JsonQuery.class);
		this.add((JsonQuery<?>)j._("obj"));
	}
	
	public void jset(int i,String value, Gson gson){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		JsonQuery<?> j = gson.fromJson(json,JsonQuery.class);
		this.set(i,(JsonQuery<?>)j._("obj"));
	}
	
	public Object jsonQueryArraySet(int i,Object value){
		if(value instanceof JsonQuery){
			return super.set(i,(JsonQuery<?>) value);
		}else{
			return super.set(i,new JsonQuery<Object>(value));
		}
	}
}
