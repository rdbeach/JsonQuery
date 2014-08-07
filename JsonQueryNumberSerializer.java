
import java.lang.reflect.Type;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.internal.LazilyParsedNumber;


public class JsonQueryNumberSerializer implements JsonSerializer<LazilyParsedNumber> {

  public JsonElement serialize(final LazilyParsedNumber number, final Type typeOfT, final JsonSerializationContext context)
      throws JsonParseException {
	  if(Math.floor(number.doubleValue())==number.doubleValue()){
		  return context.serialize(number.longValue());
	  }else{
		  return context.serialize(number.doubleValue());
	  }
  }

}
