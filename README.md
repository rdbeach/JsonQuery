JsonQuery
=====

A Java based structured query language for JSON.

## A quick look

***NOTE*** None of the code in this package is guaranteed to be stable or bug free. The SQL engine, in particular, is still in the experimental stages.

JsonQuery gives you Java tools for consuming, traversing, querying, editing, and producing JSON. It uses the Gson library for serialization and deserialization.

It consists of two parts:

	1) Single node operations.
	
	2) JSQL queries (JSON Strucured Query Language)
	

The single node operators allow you to maniputate the JSON tree one node at a time. For instance, a deeply nested JSON field 

	"company":{
		"sales":{
			"international":{
				"reps":[
					{
						"name":"bob",
						"start date":5-17-06",
						"monthly commission":5000
					},
					{
						"name":"stu",
						"start date":12-10-06",
						"monthly commission":10000
					},
					{
						"name":"bill",
						"start date":1-23-07",
						"monthly commission":4000
					}
				]
			}
		}
	}
	
can be extracted as follows:

	$.val("company.sales.international.reps.0.name");

	// bob

or updated like this:

	$.set("company.sales.international.reps.0.monthly commission",6000);



As you can see, you create a "path" string to target a particular node.



The JSQL Engine allows you to run SQL style queries on the JSON structure, like this:

	$.jsql("Select name from company:reps.? where monthly commision>5000)
	
	// The result set would return stu

The query returns a result set, which is a set of matching nodes.

This project does not address binding JSON to Java classes. Instead, the JSON information is encapsulated in a dynamic tree structure. From there, you can do with it whatever you want- write it to a class, save it to a database, or send a JSON response back to your client app after processing a request.


## Implementation Notes

To use JsonQuery, you will first need to add the Gson library to your project. You may download it here:


https://code.google.com/p/google-gson/downloads/list


To run the test file, JsonQueryTest.java, you will also need to implement the benelog/multiline project:

https://github.com/benelog/multiline

Alternatively, you can edit the JsonQueryTest.java file so that it does not contain the multiline strings.


## Usage

### Part I: Single Node Operations

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
    		
    		
The variable msg will contain the JSON string above. 
 
 
 
#### Convert the JSON string to a JsonQuery object. 
 
        // create JsonQuery object 
                
        JsonQuery $ = JsonQuery.fromJson(msg);
                
            
        // or recreate the JSON string from the JsonQuery object
                
        out($.toJson()); // out means System.out.println.
                
        // {"empID":100,"address":{"zipcode":91011,"city":"Pasadena","street":"Foolhill Blvd"},"role":"Java Developer","cities":["Los Angeles","New York"],"permanent":false,"name":"Robert","phoneNumbers":[1234567,9876543],"properties":{"salary":"$6000","age":"28 years"},"employed":true}
                
                




#### Retrieve some properties from the newly created object:
                    
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





#### Set some properties:
                    
        // Change my city like this: (If set cannot find the path to city, it will do nothing)
        
        $.set("address.city","san fran");
        
        // or like this: (Like set, if get cannot find the path, it simply returns)
        
        $.get("address.city").set("san fran");
         
        // or like this: (If put cannot find the path to city, it will create it and set the value)
        
        $.put("address.city","san fran");
        
        // or like this: (Like put, node will find or create the path to city, the field will be updated with set)
        
        $.node("address.city").set("san fran");
        
        // Print new address in json format
        
        out($.toJson("address"));
                
        // {"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"}
        
        
        // If you wish to add a bunch of things to address, you probably should "get" the address key first, and then make your additions:
        
        
        $.get("address").put("city","las vegas").put("city2","new york").put("city3","chicago")...
        
                
        
        
                
        // Update phone numbers: Use "add" to add to an array, or "set" to change existing values.
        
        JsonQuery phoneNumbers = $.get("phoneNumbers");
        phoneNumbers.add(0,5555555);
        phoneNumbers.remove(1);
                
        // Print phone numbers in json format
        
        out(phoneNumbers.toJson());
                
        // [5555555,9876543]
                    




#### Use a JSON string to add to the JsonQuery object tree (with jput):

	// jput, jadd, and jset function like their equivalents, put, add, and set, except that they take a JSON
	
	// string as the "value" argument.
                    
        // Add my hobbies
        
        $.jput("hobbies","[\"tennis\",\"hiking\",\"swimming\"]");
                
        // Print the whole thing again
        
        out($.toJson());
                
        // {"empID":100,"address":{"zipcode":91011,"city":"san fran","street":"Foolhill Blvd"},"role":"Java Developer","cities":["Los Angeles","New York"],"hobbies":["tennis","hiking","swimming"],"permanent":false,"name":"Robert","phoneNumbers":[5555555,9876543],"properties":{"salary":"$6000","age":"28 years"},"employed":true}
                    




#### More manipualtion: Removing stuff. Changing stuff. etc.
                    
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
        $.jset("properties.pets","{\"cat\":\"Mr Wiggles\",\"dog\":\"Happy\"}");
                
        out($.toJson("properties"));
                
        // {"pets":{"cat":"Mr Wiggles","dog":"Happy"},"salary":"$6000","age":"28 years"}
                
        // You can also append to the JSON object like this
                
        // first remove pets
        
        $.remove("properties.pets");
                
	/** myPets:
	   	{
	   	"cat":"Mr Happy",
	   	"dog":"Wiggles"
	   	}
	*/
                
        // create a pets JSON object
        
        JsonQuery pets = JsonQuery.fromJson(myPets);
                
        // add it
        
        $.put("properties.pets",pets);
                
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

                JsonQuery $ = JsonQuery.fromJson(msg);
                
                out(
                	$.val("data.translations.0.translatedText")
                );
                
                // Hello world
                
                
                // You can set a value like this
        
		 $.set("data.translations.0.translatedText","Bonjour");
        
        		
        	// Str gets the value of the node as a string (regardless of type)
        
        	out(
        		$.str("data.translations.0.translatedText")
        	);
        
        	// Bonjour
        		
        		
        
        	// Adds a translation to the translations array
            
        	$.add("data.translations","Bonjour:hello");
                
                // Adds a Json Object to the second position in the translations array.
                
	        $.jadd("data.translations",1,"{\"french\":\"Bonjour\",\"english\":\"hello\"}");
	        
	        out(
	        	$.get("data.translations").toJson()
	        );
	        
	       // [{"translatedText":"Bonjour"},{"english":"hello","french":"Bonjour"},"Bonjour:hello"]
	       
	       // You can use "get" to find a particular node. If get cannot find a node in the tree, 
	       // it simply returns without doing anything. 
	       
	       $.get("data.translations);
	       
	       // gets the data translations array
	       
	       // and you can do this:
	       
	       $.get("data.translations).add("another translation");
	       
	       
	       // "node", on the other hand, will first attempt to find the node, and, if it can't find 
	       // it, it will then attempt to create it:
	       
	       $.node("data.NEW_NODE").set("this is a new node");  //creates a new node
	       
	       // One rule that has been put in place is disallowing arbitrary conversions between Objects and Arrays.
	       
	       // For instance, if you have an object node, you cannot call add or jadd on that node.
	       
	       // Likewise, if you have an array node, you cannot call put or jput on that node.
	       
	       // If you want to reset a node to a different type, call "set"or "jset" on that node,
	       
	       // or first call clear() on the node, and then change it.
	       
	       // The node operator will not attempt type conversion either, so that if it encounters a type
	       
	       // inconsistency, such as applying an array method to an object node, it will fail and return null. 
	       
	       // This won't work:
	       
		$.node("data.translations.NEW_NODE").set("value");  //fails and returns null
		
		// This fails because translations is an array, and it is being referred to as an object in the javascript
		// path
		
		// This will work:
		
		$.node("data.translations.0.NEW_NODE").set("this is a new node");
		
		
		//So will this
		
		
		$.node("data.translations.10.NEW_NODE").set("this is a new node");
		
		
		// In this case, if the index is higher than the size of the array, null values will be inserted in the
		
		// array up to the index of the inserted value.



#### Chaining, chaining, chaining.:

	// Building JSON with single nodes:
	
	// .....with obj and arr functions
	
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
	
	// ["a",{"b":"c","d":[1000,true]},"g"]
	
	
	
	// .....with the node function
        
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
		
		
		
		
	// .....with jput
	
	$ = JsonQuery.fromJson("{}");

	$.put("name", "bob").
        jput("test_scores","[]").get("test_scores").
               	add(57).
                add(92).
                add(76);
            
        $.jput("family", "{}").get("family")
            	.put("mother","Donna")
            	.put("father","Bill")
            	.put("sister", "Moonbeam")
            	.jput("pets","[]").get("pets")
            		.add("rover")
            		.add("killer")
            		.add(1,"fluffy");
            		
        out($.toJson());
        
        // {"name":"bob","test_scores":[57,92,76],"family":{"pets":["rover","fluffy","killer"],"mother":"Donna","father":"Bill","sister":"Moonbeam"}}

            
        out(
        	$.str("family.pets.0")
        );
        
        // rover
        


####Tree traversal with "each"

	// Referring again to the first example...
	
	
	// use the "each" funtion to iterate over both array elements and object members
	
	// "phoneNumbers" is an array. We will iterate over it, printing both the node type, and it's value

	for(JsonQuery number: $.each("phoneNumbers")){
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


### Part II: JSQL Queries 

Comming soon.

## API

Brief description of the methods. Will make this more detailed later, as this code is still in development. Look at the java code for specifics.

        Static Methods:
        JsonQuery.fromJson: Converts a JSON string to a JsonQuery object tree
        
        
        JsonQuery Class Members
        
        toJson: turns any part of the JsonQuery object tree into a JSON string 



        Tree traversal:
        
        get: finds a node JsonQuery Object tree. 
        node: finds or creates a node in the JsonQuery Object tree.
         _: gets the next node in the JsonQuery Object tree (mostly used internally)
         
         
        JSQL queryies
        
        jsql: returns a JSQLResultSet<JsonQuery>, which as a list of matching JsonQuery nodes.
        
        
        
        Getting values:
        
       
        val: gets a node (returns object)
        str: gets a node in string format, regardless of type.
	bool: gets a boolean node (type sensitive)
        i: gets an integer node (type sensitive)
        d: gets a double node (type sensitive)
        l: gets a long node (type sensitive)
        getKey: gets the key for current node
        
        
        
        Updating Nodes: (That already exist)
        
        set: sets a value
        jset: set a value from a JSON string
        
        
        Updating/Adding Nodes: (That may or may not exist)
        
        put: sets a value
        jput: set a value from a JSON string
        
        
        Array Specific Setting Methods:
        
        add: adds a value
        jadd: adds a value from a JSON string
        
        
        
        Creating Nodes:
        
        obj: creates an empty object node
        arr: creates an empty array node
        
        
        
        Iteration:
        
        each: gets each element of an array or each member of an object
        
        
        
        Removing:
        
        remove(string): Removing an object member
        remove(int): Removing an array member
        
        
        
        Clearing
        
        clear() clears a node
        
        
        
        Testing Existence
        
        exists: true or false depending on the nodes existence.
        
        
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
        
        
        
	Inherited Methods: JsonQueryObject extends HashMap, and JsonQueryArray extends ArrayList.

## Syntax

Path keys must escape the backtick (\`) character as follows: \\\\\`

If a path key contains a period, it must by surrounded by backticks:   $.get("path.to.\`Mr. Johnson's house\`");

If a path ends in a backtick, or in a backslash, it must by surrounded by double backticks:   

$.get("windows.path.\`\`C:\\\\\`\`");

If you want to create a path key with a number value, like "2", as opposed to an array with an index of 2, you must surround the key in backticks.

$.node("a.b.0");  // makes

"a":{
	"b":[
		{}
	]
}

$.node("a.b.\`0\`"); // makes

"a":{
	"b":{
		"0":{}
	}
}




## Additional Info

The JSON string must start with braces {}.

The aim here is convenience and flexibiliy, and to give the Java manipulation a "Javascript like feel". I have not tested the performance.

It is thread safe, though, I am not sure if Gson is entirely thread safe. This is why the JsonQuery class contains the static member ensureThreadSaftey. If you are running a singe threaded application, you can set this to false and use a statically created Gson, as opposed to creating the object on request.

This code traverses the JsonElements tree and builds a corresponding tree of JsonQuery nodes (Containing HashMaps, ArrayLists, and Gson primitive types).


                
