package src;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;

import org.adrianwalker.multilinestring.Multiline;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.LazilyParsedNumber;

public class JsonOTest {
	
	 /**
	  	{
		  "empID": 100.2,
		  "name": "robert",
		  "permanent": false,
		  "address": {
		    "street": "Foolhill Blvd",
		    "city": "Pasadena",
		    "zipcode": 91011
		  },
		  "phoneNumbers": [
		    false,
		    9876543,
		    1,
		    2,
		    [{"home phone":7904004}]
		  ],
		  "role": "Java Developer",
		  "employed":true,
		  "cities": [
		    "Los Angeles",
		    "New York"
		  ],
		  "properties": {
		    "age": null,
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
	  "name" : { "first" : "Joe", "last" : "Sixpack" },
	  "gender" : "MALE",
	  "verified" : false,
	  "userImage" : "Rm9vYmFyIQ=="
    }
	
   */
  @Multiline private static String msg3;

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
        	
    	   System.out.print("hello");
        	// Test multiline string
        	System.out.println(msg);
        	
            // create JSON object 
            JsonQuery $ = JsonQuery.fromJson(msg);
            
            // or recreate JSON string from JSON object using class method
            out($.toJson());
            
            // Whats my city?
            out($._("address").str("city"));
            
            //You can also use get, but this returns object so you must cast to the type you want
            String city = (String) $._("address").val("city");
            
            out(city);
            
            // Whats my phone #
            out($._("phoneNumbers").i(1));
            
            // Change my city
            $._("address").set("city","san fran");
            
            // Print new address in json format
            out($._("address").toJson());
            
            // Update phone numbers
            JsonQuery phoneNumbers = $._("phoneNumbers");
            phoneNumbers.add(0,5555555);
            phoneNumbers.remove(1);
            
            // Print phone numbers in json format
            out(phoneNumbers.toJson());
            
            // Add my hobbies
            $.jset("hobbies","[\"tennis\",\"hiking\",\"swimming\"]");
            
            // Print the whole thing again
            out($.toJson());
            
            // Actually I don't like swimming
            $._("hobbies").remove(2);
            out($._("hobbies").toJson());
            
            out($.d("empID"));
            
            // Oh no, I lost my job
            $.remove("role");
            $.set("employed",false);
            
            out($.bool("employed"));
            
            // Print the whole thing again
            out($.toJson());
            
            // Go deeper in the tree
            $._("properties").jset("pets","{\"cat\":\"Mr Wiggles\",\"dog\":\"\"}");
            out( $._("properties")._("pets").str("dog"));
            out($._("properties").toJson());
            
            // You can also append to the JSON object like this
            
            // first remove pets
            $._("properties").remove("pets");
            
            
            // create a pets JSON object
            JsonQuery pets = JsonQuery.fromJson(myPets);
            
            // add it
            $._("properties").set("pets",pets);
            
           out( $._("properties")._("pets").str("cat"));
            
            // print all
            out($.toJson());
            
           // while($.get("phonenumbers").hasNext()){
            //	out($.get("phonenumbers").next().val() instanceof String);
          //  }
            out(
            	$.get("phoneNumbers[1]").type()
            );
            
            for(JsonQuery number: $.get("phoneNumbers").each()){
            	out(number.type() + " " + number.str());
            }
            
            out("starting object traversal");
            
            JsonQueryNode props = null;
            for(JsonQueryNode addr : $.each()){
            	if(addr.key.equals("properties")){
            		props=addr;
            	}
            }
            
            out(
            	props.get("pets.cat").val()
            );
            
            $ = JsonQuery.fromJson(msg2);
            // You can set a value like this
            $.get("data.translations[0].translatedText").set("Bonjour");
            
            // Print it out again. Str gets the value of the node as a string (regardless of type)
            out(
            	$.get("data.translations[0].translatedText").str()
            );
           
            
            // Sets the first position in the translations array to "Bonjour"
          
            // Adds a Json Object tothe first position in the translations array.
            $.get("data.translations").jadd(0,"{\"french\":\"Bonjour\",\"english\":\"hello\"}");
            
            out(
            	$.get("data.translations").toJson()
            );
            $ = JsonQuery.fromJson(msg3);
            if($.get("name.last").str().equalsIgnoreCase("xmler"))
            	$.get("name.last").set("Jsoner");
            
            out($.toJson());
            
            JsonParser parser = new JsonParser();
            JsonElement elem = parser.parse(msg3);
            out(elem.toString());
            
           
        }catch(Exception e){System.out.println(e);};
        
    }
}
