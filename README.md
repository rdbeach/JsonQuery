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
