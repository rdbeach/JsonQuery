package src;
import java.util.HashMap;
import java.util.Iterator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;



public class JsonQueryDirect extends HashMap<String,Object>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final boolean ensureThreadSafety=true;
	
	public static final JsonParser static_parser = new JsonParser();
	
	public static JsonQueryDirect fromJson(String json){
		JsonElement elem =  new JsonParser().parse(json);
		return new JsonQueryDirect(elem);
	}
	
	private JsonParser parser;
	
	public JsonElement elem;
	
	public JsonQueryDirect(JsonElement elem){
		this.elem = elem;
	}
	
	private JsonParser getParser(){
		parser = (parser!=null? parser : (ensureThreadSafety?new JsonParser():static_parser));
		return parser;
	}
	
	public JsonQueryDirect _(String key){
		 if(elem.isJsonObject()){
			JsonObject e = elem.getAsJsonObject();
			elem = e.get(key);
		 }else if(elem.isJsonNull()){
			 throw new JsonSyntaxException("The element is undefined.");
		 }else{
			 throw new JsonSyntaxException("Type mismatch.");
		 }
		 return this;
	}
	
	public JsonQueryDirect _(int key){
		 if(elem.isJsonArray()){
			JsonArray e = elem.getAsJsonArray();
			elem = e.get(key);
		 }else if(elem.isJsonNull()){
			 throw new JsonSyntaxException("The element is undefined.");
		 }else{
			 throw new JsonSyntaxException("Type mismatch.");
		 }
		 return this;
	}
	
	public String s(String key){
		 System.out.println(elem.isJsonObject());
		 if(elem.isJsonObject()){
				JsonElement value = elem.getAsJsonObject().get(key);
				
				if(value.isJsonPrimitive()){
					System.out.println(elem.isJsonObject());
					JsonPrimitive p = value.getAsJsonPrimitive();
					if(p.isString()){
						return p.getAsString();
					}
				}
				throw new JsonSyntaxException("Type mismatch.");
		 }else if(elem.isJsonNull()){
				 throw new JsonSyntaxException("The element is undefined.");
		 }else{
				 throw new JsonSyntaxException("Type mismatch.");
		 }
	}
	
	public String s(int key){
		 if(elem.isJsonArray()){
			 	JsonElement value = elem.getAsJsonArray().get(key);
				if(value.isJsonPrimitive()){
					JsonPrimitive p = value.getAsJsonPrimitive();
					if(p.isString()){
						return p.getAsString();
					}
				}
				throw new JsonSyntaxException("Type mismatch.");
		 }else if(elem.isJsonNull()){
				 throw new JsonSyntaxException("The element is undefined.");
		 }else{
				 throw new JsonSyntaxException("Type mismatch.");
		 }
	}
	
	public int i(String key){
		 if(elem.isJsonObject()){
			 JsonElement value = elem.getAsJsonObject().get(key);
				if(value.isJsonPrimitive()){
					JsonPrimitive p = value.getAsJsonPrimitive();
					if(p.isNumber()){
						return p.getAsInt();
					}
				}
				throw new JsonSyntaxException("Type mismatch.");
		 }else if(elem.isJsonNull()){
				 throw new JsonSyntaxException("The element is undefined.");
		 }else{
				 throw new JsonSyntaxException("Type mismatch.");
		 }
	}
	
	public int i(int key){
		 if(elem.isJsonArray()){
			 	JsonElement value = elem.getAsJsonArray().get(key);
				if(value.isJsonPrimitive()){
					JsonPrimitive p = value.getAsJsonPrimitive();
					if(p.isNumber()){
						return p.getAsInt();
					}
				}
				throw new JsonSyntaxException("Type mismatch.");
		 }else if(elem.isJsonNull()){
				 throw new JsonSyntaxException("The element is undefined.");
		 }else{
				 throw new JsonSyntaxException("Type mismatch.");
		 }
	}
	
	public boolean b(String key){
		 if(elem.isJsonObject()){
			 	JsonElement value = elem.getAsJsonObject().get(key);
				if(value.isJsonPrimitive()){
					JsonPrimitive p = value.getAsJsonPrimitive();
					if(p.isBoolean()){
						return p.getAsBoolean();
					}
				}
				throw new JsonSyntaxException("Type mismatch.");
		 }else if(elem.isJsonNull()){
				 throw new JsonSyntaxException("The element is undefined.");
		 }else{
				 throw new JsonSyntaxException("Type mismatch.");
		 }
	}
	
	public boolean b(int key){
		 if(elem.isJsonArray()){
				JsonElement value = elem.getAsJsonArray().get(key);
				if(value.isJsonPrimitive()){
					JsonPrimitive p = value.getAsJsonPrimitive();
					if(p.isBoolean()){
						return p.getAsBoolean();
					}
				}
				throw new JsonSyntaxException("Type mismatch.");
		 }else if(elem.isJsonNull()){
				 throw new JsonSyntaxException("The element is undefined.");
		 }else{
				 throw new JsonSyntaxException("Type mismatch.");
		 }
	}
	
	public void jset(String key, String value){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		try{
			JsonElement element = parser.parse(json).getAsJsonObject().get("obj");
			if(elem.isJsonObject()){
				JsonObject obj = elem.getAsJsonObject();
				obj.add(key, element);
				return;
			}
			throw new JsonSyntaxException("Type mismatch.");
		}catch(com.google.gson.JsonSyntaxException e){
			this.set(key, value);
		}
	}
	
	public void jadd(String value){
	    if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		try{
			JsonElement element = parser.parse(json).getAsJsonObject().get("obj");
			if(elem.isJsonArray()){
				JsonArray arr = elem.getAsJsonArray();
				arr.add(element);
				return;
			}
			throw new JsonSyntaxException("Type mismatch.");
		}catch(com.google.gson.JsonSyntaxException e){
			this.add(value);
		}
	}
	public void set(String key, int i){
		if(elem.isJsonObject()){
			JsonObject obj = elem.getAsJsonObject();
			Number num = i;
			obj.addProperty(key,num);
			return;
		}
		throw new JsonSyntaxException("Type mismatch.");
	}
	public void set(String key, boolean i){
		if(elem.isJsonObject()){
			JsonObject obj = elem.getAsJsonObject();
			Boolean bool = i;
			obj.addProperty(key,bool);
			return;
		}
		throw new JsonSyntaxException("Type mismatch.");
	}
	public void set(String key, Object o){
		if(elem.isJsonObject()){
			JsonObject obj = elem.getAsJsonObject();
			if(o instanceof JsonElement){
				obj.add(key, (JsonElement)o);
			}else if (o instanceof String){
				obj.addProperty(key, (String)o);
			}else if (o instanceof Number){
				obj.addProperty(key, (Number)o);
			}else if (o instanceof Boolean){
				obj.addProperty(key, (Boolean)o);
				
			}else if (o instanceof Character){
				obj.addProperty(key, (Character)o);
			}
			return;
		}
		throw new JsonSyntaxException("Type mismatch.");
	}
	
	public void set(int key, Object o){
		if(elem.isJsonArray()){
			JsonArray arr = elem.getAsJsonArray();
			int index = 0;
			Iterator<JsonElement> it = arr.iterator();
			while(it.hasNext()){
				//Make an arraylist. Put the elements in the arraylist
				//remove the elemnent from the jsonarray
				//after your done add all
			}
			
			return;
		}
		throw new JsonSyntaxException("Type mismatch.");
	}

	public void set(int key, int i){
		
	}
	public void set(int key, boolean b){
		
	}
	public void add(String s){
		
	}
	public String toJson(){
		return (String) elem.toString();
	}
}
