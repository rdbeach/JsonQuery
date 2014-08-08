
import java.lang.reflect.Type;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


public class JsonQueryNumberSerializer implements JsonSerializer<JsonQueryNumber> {

  public JsonElement serialize(final JsonQueryNumber number, final Type typeOfT, final JsonSerializationContext context)
	  throws JsonParseException {
	  if(Math.floor(number.doubleValue())==number.doubleValue()){
		  return context.serialize(number.longValue());
	  }else{
		  return context.serialize(number.doubleValue());
	  }
  }

}
