package src;
import java.io.IOException;

import org.adrianwalker.multilinestring.Multiline;

/*
 * JsonQueryTest 
 * A test file for JsonQuery's single node traversal functions.
 */

public class JsonQueryTest {
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
		"phoneNumbers": [1234567,9876543],
		"role": "Java Developer",
		"employed":true,
		"cities": ["Los Angeles","New York"],
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
			
			JsonQuery $ = JsonQuery.fromJson(msg);
			
			// or recreate JSON string from JSON object using class method
			
			out($.toJson());
			
			// Whats my city? (str gets a string)
	        
			out($.str("address.city"));
	                
	        // Pasadena
	                
	        //You can also use "val", but this returns Object, so you must cast to the type you want
	        
	        String city = (String) $.val("address.city");
	                
	        out(city);
	                
	        // Pasadena
	                
	        // Whats my phone # (i gets an integer)
	        
	        out($.i("phoneNumbers.1"));
	                
	        // 9876543

			// Change my city like this: (If set cannot find the path to city, it will do nothing)
	        
	        $.set("address.city","san fran");
	         
	        // or like this: (If put cannot find the path to city, it will create it and set the value)
	        
	        $.put("address.city","san fran");
	        
	        // or like this: (Like put, node will find or create the path to city, the field will be updated with set)
	        
	        $.node("address.city").set("san fran");
			
	        // Print new address in json format
	        
	        out($.toJson("address"));
	                
	        // {"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"}
	                
	        // Update phone numbers
	        
	        JsonQuery phoneNumbers = $.get("phoneNumbers");
	        phoneNumbers.add(0,5555555);
	        phoneNumbers.remove(1);
	                
	        // Print phone numbers in json format
	        
	        out(phoneNumbers.toJson());
	                
	        // [5555555,9876543]
			
			// Add my hobbies
			
	        $.jput("hobbies","[\"tennis\",\"hiking\",\"swimming\"]");
			
			// Print the whole thing again
			
	        out($.toJson());
	        
	        // {"empID":100,"address":{"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"},"role":"Java Developer","cities":["Los Angeles","New York"],"hobbies":["tennis","hiking","swimming"],"permanent":false,"name":"Robert","phoneNumbers":[5555555,9876543],"properties":{"salary":"$6000","age":"28 years"},"employed":true}
			
	        // Actually I don't like swimming
	        
	        $.remove("hobbies.2");
	        out($.toJson("hobbies"));
	                
	        // ["tennis","hiking"]
	                
	        // Oh no, I lost my job
	        
	        $.remove("role");
	        $.set("employed",false);
	                
	        // Print the whole thing again
	        
	        out($.toJson());
	                
	        // {"empID":100,"address":{"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"},"cities":["Los Angeles","New York"],"hobbies":["tennis","hiking"],"permanent":false,"name":"Robert","phoneNumbers":[5555555,9876543],"properties":{"salary":"$6000","age":"28 years"},"employed":false}
		
			
			// Add more to the JSON object tree
	        $.jput("properties.pets","{\"cat\":\"Mr Wiggles\",\"dog\":\"Happy\"}");
	                
	        out($.toJson("properties"));
	                
	        // {"pets":{"cat":"Mr Wiggles","dog":"Happy"},"salary":"$6000","age":"28 years"}
	                
	        // You can also append to the JSON object like this
	                
	        // first remove pets
	        
	        $.remove("properties.pets");
	                
	        // create a pets JSON object
	        
	        JsonQuery pets = JsonQuery.fromJson(myPets);
	                
	        // add it
	        
	        $.put("properties.pets",pets);
	                
	        // print all
	        
	        out($.toJson());
	                
	        // {"empID":100,"address":{"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"},"cities":["Los Angeles","New York"],"hobbies":["tennis","hiking"],"permanent":false,"name":"Robert","phoneNumbers":[5555555,9876543],"properties":{"pets":{"cat":"Mr Happy","dog":"Wiggles"},"salary":"$6000","age":"28 years"},"employed":false}
			
	        
	        
			// Test msg2
			
			$ = JsonQuery.fromJson(msg2);
            out(
            	$.val("data.translations.0.translatedText")
            );
            
            // Hello world
            
            // Adds a translation to the translations array
            
        	$.add("data.translations","Bonjour:hello");
                
            // Adds a Json Object to the second position in the translations array.
                
	        $.jadd("data.translations",1,"{\"french\":\"Bonjour\",\"english\":\"hello\"}");
	        
	        out(
	        	$.get("data.translations").toJson()
	        );
            
            
            
            // Building JSON with single nodes:
	        
	        JsonQuery json = $.arr();
	        json.
	        	add("a").
	        	add($.obj().
	        		put("b", "c").
	        		put("d",$.arr().
	        			add(1000).
	        			add(true)
    				)
	        	).add("g");
	        
	        out(json.toJson());
            
            $ = JsonQuery.fromJson("{}");
            
            $.node("notification").
			put("message","test").
			put("sound","sounds/ararmsound.wav").
			node("target").
				node("apps").
					node("0").
						put("id","app_id").
						node("platforms").
							add("ios");
            $.put("access_token", "access_token");
            
            
            out($.toJson());
            
            // {"access_token":"access_token","notification":{"sound":"sounds/ararmsound.wav","message":"test","target":{"apps":[{"id":"app_id","platforms":["ios"]}]}}}
			
		}catch(Exception e){System.out.println(e);};
	}
}