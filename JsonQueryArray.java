import java.util.ArrayList;

import com.google.gson.Gson;



public class JsonQueryArray extends ArrayList<JsonQueryNode>{

	/**
	 * 
	 */
	
	private static final long serialVersionUID = 1L;
	
	public void jadd(int i,String value, Gson gson){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		JsonQueryNode node = gson.fromJson(json,JsonQueryNode.class);
		this.add(i,(JsonQueryNode)node._("obj"));
	}
	
	public void jadd(String value, Gson gson){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		JsonQueryNode node = gson.fromJson(json,JsonQueryNode.class);
		this.add((JsonQueryNode)node._("obj"));
	}
	
	public void jset(int i,String value, Gson gson){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		JsonQueryNode node = gson.fromJson(json,JsonQueryNode.class);
		this.set(i,(JsonQueryNode)node._("obj"));
	}
	
	public Object jsonQueryArraySet(int i,Object value){
		if(value instanceof JsonQueryNode){
			return super.set(i,(JsonQueryNode) value);
		}else{
			return super.set(i,new JsonQueryNode(value));
		}
	}
	
	public JsonQueryArray whereKeyEquals(String key){
		JsonQueryArray array = new JsonQueryArray();
		for(JsonQueryNode node:this){
			if(node.key.equals(key))
				array.add(node);
		}
		return array;
	}
}
