JsonO
=====

A Gson extension that makes the conversion between JSON and Java easier.


## Implementaion Notes

To use JsonO, you will first need to add the Gson library to your project. You may download it here:


https://code.google.com/p/google-gson/downloads/list


To run the test file, JsonOTest.java, you will also need to impement the benelog/multiline project:

https://github.com/benelog/multiline

Alternatively, you can edit the JsonOTest.java file so that it does not cantain the multiline strings.


## Usage

Here is an example.

Start with a JSON string:

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
    		
 String msg will contain the json above. Now, convert the JSON to a JsonO object. Out mean System.out.println.
 
                // create JSON object 
                JsonO json = JsonO.fromJson(msg);
                    
                // or recreate JSON string from JSON object using class method
                out(json.toJson());
                
                
                //Prints
                //{"empID":100,"address":{"zipcode":91011,"city":"Pasadena","street":"Foolhill Blvd"},"role":"Java                     Developer","cities":["Los Angeles","New York"],"permanent":false,
                "name":"Robert","phoneNumbers":[1234567,9876543],
                "properties":{"salary":"$6000","age":"28 years"},"employed":true}
                    
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
 
