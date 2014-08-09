import java.io.IOException;
import org.adrianwalker.multilinestring.Multiline;



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
			JsonQuery json = JsonQuery.fromJson(msg);
			
			// or recreate JSON string from JSON object using class method
			out(json.toJson());
			
			// Whats my city? (str gets a string)
			out(json.get("address").str("city"));
			
			//You can also use val, but this returns object so you must cast to the type you want
			String city = (String) json.get("address").val("city");
			out(city);
			
			// Whats my phone # (i gets an integer)
			out(json.get("phoneNumbers").i(1));
			
			// Change my city
			json.get("address").set("city","san fran");
			
			// Print new address in json format
			out(json.get("address").toJson());
			
			// Update phone numbers
			JsonQuery phoneNumbers = json.get("phoneNumbers");
			phoneNumbers.add(0,5555555);
			phoneNumbers.remove(1);
			
			// Print phone numbers in json format
			out(phoneNumbers.toJson());
			
			// Add my hobbies
			json.jset("hobbies","[\"tennis\",\"hiking\",\"swimming\"]");
			
			// Print the whole thing again
			out(json.toJson());
			
			// Actually I don't like swimming
			json.get("hobbies").remove(2);
			out(json.get("hobbies").toJson());
			
			// Oh no, I lost my job
			json.remove("role");
			json.set("employed",false);
			
			// Print the whole thing again
			out(json.toJson());
			
			// Go deeper in the tree
			json.get("properties").jset("pets","{\"cat\":\"Mr Wiggles\",\"dog\":\"Happy\"}");
			out(json.get("properties").toJson());
			
			// You can also append to the JSON object like this
			// first remove pets
			json.get("properties").remove("pets");
			
			// create a pets JSON object
			JsonQuery pets = JsonQuery.fromJson(myPets);
			
			// add it
			json.get("properties").set("pets",pets);
			
			// print all
			out(json.toJson());
			
		}catch(Exception e){System.out.println(e);};
	}
}