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
			
			//You can also use val, but this returns object so you must cast to the type you want
			String city = (String) $.val("address.city");
			out(city);
			
			// Whats my phone # (i gets an integer)
			
			out($.i("phoneNumbers.1"));
			
			// Update phone numbers
			JsonQuery phoneNumbers = $.get("phoneNumbers");
			phoneNumbers.add(0,5555555);
			phoneNumbers.remove(1);
			
			// Print phone numbers in json format
			out(phoneNumbers.toJson());
			
			// Add my hobbies
			$.jput("hobbies","[\"tennis\",\"hiking\",\"swimming\"]");
			
			// Print the whole thing again
			out($.toJson());
			
			// Actually I don't like swimming
			$.remove("hobbies.2");
			out($.toJson("hobbies"));
			
			// Oh no, I lost my job
			$.remove("role");
			$.put("employed",false);
			
			// Print the whole thing again
			out($.toJson());
			
			// Go deeper in the tree
			$.get("properties").jput("pets","{\"cat\":\"Mr Wiggles\",\"dog\":\"Happy\"}");
			out($.toJson("properties"));
			
			// You can also append to the JSON object like this
			// first remove pets
			$.remove("properties.pets");
			
			// create a pets JSON object
			JsonQuery pets = JsonQuery.fromJson(myPets);
			
			// add it
			$.put("properties.pets",pets);
			
			// Add an array element
			
			$.node("properites.kids").
				add("chucky").
				add("sissy").
				add("missy");
			
			// print all
			out($.toJson());
			
			// Test msg2
			
			$ = JsonQuery.fromJson(msg2);
            out(
            	$.val("data.translations.0.translatedText")
            );
            
            // Building JSON with single nodes:
            
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
			
		}catch(Exception e){System.out.println(e);};
	}
}