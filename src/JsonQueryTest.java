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
			out($.get("address.city").str());
			
			//You can also use val, but this returns object so you must cast to the type you want
			String city = (String) $.get("address").val("city");
			out(city);
			
			// Whats my phone # (i gets an integer)
			out($.get("phoneNumbers.1").i());
			
			// Change my city
			$.get("address").set("city","san fran");
			
			// Print new address in json format
			out($.get("address").toJson());
			
			// Update phone numbers
			JsonQuery phoneNumbers = $.get("phoneNumbers");
			phoneNumbers.add(0,5555555);
			phoneNumbers.remove(1);
			
			// Print phone numbers in json format
			out(phoneNumbers.toJson());
			
			// Add my hobbies
			$.jset("hobbies","[\"tennis\",\"hiking\",\"swimming\"]");
			
			// Print the whole thing again
			out($.toJson());
			
			// Actually I don't like swimming
			$.get("hobbies").remove(2);
			out($.get("hobbies").toJson());
			
			// Oh no, I lost my job
			$.remove("role");
			$.set("employed",false);
			
			// Print the whole thing again
			out($.toJson());
			
			// Go deeper in the tree
			$.get("properties").jset("pets","{\"cat\":\"Mr Wiggles\",\"dog\":\"Happy\"}");
			out($.get("properties").toJson());
			
			// You can also append to the JSON object like this
			// first remove pets
			$.get("properties").remove("pets");
			
			// create a pets JSON object
			JsonQuery pets = JsonQuery.fromJson(myPets);
			
			// add it
			$.get("properties").set("pets",pets);
			
			// print all
			out($.toJson());
			
			// Test msg2
			
			$ = JsonQuery.fromJson(msg2);
            out(
            	$.get("data.translations.0.translatedText").val()
            );
			
		}catch(Exception e){System.out.println(e);};
	}
}