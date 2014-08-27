package src;
import java.util.Iterator;

import src.JSQL.JSQLResultSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public abstract class JsonQuery {

	public static final boolean ensureThreadSafety = true;
	
	public static final Gson static_gson = new GsonBuilder()
			.disableHtmlEscaping()
			.registerTypeAdapter(JsonQueryNode.class, new JsonQueryDeserializer())
			.serializeNulls()
			.registerTypeAdapter(JsonQueryNode.class, new JsonQuerySerializer())
			.registerTypeAdapter(JsonQueryNumber.class,
					new JsonQueryNumberSerializer()).create();
	public static final String EMPTY = "";
	
	public static final JsonQuery fromJson(String json){
		Gson gson = new GsonBuilder().
				disableHtmlEscaping().
				registerTypeAdapter(JsonQueryNode.class, new JsonQueryDeserializer()).
				registerTypeAdapter(JsonQueryNode.class, new JsonQuerySerializer()).
				registerTypeAdapter(JsonQueryNumber.class, new JsonQueryNumberSerializer()).
				serializeNulls().
				create();
		try{
			return gson.fromJson(json,JsonQueryNode.class);
		}catch(Throwable e){
			e.printStackTrace();
		}
		return null;
	}
	
	public abstract String getKey();
	
	public abstract JsonQueryNode obj();
	
	public abstract JsonQueryNode arr();

	public abstract JsonQuery _(String key);

	public abstract JsonQuery _(int key);

	public abstract JsonQuery get(String path);

	public abstract JsonQuery node(String path);

	public abstract JSQLResultSet<JsonQuery> jsql(String queryString);
	
	public abstract Object val();

	public abstract String str();

	public abstract boolean bool();

	public abstract int i();

	public abstract long l();

	public abstract double d();

	public abstract Object val(String key);

	public abstract Object val(int key);

	public abstract String str(String key);

	public abstract String str(int key);

	public abstract boolean bool(String key);

	public abstract boolean bool(int key);

	public abstract int i(String key);

	public abstract int i(int key);

	public abstract long l(String key);

	public abstract long l(int key);

	public abstract double d(String key);

	public abstract double d(int key);

	public abstract JsonQuery set(Object value);
	
	public abstract JsonQuery set(String path, Object value);
	
	public abstract JsonQuery set(int key, Object value);
	
	public abstract JsonQuery jset(String value);
	
	public abstract JsonQuery jset(String path, String value);
	
	public abstract JsonQuery jset(int key, String value);
	
	public abstract JsonQuery put(String path, Object value);
	
	public abstract JsonQuery jput(String path, String value);
	
	public abstract JsonQuery add(Object value);
	
	public abstract JsonQuery add(int i, Object value);
	
	public abstract JsonQuery jadd(String value);
	
	public abstract JsonQuery jadd(int key, String value);

	public abstract JsonQuery clear();

	public abstract JsonQuery remove(String path);

	public abstract JsonQuery remove(int key);

	public abstract JsonQueryArray each();
	
	public abstract JsonQueryArray each(String path);

	public abstract Iterator<Object> iterator();

	public abstract boolean hasNext();

	public abstract boolean exists();
	
	public abstract boolean exists(String path);

	public abstract String type();
	
	public abstract String type(String path);

	public abstract boolean isLeaf();
	
	public abstract boolean isLeaf(String path);

	public abstract boolean isObject();
	
	public abstract boolean isObject(String path);

	public abstract boolean isArray();
	
	public abstract boolean isArray(String path);

	public abstract String toJson();
	
	public abstract String toJson(String path);

}