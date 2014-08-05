import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class JsonO extends HashMap<String,Object>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final boolean ensureThreadSafety=true;
	
	public static final Gson static_gson = new GsonBuilder().
	registerTypeAdapter(JsonO.class, new JsonODeserializer()).serializeNulls().
	create();
	
	private Gson gson;
	
	public static JsonO fromJson(String json){
		Gson gson = new GsonBuilder().
				registerTypeAdapter(JsonO.class, new JsonODeserializer()).serializeNulls().
				create();
		return gson.fromJson(json,JsonO.class);
	}
	
	private Gson getGson(){
		gson = (gson!=null? gson : (ensureThreadSafety?new GsonBuilder().
					registerTypeAdapter(JsonO.class, new JsonODeserializer()).serializeNulls().
					create():static_gson));
		return gson;
	}
	
	public JsonO o(String key){
		return (JsonO)this.get(key);
	}
	
	@SuppressWarnings({ })
	public JsonA a(String key){
		return (JsonA)this.get(key);
	}
	
	public String s(String key){
		return (String)this.get(key);
	}
	public int i(String key){
		return ((Integer)this.get(key)).intValue();
	}
	public boolean b(String key){
		return ((Boolean)this.get(key)).booleanValue();
	}
	public void set(String key, String value){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		try{
			JsonO j = getGson().fromJson(json,JsonO.class);
			this.put(key,(Object)j.get("obj"));
		}catch(com.google.gson.JsonSyntaxException e){
			this.set(key,(Object)value);
		}
	}
	public void set(String key, Object value){
		this.put(key,value);
	}
	public String toJson(){
		return (String) getGson().toJson(this);
	}
}
