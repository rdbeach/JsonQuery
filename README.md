JsonQuery
=====

A Gson extension that makes manipulating JSON in Java easier.


## Implementaion Notes

To use JsonQuery, you will first need to add the Gson library to your project. You may download it here:


https://code.google.com/p/google-gson/downloads/list


To run the test file, JsonQueryTest.java, you will also need to implement the benelog/multiline project:

https://github.com/benelog/multiline

Alternatively, you can edit the JsonQueryTest.java file so that it does not contain the multiline strings.


## Usage

Are two ways to manipulate the json tree in java.

	Single node traversal
	Javascript queries.

#### We will begin with single node traversal. Here is an example.

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
    		
    		
 String msg will contain the JSON string above. Now, convert the JSON string to a JsonQuery object. Out means System.out.println.
 
 
                // create JSON object 
                JsonQuery json = JsonQuery.fromJson(msg);
                    
                // or recreate JSON string from JSON object using class method
                out(json.toJson());
                
                
Output: 

{"empID":100,"address":{"zipcode":91011,"city":"Pasadena","street":"Foolhill Blvd"},"role":"Java              Developer","cities":["Los Angeles","New York"],"permanent":false,"name":"Robert","phoneNumbers":[1234567,9876543],
"properties":{"salary":"$6000","age":"28 years"},"employed":true}



#### Get some properties:
                    
                // Whats my city? (s gets a string)
                out(json._("address").s("city"));
                
                //You can also use get, but this returns object so you must cast to the type you want
                String city = (String) json._("address").get("city");
                
                out(city);
                
                // Whats my phone # (i gets an integer)
                out(json._("phoneNumbers").i(1));


Output:

Pasadena

9876543



#### Set some properties:
                    
                // Change my city
                json._("address").set("city","san fran");
                
                // Print new address in json format
                out(json._("address").toJson());
                
                // Update phone numbers
                JsonQuery phoneNumbers = json._("phoneNumbers");
                phoneNumbers.add(0,5555555);
                phoneNumbers.remove(1);
                
                // Print phone numbers in json format
                out(phoneNumbers.toJson());
                    
Output:

{"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"}

[5555555,9876543]



#### Use a JSON string to add to the JsonQuery object tree (with jset):
                    
                // Add my hobbies
                json.jset("hobbies","[\"tennis\",\"hiking\",\"swimming\"]");
                
                // Print the whole thing again
                out(json.toJson());
                    
Output:

{"empID":100,"address":{"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"},"role":"Java Developer","cities":["Los Angeles","New York"],"hobbies":["tennis","hiking","swimming"],"permanent":false,"name":"Robert","phoneNumbers":[5555555,9876543],"properties":{"salary":"$6000","age":"28 years"},"employed":true}



#### Removing stuff. Changing stuff. You get the idea now:
                    
                // Actually I don't like swimming
                json._("hobbies").remove(2);
                out(json._("hobbies").toJson());
                
                // Oh no, I lost my job
                json.remove("role");
                json.set("employed",false);
                
                // Print the whole thing again
                out(json.toJson());
                
                // Go deeper in the tree
                json._("properties").jset("pets","{\"cat\":\"Mr Wiggles\",\"dog\":\"Happy\"}");
                
                out(json._("properties").toJson());
                
                // You can also append to the JSON object like this
                
                // first remove pets
                json._("properties").remove("pets");
                
                /** myPets:
	   		{
	   		"cat":"Mr Happy",
	   		"dog":"Wiggles"
	   		}
   		*/
                // create a pets JSON object
                JsonQuery pets = JsonQuery.fromJson(myPets);
                
                // add it
                json._("properties").set("pets",pets);
                
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

                JsonQuery json = JsonQuery.fromJson(msg);
                out(json._("data")._("translations")._(0).s("translatedText"));
                
                
Output:

Hello world




#### Chaining, chaining, chaining.:

	json.set("name", "bob")
                .jset("test_scores","[]")._("test_scores")
                .add(57).add(92).add(76);
            
        json.jset("family", "{}")._("family")
            	.set("mother","Donna")
            	.set("father","Bill")
            	.set("sister", "Moonbeam")
            	.jset("pets","[]")._("pets")
            		.add("rover")
            		.add("killer")
            		.add(1,"fluffy");
            
        out(json._get("family.pets[0]"));
        
Output:

rover




####Javascript queries (The fun part)
                
        // Here are some examples of javascript queries (using the $ method)
        
        // This one does the same thing as the single node query above.
        // Value gets the value of the node as an object
        out(
        	json.$("data.translations[0].translatedText").val()
        );
        
        // You can set a value like this
        json.$("data.translations[0].translatedText").set("Bonjour");
         
        // Print it out again. Str gets the value of the node as a string (regardless of type)
        out(
        	json.$("data.translations[0].translatedText").str()
        );
        
        // Sets the first position in the translations array to "Bonjour"
        json.$("data.translations").add(0,"Bonjour");
        
        // Adds a Json Object tothe first position in the translations array.
        json.$("data.translations").jadd(0,"{\"french\":\"Bonjour\",\"english\":\"hello\"}")
        
        out(
        	json.$("data.translations").toJson()
        );

                
Output:

Hello world

Bonjour

[{"english":"hello","french":"Bonjour"},"Bonjour",{"translatedText":"Bonjour"}]






## Syntax

Brief description of the methods. Look at the java code for specifics.

                Static Methods:
                JsonQuery.fromJson: Converts a JSON string to a JsonQuery object tree

                Generic methods:
                
                _: gets a branch in the JsonQuery Object tree
                $: traverses the JsonQuery Object tree using javascript queries. (I am making this nomenclature up as I go.)
                
                get: gets a leaf (returns object)
                s: gets a string leaf
                i: gets an integer leaf
                d: gets a double leaf
                l: gets a long leaf
                b: gets a boolean leaf
                toJson: turns any part of the JsonQuery object tree into a JSON string 
                set: sets a value
                jset: set a value from a JSON string
                
                
                Array Specific Methods:
                
                add: adds a value
                jadd: adds a value from a JSON string
                
		Inherited Methods: JsonQueryHashMap extends HashMap, and JsonQueryArrayList extends ArrayList.



## Additional Info

The JSON string must start with braces {}.

TODO: Thinking about adding search functions, bulk add functions, and more advanced query traversal methods. Any ideas? Suggestions?

The aim here is convenience and flexibiliy, and to give the Java manipulation a "Javascript like feel". I have not tested the performance.

It is thread safe, though, I am not sure if Gson is entirely thread safe. This is why the JsonO class contains the static member ensureThreadSaftey. If you are running a singe threaded application, you can set this to false and use a statically created Gson, as opposed to creating the object on request.

This code traverses the JsonElements tree and builds a corresponding tree of JsonQuery nodes (Containing HashMaps, ArrayLists, and Gson primitive types).


                
