

import java.util.Iterator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LazilyParsedNumber;


public class JsonQuery<T> implements Iterable<Object>{

	public static final boolean ensureThreadSafety=true;
	
	public static final Gson static_gson = new GsonBuilder().
	registerTypeAdapter(JsonQuery.class, new JsonQueryDeserializer()).serializeNulls().
	registerTypeAdapter(JsonQuery.class, new JsonQuerySerializer()).
	registerTypeAdapter(LazilyParsedNumber.class, new JsonQueryNumberSerializer()).
	create();
	
	
	public T node;
	
	public static JsonQuery<?> fromJson(String json){
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
	
	public JsonQuery(T node){
		this.node = node;
	}
	
	// Single node tree traversal operator
	
	public JsonQuery<?> _(String key){
		if(node instanceof JsonQueryObject){
			return (JsonQuery<?>)((JsonQueryObject)node).get(key);
		}
		return null;
	}
	
	public JsonQuery<?> _(int key){
		if(node instanceof JsonQueryArray){
			return (JsonQuery<?>)((JsonQueryArray)node).get(key);
		}
		return null;
	}
	
	// Javascript query tree traversal operator
	
	@SuppressWarnings("unchecked")
	public JsonQuery<?> $(String key) {
		String[] keys = key.replace("[",".").replace("]","").split("[.]");
		JsonQuery<T> json = this;
		int i=0;
		for(i=0;i<keys.length;i++){
			if(JsonQueryUtil.isInteger(keys[i])){
				json = (JsonQuery<T>) json._(Integer.parseInt(keys[i]));
			}else{
				json = (JsonQuery<T>) json._(keys[i]);
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
			JsonQuery<?> json = (JsonQuery<?>)((JsonQueryObject)node).get(key);
			if(json!=null) return json.node;
		}
		return null;
	}
	
	public Object get(int key) {
		if(node instanceof JsonQueryArray){
			JsonQuery<?> json = (JsonQuery<?>)((JsonQueryArray)node).get(key);
			if(json!=null) return json.node;
		}
		return null;
	}
	
	public String s(String key){
		if(node instanceof JsonQueryObject){
			JsonQuery<?> json = (JsonQuery<?>)((JsonQueryObject)node).get(key);
			if(json!=null) return (String)json.node;
		}
		return null;
	}
	
	public String s(int key){
		if(node instanceof JsonQueryArray){
			JsonQuery<?> json = (JsonQuery<?>)((JsonQueryArray)node).get(key);
			if(json!=null) return (String)json.node;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public boolean b(String key){
		if(node instanceof JsonQueryObject){
			JsonQuery<Object> json = (JsonQuery<Object>)((JsonQueryObject)node).get(key);
			if(json!=null) return (Boolean)json.node;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public boolean b(int key){
		if(node instanceof JsonQueryArray){
			JsonQuery<Object> json = (JsonQuery<Object>)((JsonQueryArray)node).get(key);
			if(json!=null) return (Boolean)json.node;
		}
		return false;
	}
	
	public int i(String key){
		if(node instanceof JsonQueryObject){
			JsonQuery<?> json = (JsonQuery<?>)((JsonQueryObject)node).get(key);
			if(json!=null) return ((Number)(json.node)).intValue();
		}
		return 0;
	}
	
	public int i(int key){
		if(node instanceof JsonQueryArray){
			JsonQuery<?> json = (JsonQuery<?>)((JsonQueryArray)node).get(key);
			if(json!=null) return ((Number)(json.node)).intValue();
		}
		return 0;
	}
	
	public long l(String key){
		if(node instanceof JsonQueryObject){
			JsonQuery<?> json = (JsonQuery<?>)((JsonQueryObject)node).get(key);
			if(json!=null) return ((Number)(json.node)).longValue();
		}
		return 0;
	}
	
	public long l(int key){
		if(node instanceof JsonQueryArray){
			JsonQuery<?> json = (JsonQuery<?>)((JsonQueryArray)node).get(key);
			if(json!=null) return ((Number)(json.node)).longValue();
		}
		return 0;
	}
	
	public double d(String key){
		if(node instanceof JsonQueryObject){
			JsonQuery<?> json = (JsonQuery<?>)((JsonQueryObject)node).get(key);
			if(json!=null) return ((Number)(json.node)).doubleValue();
		}
		return 0;
	}
	
	public double d(int key){
		if(node instanceof JsonQueryArray){
			JsonQuery<?> json = (JsonQuery<?>)((JsonQueryArray)node).get(key);
			if(json!=null) return ((Number)(json.node)).doubleValue();
		}
		return 0;
	}
	
	// Sets for the javascript queries
	
	@SuppressWarnings("unchecked")
	public JsonQuery<T> set(Object value){
		if(value instanceof JsonQuery){
			node = (T) ((JsonQuery<?>)value).node;
		}else{
			node = (T) value;
		}
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public JsonQuery<T> jset(String value){
		if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		System.out.println("here");
		JsonQuery<?> j = getGson().fromJson(json,JsonQuery.class);
		node = (T) j._("obj").node;
		return this;
	}
	
	// Sets for the single node traversal
	
	public JsonQuery<T> set(String key, Object value){
		if(node instanceof JsonQueryObject){
			((JsonQueryObject)node).set(key,value);
		}
		return this;
	}
	
	public JsonQuery<T> set(int key, Object value){
		if(node instanceof JsonQueryArray){
			((JsonQueryArray)node).jsonQueryArraySet(key,value);
		}
		return this;
	}
	
	public JsonQuery<T> jset(String key, String value){
		if(node instanceof JsonQueryObject){
			((JsonQueryObject)node).jset(key,value,getGson());
		}
		return this;
	}
	
	public JsonQuery<T> jset(int key, String value){
		if(node instanceof JsonQueryArray){
			((JsonQueryArray)node).jset(key,value,getGson());
		}
		return this;
	}
	
	
	
	// adds for javascript query and single node traversal
	
	
	
	@SuppressWarnings("rawtypes")
	public JsonQuery<T> add(Object value){
		if(node instanceof JsonQueryArray){
			if(value instanceof JsonQuery){
				((JsonQueryArray)node).add((JsonQuery) value);
			}else{
				((JsonQueryArray)node).add(new JsonQuery<Object>(value));
			}
		}
		return this;
	}
	
	@SuppressWarnings("rawtypes")
	public JsonQuery<T> add(int i,Object value){
		if(node instanceof JsonQueryArray){
			if(value instanceof JsonQuery){
				((JsonQueryArray)node).add(i,(JsonQuery) value);
			}else{
				((JsonQueryArray)node).add(i,new JsonQuery<Object>(value));
			}
		}
		return this;
	}
	
	public JsonQuery<T> jadd(String value){
		if(node instanceof JsonQueryArray){
			((JsonQueryArray)node).jadd(value,getGson());
		}
		return this;
	}
	
	public JsonQuery<T> jadd(int key, String value){
		if(node instanceof JsonQueryArray){
			((JsonQueryArray)node).jadd(key,value,getGson());
		}
		return this;
	}
	
	public JsonQuery<T> remove(String key){
		if(node instanceof JsonQueryObject){
			((JsonQueryObject)node).remove(key);
		}
		return this;
	}
	
	public JsonQuery<T> remove(int key){
		if(node instanceof JsonQueryArray){
			((JsonQueryArray)node).remove(key);
		}
		return this;
	}
	
	public String toJson(){
		return (String) getGson().toJson(this.node);
	}
	
	public boolean hasNext(){
		if(node instanceof JsonQueryArray){
			//Iterator<Object> it = ((JsonQueryArrayList)node).iterator();
			//if()
		}
		return false;
	}
	
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
}
