
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import org.adrianwalker.multilinestring.Multiline;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JsonQueryTest {
	
	 /**
	  	{
		  "empID": 100,
		  "name": "Robert",
		  "permanent": true,
		  "address": {
		    "street": "Foolhill Blvd",
		    "city": "Pasadena",
		    "zipcode": 91011
		  },
		  "phoneNumbers": [
		    1234567,
		    9876543
		  ],
		  "role": "Java Developer",
		  "employed":true,
		  "cities": [
		    "Los Angeles",
		    "New York"
		  ],
		  "properties": {
		    "age": "28 years",
		    "salary": "$6000"
		  }
		}
	*/
   @Multiline private static String msg;
   
   /**
 	{
 		"data": {
 			"something":"",
  			"translations": [
   				{
    				"translatedText": "Hello world"
   				}
  			]
 		}}
	
    */
   @Multiline private static String msg2;

   /**
	   {
	   	"cat":"Mr Happy",
	   	"dog":"Wiggles"
	   }
   */
   @Multiline private static String myPets;
   
    private static void out(Object o){
    	System.out.println(o);
    }
	
    public static void main(String[] args) throws IOException {
        try{
        	
        	// Test multiline string
        	System.out.println(msg);
        	
        	
        	
        	
        	//JsonHashMap json2 = JsonHashMap.fromJson(msg2);
            //out(json2.a(0).o("data").a{"translations").o(0).s("translatedText"));
        	
            // create JSON object 
            JsonQuery json2 = JsonQuery.fromJson("{}");
            
            JsonQuery profile = JsonQuery.fromJson(msg);

            //out( json2._("json").get("idid")==null);
            
            json2.set("name", "bob")
                .jset("test_scores","[]")._("test_scores")
                .add(57).add(92).add(76);
            
            json2.jset("family", "{}")._("family")
            			.set("mother","Donna")
            			.set("father","Bill")
            			.set("sister", "Moonbeam")
            			.jset("pets","[]")._("pets")
            				.add("rover")
            				.add("killer")
            				.add(1,"fluffy");
            
           // out(json2._get("family.pets[0]"));
            out(json2._("family")._("pets").get(0));
            out(json2.toJson());
            // or recreate JSON string from JSON object using class method
           // out(json.toJson());
        	
        	JsonQuery json = JsonQuery.fromJson(msg2);
            out(json.get("a"));
            
            // Whats my city?
        	out(json._("data")._("translations").toJson());
        	out(json._("data")._("translations")._(0).s("translatedText"));
        	out(json._("data")._("translations")._(0).s("translatedText"));
        	//out(json._get("data.translations[0].translatedText"));
        	
        	out(json.$("data.translations[0].translatedText").get());
        	
        	
        	json.$("data.translations[0].translatedText").set(false);
        	out(json.$("data.translations[0].translatedText").get());
        	
        	out(json.$("data.translations").add(profile));
        	
        	out(json._("data")._("translations")._(0).b("translatedText"));
        	
        	
        	ArrayList arr = (ArrayList)json.$("data.translations").get();
        	
        	Iterator it = arr.iterator();
        	int i = 0;
            while(it.hasNext()){
            	it.next();
            	out(json.$("data.translations").get(i));
            	i++;
            }
            //out(json.$("data.translations").jadd(0,"{\"french\":\"Bonjour\",\"english\":\"hello\"}"));
            out(json.$("data.translations").toJson());
        	out(json.toJson());
        }catch(Exception e){System.out.println(e);};
        
    }
}
