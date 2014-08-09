
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;



public class JsonQueryDeserializer implements JsonDeserializer<JsonQuery> {

  public JsonQuery deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
      throws JsonParseException {
	  
	   JsonQueryObject map = new JsonQueryObject();
	   final JsonObject jsonObject= json.getAsJsonObject();
	   final  Set<Map.Entry<String,JsonElement>> elemset = jsonObject.entrySet();
    
	   for (Map.Entry<String, JsonElement> elem : elemset){
		   // System.out.println(elem.getKey() + "/" + elem.getValue());
		    String key = elem.getKey();
		    JsonElement value = elem.getValue();
		    map.put(key,get_value(value,typeOfT,context));
	   }

	   return new JsonQuery(map);
  }
  
  public JsonQuery get_value(final JsonElement elem, final Type typeOfT, final JsonDeserializationContext context){
	  if(elem.isJsonObject()){
	    	JsonQuery innermap = deserialize(elem,typeOfT,context);
	    	return innermap;
	  }
	  else if(elem.isJsonPrimitive()){
	  		if(elem.getAsJsonPrimitive().isString()){
					return new JsonQuery(elem.getAsJsonPrimitive().getAsString());
	  		}else if(elem.getAsJsonPrimitive().isNumber()){
	  			   return new JsonQuery(new JsonQueryNumber(elem.getAsJsonPrimitive().getAsNumber()));
	  		}else if(elem.getAsJsonPrimitive().isBoolean()){
		    	   return new JsonQuery((Boolean)elem.getAsJsonPrimitive().getAsBoolean());
	  		}
	  }
	  else if(elem.isJsonArray()){
		    Collection<JsonQuery> collection = new JsonQueryArray();
	    	Iterator<?> members = elem.getAsJsonArray().iterator();
	    	while (members.hasNext()){
	    		JsonElement member = (JsonElement) members.next();
				collection.add(get_value(member,typeOfT,context));
			}
	    	return new JsonQuery((JsonQueryArray)collection);
	  } else if(elem.isJsonNull()){
		  return new JsonQuery(null);
	  }
	  return null;
  }
}
