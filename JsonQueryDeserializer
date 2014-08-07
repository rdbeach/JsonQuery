
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


@SuppressWarnings("rawtypes")
public class JsonQueryDeserializer implements JsonDeserializer<JsonQuery> {

  @Override
  public JsonQuery<JsonQueryHashMap> deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
      throws JsonParseException {
	  
	   JsonQueryHashMap map = new JsonQueryHashMap();
	   final JsonObject jsonObject= json.getAsJsonObject();
	   final  Set<Map.Entry<String,JsonElement>> elemset = jsonObject.entrySet();
    
	   for (Map.Entry<String, JsonElement> elem : elemset){
		   // System.out.println(elem.getKey() + "/" + elem.getValue());
		    String key = elem.getKey();
		    JsonElement value = elem.getValue();
		    map.put(key,get_value(value,typeOfT,context));
	   }

	   return new JsonQuery<JsonQueryHashMap>(map);
  }
  
  public Object get_value(final JsonElement elem, final Type typeOfT, final JsonDeserializationContext context){
	  if(elem.isJsonObject()){
	    	JsonQuery<JsonQueryHashMap> innermap = deserialize(elem,typeOfT,context);
	    	return innermap;
	  }
	  else if(elem.isJsonPrimitive()){
	  		if(elem.getAsJsonPrimitive().isString()){
	  				return new JsonQuery<String>((String)elem.getAsJsonPrimitive().getAsString());
	  		}else if(elem.getAsJsonPrimitive().isNumber()){
	  			   return new JsonQuery<Number>((Number)elem.getAsJsonPrimitive().getAsNumber());
	  		}else if(elem.getAsJsonPrimitive().isBoolean()){
		    		return new JsonQuery<Boolean>((Boolean)elem.getAsJsonPrimitive().getAsBoolean());
	  		}
	  }
	  else if(elem.isJsonArray()){
		    Collection<Object> collection = new JsonQueryArrayList();
	    	Iterator<?> members = elem.getAsJsonArray().iterator();
	    	while (members.hasNext()){
	    		JsonElement member = (JsonElement) members.next();
				collection.add(get_value(member,typeOfT,context));
			}
	    	return new JsonQuery<JsonQueryArrayList>((JsonQueryArrayList)collection);
	  } else if(elem.isJsonNull()){
		  return new JsonQuery<Object>(null);
	  }
	  return null;
  }
}
