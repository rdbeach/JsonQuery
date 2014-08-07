
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LazilyParsedNumber;


public class JsonQuery<T>{

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
	
	public JsonQuery<?> _(String key){
		if(node instanceof JsonQueryHashMap){
			return (JsonQuery<?>)((JsonQueryHashMap)node).get(key);
		}
		return null;
	}
	
	public JsonQuery<?> _(int key){
		if(node instanceof JsonQueryArrayList){
			return (JsonQuery<?>)((JsonQueryArrayList)node).get(key);
		}
		return null;
	}
	
	public Object get(String key) {
		if(node instanceof JsonQueryHashMap){
			return ((JsonQuery<?>)((JsonQueryHashMap)node).get(key)).node;
		}
		return null;
	}
	
	public Object get(int key) {
		if(node instanceof JsonQueryArrayList){
			return ((JsonQuery<?>)((JsonQueryArrayList)node).get(key)).node;
		}
		return null;
	}
	
	public String s(String key){
		if(node instanceof JsonQueryHashMap){
			return (String)((JsonQuery<?>)((JsonQueryHashMap)node).get(key)).node;
		}
		return null;
	}
	
	public String s(int key){
		if(node instanceof JsonQueryArrayList){
			return (String)((JsonQuery<?>)((JsonQueryArrayList)node).get(key)).node;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public boolean b(String key){
		if(node instanceof JsonQueryHashMap){
			return (boolean)((JsonQuery<Object>)((JsonQueryHashMap)node).get(key)).node;
		}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	public boolean b(int key){
		if(node instanceof JsonQueryArrayList){
			return (boolean)((JsonQuery<Object>)((JsonQueryArrayList)node).get(key)).node;
		}
		return false;
	}
	
	public int i(String key){
		if(node instanceof JsonQueryHashMap){
			
			return ((Number)((JsonQuery<?>)((JsonQueryHashMap)node).get(key)).node).intValue();
		}
		return 0;
	}
	
	public int i(int key){
		if(node instanceof JsonQueryArrayList){
			System.out.println(((JsonQuery<?>)((JsonQueryArrayList)node).get(key)).node instanceof JsonQuery);
			return ((Number)((JsonQuery<?>)((JsonQueryArrayList)node).get(key)).node).intValue();
		}
		return 0;
	}
	
	public long l(String key){
		if(node instanceof JsonQueryHashMap){
			return ((Number)((JsonQuery<?>)((JsonQueryHashMap)node).get(key)).node).longValue();
		}
		return 0;
	}
	
	public long l(int key){
		if(node instanceof JsonQueryArrayList){
			return ((Number)((JsonQuery<?>)((JsonQueryArrayList)node).get(key)).node).longValue();
		}
		return 0;
	}
	
	public double d(String key){
		if(node instanceof JsonQueryHashMap){
			return ((Number)((JsonQuery<?>)((JsonQueryHashMap)node).get(key)).node).doubleValue();
		}
		return 0;
	}
	
	public double d(int key){
		if(node instanceof JsonQueryArrayList){
			return ((Number)((JsonQuery<?>)((JsonQueryArrayList)node).get(key)).node).doubleValue();
		}
		return 0;
	}
	
	public JsonQuery<T> set(String key, Object value){
		if(node instanceof JsonQueryHashMap){
			((JsonQueryHashMap)node).set(key,value);
		}
		return this;
	}
	
	public JsonQuery<T> jset(String key, String value){
		if(node instanceof JsonQueryHashMap){
			((JsonQueryHashMap)node).jset(key,value,getGson());
		}
		return this;
	}
	
	public JsonQuery<T> jset(int key, String value){
		if(node instanceof JsonQueryArrayList){
			((JsonQueryArrayList)node).jset(key,value,getGson());
		}
		return this;
	}
	
	public JsonQuery<T> add(Object value){
		if(node instanceof JsonQueryArrayList){
			((JsonQueryArrayList)node).add(new JsonQuery<Object>(value));
		}
		return this;
	}
	
	public JsonQuery<T> add(int i,Object value){
		if(node instanceof JsonQueryArrayList){
			((JsonQueryArrayList)node).add(i,new JsonQuery<Object>(value));
		}
		return this;
	}
	
	public JsonQuery<T> jadd(String value){
		if(node instanceof JsonQueryArrayList){
			((JsonQueryArrayList)node).jadd(value,getGson());
		}
		return this;
	}
	
	public JsonQuery<T> jadd(int key, String value){
		if(node instanceof JsonQueryArrayList){
			((JsonQueryArrayList)node).jadd(key,value,getGson());
		}
		return this;
	}
	
	public JsonQuery<T> remove(String key){
		if(node instanceof JsonQueryHashMap){
			((JsonQueryHashMap)node).remove(key);
		}
		return this;
	}
	
	public JsonQuery<T> remove(int key){
		if(node instanceof JsonQueryArrayList){
			((JsonQueryArrayList)node).remove(key);
		}
		return this;
	}
	
	public String toJson(){
		return (String) getGson().toJson(this.node);
	}
}
