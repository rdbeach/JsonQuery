
import java.io.IOException;

import org.adrianwalker.multilinestring.Multiline;

public class JsonOTest {
	
	 /**
	  	{
		  "empID": 100,
		  "name": "Robert",
		  "permanent": false,
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
  			"translations": [
   				{
    				"translatedText": "Hello world"
   				}
  			]
 		}
	}
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
        	
            // create JSON object 
            JsonO json = JsonO.fromJson(msg);
            
            // or recreate JSON string from JSON object using class method
            out(json.toJson());
            
            // Whats my city?
            out(json.o("address").s("city"));
            
            // Whats my phone #
            out(json.a("phoneNumbers").i(1));
            
            // Change my city
            json.o("address").set("city","san fran");
            
            // Print new address in json format
            out(json.o("address").toJson());
            
            // Update phone numbers
            JsonA phoneNumbers = json.a("phoneNumbers");
            phoneNumbers.add(0,5555555);
            phoneNumbers.remove(1);
            
            // Print phone numbers in json format
            out(phoneNumbers.toJson());
            
            // Add my hobbies
            json.set("hobbies","[\"tennis\",\"hiking\",\"swimming\"]");
            
            // Print the whole thing again
            out(json.toJson());
            
            // Actually I don't like swimming
            json.a("hobbies").remove(2);
            out(json.a("hobbies").toJson());
            
            // Oh no, I lost my job
            json.remove("role");
            json.set("employed",false);
            
            // Print the whole thing again
            out(json.toJson());
            
            // Go deeper in the tree
            json.o("properties").set("pets","{\"cat\":\"Mr Wiggles\",\"dog\":\"Happy\"}");
            
            out(json.o("properties").toJson());
            
            // You can also append to the JSON object like this
            
            // first remove pets
            json.o("properties").remove("pets");
            
            
            // create a pets JSON object
            JsonO pets = JsonO.fromJson(myPets);
            
            // add it
            json.o("properties").set("pets",pets);
            
            // print all
            out(json.toJson());
           
        }catch(Exception e){System.out.println(e);};
        
    }
}
