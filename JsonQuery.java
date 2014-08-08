

import java.util.Iterator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LazilyParsedNumber;


public class JsonQuery implements Iterable<Object>{

	public static final boolean ensureThreadSafety=true;
	
	public static final Gson static_gson = new GsonBuilder().
	registerTypeAdapter(JsonQuery.class, new JsonQueryDeserializer()).serializeNulls().
	registerTypeAdapter(JsonQuery.class, new JsonQuerySerializer()).
	registerTypeAdapter(LazilyParsedNumber.class, new JsonQueryNumberSerializer()).
	create();
	
	
	public Object node;
	
	public static JsonQuery fromJson(String json){
		Gson gson = new GsonBuilder().
				registerTypeAdapter(JsonQuery.class, new JsonQueryDeserializer()).
				registerTypeAdapter(JsonQuery.class, new JsonQuerySerializer()).
				registerTypeAdapter(LazilyParsedNumber.class, new JsonQueryNumberSerializer()).
				serializeNulls().
				create();
		return gson.fromJson(json,JsonQuery.class);
		
	}
	
	private transient Gson gson;
	
	private Gson getGson(){
		gson = (gson!=null? gson : (ensureThreadSafety?new GsonBuilder().
					registerTypeAdapter(JsonQuery.class, new JsonQueryDeserializer()).
					registerTypeAdapter(JsonQuery.class, new JsonQuerySerializer()).
					registerTypeAdapter(LazilyParsedNumber.class, new JsonQueryNumberSerializer()).
					serializeNulls().
					create():static_gson));
		return gson;
	}
	
	public JsonQuery(){
		
	}
	
	public JsonQuery(Object node){
		this.node = node;
	}
	
	// Single node tree traversal operator
	
	public JsonQuery _(String key){
		if(node instanceof JsonQueryObject){
			return (JsonQuery)((JsonQueryObject)node).get(key);
		}
		return null;
	}
	
	public JsonQuery _(int key){
		if(node instanceof JsonQueryArray){
			return (JsonQuery)((JsonQueryArray)node).get(key);
		}
		return null;
	}
	
	// Javascript query tree traversal operator
	
	public JsonQuery $(String key) {
		String[] keys = key.replace("[",".").replace("]","").split("[.]");
		JsonQuery json = this;
		int i=0;
		for(i=0;i<keys.length;i++){
			if(JsonQueryUtil.isInteger(keys[i])){
				json = (JsonQuery) json._(Integer.parseInt(keys[i]));
			}else{
				json = (JsonQuery) json._(keys[i]);
			}
		}
		return json;
	}
	
	// Gets for the javascript queries
	
	public Object get() {
		if(node!=null){
			return node;
		}
		return null;
	}
	
	public String s() {
		if(node!=null){
			return (String) node;
		}
		return null;
	}
	
	public boolean b() {
		if(node!=null){
			return (Boolean) node;
		}
		return false;
	}
	
	public int i() {
		if(node!=null){
			return ((Number)node).intValue();
		}
		return 0;
	}
	
	public long l() {
		if(node!=null){
			return ((Number)node).longValue();
		}
		return 0;
	}
	
	public double d() {
		if(node!=null){
			return ((Number)node).doubleValue();
		}
		return 0;
	}
	
	// Gets for the single node traversal queries
	
	public Object get(String key) {
		if(node instanceof JsonQueryObject){
			JsonQuery json = (JsonQuery)((JsonQueryObject)node).get(key);
			if(json!=null) return json.node;
		}
		return null;
	}
	
	public Object get(int key) {
		if(node instanceof JsonQueryArray){
			JsonQuery json = (JsonQuery)((JsonQueryArray)node).get(key);
			if(json!=null) return json.node;
		}
		return null;
	}
	
	public String s(String key){
		if(node instanceof JsonQueryObject){
			JsonQuery json = (JsonQuery)((JsonQueryObject)node).get(key);
			if(json!=null) return (String)json.node;
		}
		return null;
	}
	
	public String s(int key){
		if(node instanceof JsonQueryArray){
			JsonQuery json = (JsonQuery)((JsonQueryArray)node).get(key);
			if(json!=null) return (String)json.node;
		}
		return null;
	}
	
	public boolean b(String key){
		if(node instanceof JsonQueryObject){
			JsonQuery json = (JsonQuery)((JsonQueryObject)node).get(key);
			if(json!=null) return (Boolean)json.node;
		}
		return false;
	}
	
	public boolean b(int key){
		if(node instanceof JsonQueryArray){
			JsonQuery json = (JsonQuery)((JsonQueryArray)node).get(key);
			if(json!=null) return (Boolean)json.node;
		}
		return false;
	}
	
	public int i(String key){
		if(node instanceof JsonQueryObject){
			JsonQuery json = (JsonQuery)((JsonQueryObject)node).get(key);
			if(json!=null) return ((Number)(json.node)).intValue();
		}
		return 0;
	}
	
	public int i(int key){
		if(node instanceof JsonQueryArray){
			JsonQuery json = (JsonQuery)((JsonQueryArray)node).get(key);
			if(json!=null) return ((Number)(json.node)).intValue();
		}
		return 0;
	}
	
	public long l(String key){
		if(node instanceof JsonQueryObject){
			JsonQuery json = (JsonQuery)((JsonQueryObject)node).get(key);
			if(json!=null) return ((Number)(json.node)).longValue();
		}
		return 0;
	}
	
	public long l(int key){
		if(node instanceof JsonQueryArray){
			JsonQuery json = (JsonQuery)((JsonQueryArray)node).get(key);
			if(json!=null) return ((Number)(json.node)).longValue();
		}
		return 0;
	}
	
	public double d(String key){
		if(node instanceof JsonQueryObject){
			JsonQuery json = (JsonQuery)((JsonQueryObject)node).get(key);
			if(json!=null) return ((Number)(json.node)).doubleValue();
		}
		return 0;
	}
	
	public double d(int key){
		if(node instanceof JsonQueryArray){
			JsonQuery json = (JsonQuery)((JsonQueryArray)node).get(key);
			if(json!=null) return ((Number)(json.node)).doubleValue();
		}
		return 0;
	}
	
	// Sets for the javascript queries
	
	
	public JsonQuery set(Object value){
		if(value instanceof JsonQuery){
			node = ((JsonQuery)value).node;
		}else{
			node = formatValue(value);
		}
		return this;
	}
	
	public JsonQuery jset(String value){
		if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		System.out.println("here");
		JsonQuery j = getGson().fromJson(json,JsonQuery.class);
		node =  j._("obj").node;
		return this;
	}
	
	// Sets for the single node traversal
	
	public JsonQuery set(String key, Object value){
		if(node instanceof JsonQueryObject){
			((JsonQueryObject)node).set(key,formatValue(value));
		}
		return this;
	}
	
	public JsonQuery set(int key, Object value){
		if(node instanceof JsonQueryArray){
			((JsonQueryArray)node).jsonQueryArraySet(key,formatValue(value));
		}
		return this;
	}
	
	public JsonQuery jset(String key, String value){
		if(node instanceof JsonQueryObject){
			((JsonQueryObject)node).jset(key,value,getGson());
		}
		return this;
	}
	
	public JsonQuery jset(int key, String value){
		if(node instanceof JsonQueryArray){
			((JsonQueryArray)node).jset(key,value,getGson());
		}
		return this;
	}
	
	
	
	// adds for javascript query and single node traversal
	
	
	
	public JsonQuery add(Object value){
		if(node instanceof JsonQueryArray){
			if(value instanceof JsonQuery){
				((JsonQueryArray)node).add((JsonQuery) value);
			}else{
				((JsonQueryArray)node).add(new JsonQuery(formatValue(value)));
			}
		}
		return this;
	}
	
	public JsonQuery add(int i,Object value){
		if(node instanceof JsonQueryArray){
			if(value instanceof JsonQuery){
				((JsonQueryArray)node).add(i,(JsonQuery) value);
			}else{
				((JsonQueryArray)node).add(i,new JsonQuery(formatValue(value)));
			}
		}
		return this;
	}
	
	public JsonQuery jadd(String value){
		if(node instanceof JsonQueryArray){
			((JsonQueryArray)node).jadd(value,getGson());
		}
		return this;
	}
	
	public JsonQuery jadd(int key, String value){
		if(node instanceof JsonQueryArray){
			((JsonQueryArray)node).jadd(key,value,getGson());
		}
		return this;
	}
	
	// Removing elements
	
	public JsonQuery remove(String key){
		if(node instanceof JsonQueryObject){
			((JsonQueryObject)node).remove(key);
		}
		return this;
	}
	
	public JsonQuery remove(int key){
		if(node instanceof JsonQueryArray){
			((JsonQueryArray)node).remove(key);
		}
		return this;
	}
	
	// Iterating
	
	public JsonQueryArray each(){
		if(node instanceof JsonQueryArray){
			return (JsonQueryArray) node;
		}
		return null;
	}

	public Iterator<Object> iterator() {
		// TODO this is a stub
		return null;
	}
	
	public boolean hasNext(){
		if(node instanceof JsonQueryArray){
			//Iterator<Object> it = ((JsonQueryArrayList)node).iterator();
			//if()
		}
		return false;
	}
	
	// Type Determination
	
	public String type(){
		if(node instanceof JsonQueryObject){
			return "object";
		}else if(node instanceof JsonQueryArray){
			return "array";
		}else if(node instanceof String){
			return "string";
		}else if(node instanceof JsonQueryNumber){
			return "number";
		}else if(node instanceof Boolean){
			return "boolean";
		}else{
			return "null";
		}
	}
	
	public boolean isLeaf(){
		if(node instanceof JsonQueryObject || node instanceof JsonQueryArray)
			return false;
		return true;
	}
	
	private  Object formatValue(Object value){
		if(value instanceof Number){
			value = new JsonQueryNumber(new LazilyParsedNumber(value.toString()));
		}
		return  value;
	}
		
	public boolean isObject(){
		if(node instanceof JsonQueryObject)
			return true;
		return false;
	}
		
	public boolean isArray(){
		if(node instanceof JsonQueryArray)
			return true;
		return false;
	}
	
	// Write to JSON string
	
	public String toJson(){
		return (String) getGson().toJson(this.node);
	}
	
}
