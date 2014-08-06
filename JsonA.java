import java.util.ArrayList;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class JsonA extends ArrayList<Object>{


	private static final long serialVersionUID = 1L;
	
	private Gson gson;
	
	private Gson getGson(){
		gson = (gson!=null? gson : (JsonO.ensureThreadSafety?new GsonBuilder().
					registerTypeAdapter(JsonO.class, new JsonODeserializer()).serializeNulls().
					create():JsonO.static_gson));
		return gson;
	}
	
	public JsonO o(int key){
		return (JsonO)this.get(key);
	}
	public JsonA a(int key){
		return (JsonA)this.get(key);
	}
	public String s(int key){
		return (String)this.get(key);
	}
	public int i(int key){
		return ((Integer)this.get(key)).intValue();
	}
	public boolean b(int key){
		return ((Boolean)this.get(key)).booleanValue();
	}
	public void jadd(int i,String value){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		try{
			JsonO j = getGson().fromJson(json,JsonO.class);
			this.add(i,(Object)j.get("obj"));
		}catch(com.google.gson.JsonSyntaxException e){
			this.add(i,(Object)value);
		}
	}
	public void jadd(String value){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		try{
			JsonO j = getGson().fromJson(json,JsonO.class);
			this.add((Object)j.get("obj"));
		}catch(com.google.gson.JsonSyntaxException e){
			this.add((Object)value);
		}
	}
	
	public void jset(int i,String value){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		try{
			JsonO j = getGson().fromJson(json,JsonO.class);
			this.set(i,(Object)j.get("obj"));
		}catch(com.google.gson.JsonSyntaxException e){
			this.set(i,(Object)value);
		}
	}
	public String toJson(){
		return (String) getGson().toJson(this);
	}
}
