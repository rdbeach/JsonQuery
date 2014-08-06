JsonO
=====

A Gson extension that makes the conversion between JSON and Java easier.


## Implementaion Notes

To use JsonO, you will first need to add the Gson library to your project. You may download it here:


https://code.google.com/p/google-gson/downloads/list


To run the test file, JsonOTest.java, you will also need to implement the benelog/multiline project:

https://github.com/benelog/multiline

Alternatively, you can edit the JsonOTest.java file so that it does not contain the multiline strings.


## Usage

#### Here is an example.

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
    		
    		
 String msg will contain the JSON string above. Now, convert the JSON string to a JsonO object. Out means System.out.println.
 
 
                // create JSON object 
                JsonO json = JsonO.fromJson(msg);
                    
                // or recreate JSON string from JSON object using class method
                out(json.toJson());
                
                
Output: 

{"empID":100,"address":{"zipcode":91011,"city":"Pasadena","street":"Foolhill Blvd"},"role":"Java              Developer","cities":["Los Angeles","New York"],"permanent":false,"name":"Robert","phoneNumbers":[1234567,9876543],
"properties":{"salary":"$6000","age":"28 years"},"employed":true}



#### Get some properties:
                    
                // Whats my city?
                out(json.o("address").s("city"));
                
                // Whats my phone #
                out(json.a("phoneNumbers").i(1));


Output:

Pasadena

9876543



#### Set some properties:
                    
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
                    
Output:

{"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"}

[5555555,9876543]



#### Use a JSON string to add to the JsonO object tree (with jset):
                    
                // Add my hobbies
                json.jset("hobbies","[\"tennis\",\"hiking\",\"swimming\"]");
                
                // Print the whole thing again
                out(json.toJson());
                    
Output:

{"empID":100,"address":{"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"},"role":"Java Developer","cities":["Los Angeles","New York"],"hobbies":["tennis","hiking","swimming"],"permanent":false,"name":"Robert","phoneNumbers":[5555555,9876543],"properties":{"salary":"$6000","age":"28 years"},"employed":true}



#### Removing stuff. Changing stuff. You get the idea now:
                    
                // Actually I don't like swimming
                json.a("hobbies").remove(2);
                out(json.a("hobbies").toJson());
                
                // Oh no, I lost my job
                json.remove("role");
                json.set("employed",false);
                
                // Print the whole thing again
                out(json.toJson());
                
                // Go deeper in the tree
                json.o("properties").jset("pets","{\"cat\":\"Mr Wiggles\",\"dog\":\"Happy\"}");
                
                out(json.o("properties").toJson());
                
                // You can also append to the JSON object like this
                
                // first remove pets
                json.o("properties").remove("pets");
                
                /** myPets:
	   		{
	   		"cat":"Mr Happy",
	   		"dog":"Wiggles"
	   		}
   		*/
                // create a pets JSON object
                JsonO pets = JsonO.fromJson(myPets);
                
                // add it
                json.o("properties").set("pets",pets);
                
                // print all
                out(json.toJson());
 
Output:

["tennis","hiking"]

{"empID":100,"address":{"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"},"cities":["Los Angeles","New York"],"hobbies":["tennis","hiking"],"permanent":false,"name":"Robert","phoneNumbers":[5555555,9876543],"properties":{"salary":"$6000","age":"28 years"},"employed":false}

{"pets":{"cat":"Mr Wiggles","dog":"Happy"},"salary":"$6000","age":"28 years"}

{"empID":100,"address":{"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"},"cities":["Los Angeles","New York"],"hobbies":["tennis","hiking"],"permanent":false,"name":"Robert","phoneNumbers":[5555555,9876543],"properties":{"pets":{"cat":"Mr Happy","dog":"Wiggles"},"salary":"$6000","age":"28 years"},"employed":false}



#### Another example:

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
                
                
Grab the "translatedText" value like this:

                JsonO json = JsonO.fromJson(msg);
                out(json.o("data").a{"translations").o(0).s("translatedText"));
                
Output:

Hello world



## Syntax

Brief description of the methods. Look at the java code for specifics.

                Static Methods:
                Json.fromJson: Converts a JSON string to a JsonO object

                Generic methods:
                
                o: gets an object
                a: gets an array
                s: gets a string
                i: gets an integer
                b: gets a boolean
                toJson: turns any part of the JsonO object tree into a JSON string 
                set: sets a value
                jset: set ad value from a JSON string
                
                Array Specific Methods:
                
                add: adds an value
                jadd: adds a value from a JSON string
                
		Inherited Methods: JsonO extends HashMap, and JsonA extends Arraylist.



## Additional Info

Not sure what happens to other number types besides int. You may want to wrap floats in a string in your json, or just fork the code to suit your needs.

The aim here is convenience and flexibiliy, and to give the Java manipulation a "Javascript like feel". I have not tested the performance.

It is thread safe, though, I am not sure if Gson is entirely thread safe. This is why the JsonO class contains the static member ensureThreadSaftey. If you are running a singe threaded application, you can set this to false and use a statically created Gson, as opposed to creating the object on request.

This code traverses the JsonElements tree and builds a corresponding tree of JsonO and JsonA elements (Essentially maps and arrays). You probably could acheive the same functionality just by wrapping the JsonElements tree directly. However, having the data encapsulated in your own data types may offer some advantages. Especially if you want to extend this further. For instance, I see no way in the Gson code to directly set an element of an JsonArray at a certain index, but by reconstruction the arrays with arraylists, this is quite easy.


                
