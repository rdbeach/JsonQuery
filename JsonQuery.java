

import java.util.Iterator;
import java.util.Map.Entry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LazilyParsedNumber;


public class JsonQuery implements Iterable<Object>{

	public static final boolean ensureThreadSafety=true;
	
	public static final Gson static_gson = new GsonBuilder().
		disableHtmlEscaping().
		registerTypeAdapter(JsonQuery.class, new JsonQueryDeserializer()).serializeNulls().
		registerTypeAdapter(JsonQuery.class, new JsonQuerySerializer()).
		registerTypeAdapter(JsonQueryNumber.class, new JsonQueryNumberSerializer()).
		create();
	
	public transient String key="";
	public Object element;
	
	public transient JsonQuery antenode;

	
	public static JsonQuery fromJson(String json){
		Gson gson = new GsonBuilder().
				disableHtmlEscaping().
				registerTypeAdapter(JsonQuery.class, new JsonQueryDeserializer()).
				registerTypeAdapter(JsonQuery.class, new JsonQuerySerializer()).
				registerTypeAdapter(JsonQueryNumber.class, new JsonQueryNumberSerializer()).
				serializeNulls().
				create();
		try{
			return gson.fromJson(json,JsonQuery.class);
		}catch(Throwable e){
			e.printStackTrace();
		}
		return null;
		
	}
	

	private transient Gson gson;
	
	private Gson getGson(){
		gson = (gson!=null? gson : (ensureThreadSafety?new GsonBuilder().
					disableHtmlEscaping().
					registerTypeAdapter(JsonQuery.class, new JsonQueryDeserializer()).
					registerTypeAdapter(JsonQuery.class, new JsonQuerySerializer()).
					registerTypeAdapter(JsonQueryNumber.class, new JsonQueryNumberSerializer()).
					serializeNulls().
					create():static_gson));
		return gson;
	}
	
	public JsonQuery(){
		
	}
	
	public JsonQuery(Object node){
		this.element = node;
	}
	
	public JsonQuery(Object node, String key){
		this.element = node;
		this.key = key;
	}
	
	// Single node tree traversal operator
	
	public JsonQuery _(String key){
		if(element instanceof JsonQueryObject){
			JsonQuery j = (JsonQuery)((JsonQueryObject)element).get(key);
			if(j!=null)return j;
		}
		return new JsonQuery(null,null);
	}
	
	public JsonQuery _(int key){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQuery j = (JsonQuery)((JsonQueryArray)element).get(key);
				if(j!=null)return j;
			}
		}
		return new JsonQuery(null,null);
	}
	
	private String[] getKeys(String keyString){
		return keyString.replace("[",".").replace("]","").split("(?<!\\\\)[.]");
	}
	public JsonQuery node(String keyString) {
		String[] keys = getKeys(keyString);
		JsonQuery json = this;
		try{
			int i=0;
			for(i=0;i<keys.length;i++){
				if(JsonQueryUtil.isInteger(keys[i])){
					int index = Integer.parseInt(keys[i]);
					JsonQuery j = (JsonQuery) json._(index);
					if(!j.exists()){
						if(!(json.element instanceof JsonQueryArray)) {
							if(json.element==null){
								json.element = new JsonQueryArray();
							}else{
								return new JsonQuery(null,null);
							}
						}
						JsonQueryArray array = (JsonQueryArray) json.element;
						for(int k = array.size(); k<index+1; k++){
							array.add(new JsonQuery(null));
						}
						j.key="";
						array.set(index,j);
						if(i+1<keys.length){
							if(JsonQueryUtil.isInteger(keys[i+1])){
								j.element=new JsonQueryArray();
							}else{
								j.element=new JsonQueryObject();
							}
						}
					}
					json=j;
				}else{
					JsonQuery j = (JsonQuery) json._(keys[i]);
					if(!j.exists()){
						j.key=keys[i];
						if(!(json.element instanceof JsonQueryObject)) {
							if(json.element==null){
								json.element = new JsonQueryObject();
							}else{
								return new JsonQuery(null,null);
							}
						}
						((JsonQueryObject) json.element).put(keys[i],j);
						if(i+1<keys.length){
							if(JsonQueryUtil.isInteger(keys[i+1])){
								j.element=new JsonQueryArray();
							}else{
								j.element=new JsonQueryObject();
							}
						}
					}
					json=j;
				}
			}
			return json;
		}catch(Throwable e){
			handleException(e);
		}
		return new JsonQuery(null,null);
	}
	// Javascript query tree traversal operator
	
	public JsonQuery get(String keyString) {
		String[] keys = getKeys(keyString);
		JsonQuery json = this;
		try{
			int i=0;
			for(i=0;i<keys.length;i++){
				if(JsonQueryUtil.isInteger(keys[i])){
					json = (JsonQuery) json._(Integer.parseInt(keys[i]));
				}else{
					json = (JsonQuery) json._(keys[i]);
				}
				if(json==null)break;
			}
			if(json!=null)return json;
		}catch(Throwable e){
			handleException(e);
		}
		return new JsonQuery(null,null);
	}
	
	// Gets for the javascript queries
	
	public Object val() {
			return element;
	}
	
	public String str() {
		if(element!=null){
			if(element instanceof JsonQueryObject||element instanceof JsonQueryArray){
				return this.toJson();
			}else{
				return element.toString();
			}
		}
		return null;
	}
	
	public boolean bool() {
		if(element!=null){
			return (Boolean) element;
		}
		return false;
	}
	
	public int i() {
		if(element!=null){
			return ((Number)element).intValue();
		}
		return 0;
	}
	
	public long l() {
		if(element!=null){
			return ((Number)element).longValue();
		}
		return 0;
	}
	
	public double d() {
		if(element!=null){
			return ((Number)element).doubleValue();
		}
		return 0;
	}
	
	// Gets for the single node traversal queries
	
	public Object val(String key) {
		if(element instanceof JsonQueryObject){
			JsonQuery json = (JsonQuery)((JsonQueryObject)element).get(key);
			if(json!=null) return json.element;
		}
		return null;
	}
	
	public Object val(int key) {
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQuery json = (JsonQuery)((JsonQueryArray)element).get(key);
				if(json!=null) return json.element;
			}
		}
		return null;
	}
	
	public String str(String key) {
		if(element instanceof JsonQueryObject){
			JsonQuery json = (JsonQuery)((JsonQueryObject)element).get(key);
			if(json!=null){
				if(json.element instanceof JsonQueryObject||json.element instanceof JsonQueryArray){
					return json.toJson();
				}else if(json.element!=null){
					return json.element.toString();
				}
			}
		}
		return null;
	}
	
	public String str(int key) {
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQuery json = (JsonQuery)((JsonQueryArray)element).get(key);
				if(json!=null){
					if(json.element instanceof JsonQueryObject||json.element instanceof JsonQueryArray){
						return json.toJson();
					}else if(json.element!=null){
						return json.element.toString();
					}
				}
			}
		}
		return null;
	}
	
	public boolean bool(String key){
		if(element instanceof JsonQueryObject){
			JsonQuery json = (JsonQuery)((JsonQueryObject)element).get(key);
			if(json!=null) return (Boolean)json.element;
			
		}
		return false;
	}
	
	public boolean bool(int key){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQuery json = (JsonQuery)((JsonQueryArray)element).get(key);
				if(json!=null) return (Boolean)json.element;
			}
		}
		return false;
	}
	
	public int i(String key){
		if(element instanceof JsonQueryObject){
			JsonQuery json = (JsonQuery)((JsonQueryObject)element).get(key);
			if(json!=null&&json.element!=null) return ((Number)(json.element)).intValue();
		}
		return 0;
	}
	
	public int i(int key){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQuery json = (JsonQuery)((JsonQueryArray)element).get(key);
				if(json!=null&&json.element!=null) return ((Number)(json.element)).intValue();
			}
		}
		return 0;
	}
	
	public long l(String key){
		if(element instanceof JsonQueryObject){
				JsonQuery json = (JsonQuery)((JsonQueryObject)element).get(key);
				if(json!=null&&json.element!=null) return ((Number)(json.element)).longValue();
		}
		return 0;
	}
	
	public long l(int key){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
					JsonQuery json = (JsonQuery)((JsonQueryArray)element).get(key);
					if(json!=null&&json.element!=null) return ((Number)(json.element)).longValue();
			}
		}
		return 0;
	}
	
	public double d(String key){
		if(element instanceof JsonQueryObject){
			JsonQuery json = (JsonQuery)((JsonQueryObject)element).get(key);
			if(json!=null&&json.element!=null) return ((Number)(json.element)).doubleValue();
		}
		return 0;
	}
	
	public double d(int key){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQuery json = (JsonQuery)((JsonQueryArray)element).get(key);
				if(json!=null&&json.element!=null) return ((Number)(json.element)).doubleValue();
			}
		}
		return 0;
	}
	
	// Sets for the javascript queries
	
	
	public JsonQuery set(Object value){
		if(value instanceof JsonQuery){
			value = this;
		}else{
			element = formatValue(value);
		}
		return this;
	}
	
	public JsonQuery jset(String value){
		if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		try{
			JsonQuery j = getGson().fromJson(json,JsonQuery.class);
			element =  j._("obj").element;
		}catch(Throwable e){
			handleException(e);
		}
		return this;
	}
	
	// Sets for the single node traversal
	
	public JsonQuery set(String key, Object value){
		if(element == null)element = new JsonQueryObject();
		if(element instanceof JsonQueryObject){
			((JsonQueryObject)element).set(key,formatValue(value));
		}
		return this;
	}
	
	public JsonQuery set(int key, Object value){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				((JsonQueryArray)element).jsonQueryArraySet(key,formatValue(value));
			}
		}
		return this;
	}
	
	public JsonQuery jset(String key, String value){
		if(element == null)element = new JsonQueryObject();
		if(element instanceof JsonQueryObject){
			try{
				((JsonQueryObject)element).jset(key,value,getGson());
			}catch(Throwable e){
				handleException(e);
			}
		}
		return this;
	}
	
	public JsonQuery jset(int key, String value){
		if(element instanceof JsonQueryArray){
			try{
				if(key<((JsonQueryArray)element).size()){
					((JsonQueryArray)element).jset(key,value,getGson());
				}
			}catch(Throwable e){
				handleException(e);
			}
		}
		return this;
	}
	
	
	
	// adds for javascript query and single node traversal
	
	
	
	public JsonQuery add(Object value){
		if(element == null)element = new JsonQueryArray();
		if(element instanceof JsonQueryArray){
			if(value instanceof JsonQuery){
				((JsonQueryArray)element).add((JsonQuery) value);
			}else{
				((JsonQueryArray)element).add(new JsonQuery(formatValue(value)));
			}
		}
		return this;
	}
	
	public JsonQuery add(int i,Object value){
		
		if(element == null && i==0)element = new JsonQueryArray();
		if(element instanceof JsonQueryArray){
			try{
				if(i<=((JsonQueryArray)element).size()){
					if(value instanceof JsonQuery){
						((JsonQueryArray)element).add(i,(JsonQuery) value);
					}else{
						((JsonQueryArray)element).add(i,new JsonQuery(formatValue(value)));
					}
				}
			}catch(Throwable e){
				handleException(e);
			}
		}
		return this;
	}
	
	public JsonQuery jadd(String value){
		if(element == null)element = new JsonQueryArray();
		if(element instanceof JsonQueryArray){
			try{
				((JsonQueryArray)element).jadd(value,getGson());
			}catch(Throwable e){
				handleException(e);
			}
		}
		return this;
	}
	
	public JsonQuery jadd(int key, String value){
		if(element == null&&key==0)element = new JsonQueryArray();
		if(element instanceof JsonQueryArray){
			try{
				if(key<=((JsonQueryArray)element).size()){
					((JsonQueryArray)element).jadd(key,value,getGson());
				}
			}catch(Throwable e){
				handleException(e);
			}
		}
		return this;
	}
	
	// Removing elements
	
	public JsonQuery toNull(){
		this.element=null;
		return this;
	}
	
	public JsonQuery remove(String key){
		if(element instanceof JsonQueryObject){
			((JsonQueryObject)element).remove(key);
		}
		return this;
	}
	
	public JsonQuery remove(int key){
		if(element instanceof JsonQueryArray){
			((JsonQueryArray)element).remove(key);
		}
		return this;
	}
	
	// Iterating
	
	public JsonQueryArray each(){
		if(element instanceof JsonQueryArray){
			return (JsonQueryArray) element;
		}
		if(element instanceof JsonQueryObject){
			JsonQueryArray  array = new JsonQueryArray();
			for (Entry<String, JsonQuery> entry : ((JsonQueryObject)element).entrySet()) {
			    String key = entry.getKey();
			    Object value = entry.getValue();
			    JsonQuery j = new JsonQuery(((JsonQuery)value).element,key);
			    array.add(j);
			}
			return array;
		}
		return new JsonQueryArray();
	}

	public Iterator<Object> iterator() {
		// TODO this is a stub
		return null;
	}
	
	public boolean hasNext(){
		// TODO
		if(element instanceof JsonQueryArray){
			//Iterator<Object> it = ((JsonQueryArrayList)node).iterator();
			//if()
		}
		return false;
	}
	
	// Type Determination
	
	public boolean exists(){
		if(key==null)return false;
		return true;
	}
	
	public String type(){
		if(element instanceof JsonQueryObject){
			return "object";
		}else if(element instanceof JsonQueryArray){
			return "array";
		}else if(element instanceof String){
			return "string";
		}else if(element instanceof JsonQueryNumber){
			return "number";
		}else if(element instanceof Boolean){
			return "boolean";
		}else{
			return "null";
		}
	}
	
	public boolean isLeaf(){
		if(element instanceof JsonQueryObject || element instanceof JsonQueryArray)
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
		if(element instanceof JsonQueryObject)
			return true;
		return false;
	}
		
	public boolean isArray(){
		if(element instanceof JsonQueryArray)
			return true;
		return false;
	}
	
	// Exception Handling
	
	private void handleException(Throwable e){
		System.out.println("error");
		e.printStackTrace();
	}
	
	// Write to JSON string
	
	public String toJson(){
		try{
			return (String) getGson().toJson(this.element);
		}catch(Throwable e){
			handleException(e);
		}
		return null;
	}
	
	
}
