import java.util.ArrayList;
import com.google.gson.Gson;


public class JsonQueryArrayList extends ArrayList<Object>{

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
	
	public Object set(int i,Object value){
		if(value instanceof JsonQuery){
			return super.set(i,value);
		}else{
			return super.set(i,new JsonQuery<Object>(value));
		}
	}
}
