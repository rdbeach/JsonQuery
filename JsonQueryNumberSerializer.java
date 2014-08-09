
import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;


public class JsonQueryNumberSerializer implements JsonSerializer<JsonQueryNumber> {

  public JsonElement serialize(final JsonQueryNumber number, final Type typeOfT, final JsonSerializationContext context)
	  throws JsonParseException {
		  return new JsonPrimitive(number);
  }

}
