import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


public class JsonQueryBuilder {
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
}
