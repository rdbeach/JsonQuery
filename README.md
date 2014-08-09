JsonQuery
=====

A Gson extension that makes manipulating JSON in Java easier.

## A quick look

***NOTE*** This project is for testing only. It is very much a work in progress.

JsonQuery gives you Java tools for consuming, traversing, querying, editing, and producing JSON.

For instance, a deeply nested JSON field can be extracted in one brief line:

**$.get("planets.earth.north_america.california.los_angeles.zip_codes.my_zip_code").val();**

This project does not address binding JSON to Java classes. Instead, the JSON information is encapsulated in a dynamic tree structure. From there, you can do with it whatever you want- write it to a class, save it to a database, or send a JSON response back to your client app after processing a request.


## Implementation Notes

To use JsonQuery, you will first need to add the Gson library to your project. You may download it here:


https://code.google.com/p/google-gson/downloads/list


To run the test file, JsonQueryTest.java, you will also need to implement the benelog/multiline project:

https://github.com/benelog/multiline

Alternatively, you can edit the JsonQueryTest.java file so that it does not contain the multiline strings.


## Usage

There are two ways to manipulate the json tree using JsonQuery.

	- Single node traversal
	- Javascript queries.
	
	

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
 
                // create JsonQuery object 
                JsonQuery $ = JsonQuery.fromJson(msg);
                    
                // or recreate JSON string from JsonQuery object using class method
                out($.toJson());
                
                // {"empID":100,"address":{"zipcode":91011,"city":"Pasadena","street":"Foolhill Blvd"},"role":"Java Developer","cities":["Los Angeles","New York"],"permanent":false,"name":"Robert","phoneNumbers":[1234567,9876543],"properties":{"salary":"$6000","age":"28 years"},"employed":true}
                
                




#### Retrieve some properties from the newly created object:
                    
                // Whats my city? (str gets a string)
                out($.get("address").str("city"));
                
                // Pasadena
                
                //You can also use "val", but this returns Object, so you must cast to the type you want
                String city = (String) $.get("address").val("city");
                
                out(city);
                
                // Pasadena
                
                // Whats my phone # (i gets an integer)
                out($.get("phoneNumbers").i(1));
                
                // 9876543





#### Set some properties:
                    
                // Change my city
                $.get("address").set("city","san fran");
                
                // Print new address in json format
                out($.get("address").toJson());
                
                // {"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"}
                
                // Update phone numbers
                JsonQuery phoneNumbers = $.get("phoneNumbers");
                phoneNumbers.add(0,5555555);
                phoneNumbers.remove(1);
                
                // Print phone numbers in json format
                out(phoneNumbers.toJson());
                
                // [5555555,9876543]
                    




#### Use a JSON string to add to the JsonQuery object tree (with jset):
                    
                // Add my hobbies
                $.jset("hobbies","[\"tennis\",\"hiking\",\"swimming\"]");
                
                // Print the whole thing again
                out($.toJson());
                
                // {"empID":100,"address":{"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"},"role":"Java Developer","cities":["Los Angeles","New York"],"hobbies":["tennis","hiking","swimming"],"permanent":false,"name":"Robert","phoneNumbers":[5555555,9876543],"properties":{"salary":"$6000","age":"28 years"},"employed":true}
                    




#### Removing stuff. Changing stuff. You get the idea now:
                    
                // Actually I don't like swimming
                $.get("hobbies").remove(2);
                out($.get("hobbies").toJson());
                
                // ["tennis","hiking"]
                
                // Oh no, I lost my job
                $.remove("role");
                $.set("employed",false);
                
                // Print the whole thing again
                out($.toJson());
                
                // {"empID":100,"address":{"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"},"cities":["Los Angeles","New York"],"hobbies":["tennis","hiking"],"permanent":false,"name":"Robert","phoneNumbers":[5555555,9876543],"properties":{"salary":"$6000","age":"28 years"},"employed":false}
                
                // Go deeper in the tree
                $.get("properties").jset("pets","{\"cat\":\"Mr Wiggles\",\"dog\":\"Happy\"}");
                
                out($.get("properties").toJson());
                
                // {"pets":{"cat":"Mr Wiggles","dog":"Happy"},"salary":"$6000","age":"28 years"}
                
                // You can also append to the JSON object like this
                
                // first remove pets
                $.get("properties").remove("pets");
                
		/** myPets:
	   		{
	   		"cat":"Mr Happy",
	   		"dog":"Wiggles"
	   		}
		*/
                // create a pets JSON object
                JsonQuery pets = JsonQuery.fromJson(myPets);
                
                // add it
                $.get("properties").set("pets",pets);
                
                // print all
                out($.toJson());
                
                // {"empID":100,"address":{"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"},"cities":["Los Angeles","New York"],"hobbies":["tennis","hiking"],"permanent":false,"name":"Robert","phoneNumbers":[5555555,9876543],"properties":{"pets":{"cat":"Mr Happy","dog":"Wiggles"},"salary":"$6000","age":"28 years"},"employed":false}
 




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

                JsonQuery j$ = JsonQuery.fromJson(msg);
                out($.get("data").get("translations").get(0).str("translatedText"));
                
                // Hello world
                
                



#### Chaining, chaining, chaining.:

	$.set("name", "bob")
                .jset("test_scores","[]")._("test_scores")
                .add(57).add(92).add(76);
            
        $.jset("family", "{}")._("family")
            	.set("mother","Donna")
            	.set("father","Bill")
            	.set("sister", "Moonbeam")
            	.jset("pets","[]")._("pets")
            		.add("rover")
            		.add("killer")
            		.add(1,"fluffy");
            
        out(
        	$.get("family").get("pets").str(0)
        );
        
        // rover
        




####Javascript queries (The fun part)

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
	
                
        // Here are some examples of javascript queries (using the get method)
        
        
        // This one does the same thing as the single node query above.
        // "val" gets the value of the node as an object
        
        out(
        	$.get("data.translations[0].translatedText").val()
        );
        
        // Hello World
        
        
        
        // You can set a value like this
        
        $.get("data.translations[0].translatedText").set("Bonjour");
        
        		
         
        // Str gets the value of the node as a string (regardless of type)
        
        out(
        	$.get("data.translations[0].translatedText").str()
        );
        
        // Bonjour
        		
        		
        
        // Sets the first position in the translations array to "Bonjour"
        
        $.get("data.translations").add(0,"Bonjour");
        
        
        
        // Adds a Json Object to the first position in the translations array.
        
        $.get("data.translations").jadd(0,"{\"french\":\"Bonjour\",\"english\":\"hello\"}");
        
        out(
        	$.get("data.translations").toJson()
        );
        
       // [{"english":"hello","french":"Bonjour"},"Bonjour",{"translatedText":"Bonjour"}]
       
       // Instead of "get", you can use "node". If get cannot find a node in the tree, it simply returns without
       // doing anything. Node, on the other hand, will first attempt to find the node, and, if it can't find 
       // it, it will then attempt to create it:
       
       $.node("data.NEW_NODE").set("this is a new node");  //creates a new node
       
       // One rule that has been put in place is disallowing arbitrary type conversions. In other words, if a node
       // has its type set to Array, it can only be converted to another type, such as object, by calling 
       // set(Object value) on that node, or by first calling toNull() on the node, and then changing it.
       
       // The node operator will not attempt type conversion, so that if it encounters a type inconsistency, 
       // such as applying an array method to an object node, it will fail and return null. So this won't work:
       
       
	$.node("data.translations.NEW_NODE").set("this is a new node");  //fails and returns null
	
	// This fails because translations is an array, and it is being referred to as an object in the javascript
	// query
	
	// This will work:
	
	$.node("data.translations[0].NEW_NODE").set("this is a new node");
	
	//So will this
	
	
	$.node("data.translations[10].NEW_NODE").set("this is a new node");
	
	
	// In this case, if the index is higher than the size of the array, null values will be inserted in the
	// array up to the index of the inserted value.
	
                






####Tree traversal 

	// Referring again to the first example...
	
	
	// use the "each" funtion to iterate over both array elements and object members
	
	// "phoneNumbers" is an array. We will iterate over it, printing both the node type, and it's value

	for(JsonQuery number: $.get("phoneNumbers").each()){
            	out(
            		number.type() + " " + number.str()
            	);
        }
        
	// number 1234567
	// number 9876543

        
            
	// Now let's iterate over the members of the json object. When you do this, each member will have a key property that you can examine to determine the member's name. You can call the type method for each member also. (See the API section below for possible types). This is not a recursive iteration. It only iterates over the direct members. If you want to iterate over the whole tree, you can write your own recursive function.
	
	// In particular, we will look for the "properties" member, and save it to our props variable.
	
	JsonQuery props = null;
	for(JsonQuery member : $.each()){
	    if(member.key.equals("properties")){
	    	props=member;
	    }
	}
	
	// Now that we have "properties", we will output the age property as a string.
	
	out(
	    props.$("age").str()
	);
	
	// 28 years




## API

Brief description of the methods. Will make this more detailed later, as this code is still in development. Look at the java code for specifics.

                Static Methods:
                JsonQuery.fromJson: Converts a JSON string to a JsonQuery object tree
                
                
                JsonQuery Class Members
                
                toJson: turns any part of the JsonQuery object tree into a JSON string 



                Tree traversal:
                
                get: finds a node JsonQuery Object tree. 
                node: finds or creates a node in the JsonQuery Object tree using javascript queries.
                 _: gets the next node in the JsonQuery Object tree (mostly used internally)
                
                
                
                Getting values:
                
               
                val: gets a node (returns object) (used with javascript queries).
                str: gets a node in string format, regardless of type.
           	b: gets a boolean node (type sensitive)
                i: gets an integer node (type sensitive)
                d: gets a double node (type sensitive)
                l: gets a long node (type sensitive)
                
                
                
                Settng Values:
                
                set: sets a value
                jset: set a value from a JSON string
                
                
                
                Array Specific Setting Methods:
                
                add: adds a value
                jadd: adds a value from a JSON string
                
                
                
                Iteration:
                
                each: gets each element of an array or each member of an object
                
                
                
                Removing:
                
                remove(string): Removing an object member
                remove(int): Removing an array member
                toNull() seting a node to null
                
                
                
                Type determination:
                
                type : gets the type of the current node as a string.
                	Possible types are:
                	object
                	array
                	string
                	number
                	boolean
                	null
                	
                isLeaf: returns true if the node is a leaf (endpoint)
                isObject: returns true if the node is an object
                isArray: returns true if the node is an array
                
                
                
		Inherited Methods: JsonQueryHashMap extends HashMap, and JsonQueryArrayList extends ArrayList.



## Additional Info

The JSON string must start with braces {}.

TODO: Need to improve the parsing of the json queries. It is not ready to handle arbitrary queries. Thinking about adding search functions, bulk add functions, and more advanced query traversal methods. Any ideas? Suggestions?

The aim here is convenience and flexibiliy, and to give the Java manipulation a "Javascript like feel". I have not tested the performance.

It is thread safe, though, I am not sure if Gson is entirely thread safe. This is why the JsonQuery class contains the static member ensureThreadSaftey. If you are running a singe threaded application, you can set this to false and use a statically created Gson, as opposed to creating the object on request.

This code traverses the JsonElements tree and builds a corresponding tree of JsonQuery nodes (Containing HashMaps, ArrayLists, and Gson primitive types).


                
