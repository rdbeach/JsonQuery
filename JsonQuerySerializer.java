import java.lang.reflect.Type;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;



public class JsonQuerySerializer implements JsonSerializer<JsonQuery> {

  public JsonElement serialize(final JsonQuery json, final Type typeOfT, final JsonSerializationContext context)
      throws JsonParseException {
	
	  return context.serialize(json.node);
  }

}
