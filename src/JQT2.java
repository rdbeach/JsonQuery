package src;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Iterator;

import org.adrianwalker.multilinestring.Multiline;

import src.JSQL.JSQLResultSet;
import src.JSQL.JSQLNode;
import src.JSQL.JSQLArray;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.LazilyParsedNumber;

public class JQT2 {
	
	 /**
	  	{
		  "empID": 100.2,
		  "name": "robert",
		  "permanent": false,
		  "address": {
		    "street": "Foolhill Blvd",
		    "city": "Pasadena",
		    "zipcode": 91011,
		    "properties": {
			    "age": null,
			    "salary": {
			    	"mike":{
			    		"base":2000,
			    		"last_name":"bubba"
			    	},
			    	"richard":{
			    		"last_name":"ruth",
			    		"base":3000
			    	},
			    	"jim":{
			    		"last_name":"rogin",
			    		"base":5000
			    	}
			    }
		    },
		    "corry":{
		    		"last_name":"horry",
		    		"base":3000
		    }
		  },
		  "phoneNumbers": [
		    false,
		    9876543,
		    1,
		    2,
		    [{"home phone":7904004}]
		  ],
		  "role": "Java Developer",
		  "employed":true,
		  "cities": [
		    "Los Angeles",
		    "New York"
		  ],
		  "properties": {
		    "age": null,
		    "salary": {
		    	"mike":{
		    		"base":2000,
		    		"last_name":"bubba"
		    	},
		    	"richard":{
		    		"last_name":"ruth",
		    		"base":3000
		    	},
		    	"jim":{
		    		"last_name":"godzilla",
		    		"base":5000
		    	}
		    }
		  }
		}
	*/
   @Multiline private static String msg;
   
   /**
 	{
 		"data": {
 			"something":"",
  			"translations": [
   				{
    				"translatedText": "Hello world"
   				}
  			]
 		}}
	
    */
   @Multiline private static String msg2;
   
   /**
	{
    "array" : [ 1, { "name" : "Billy" }, null ],
    "object" : {
      "id" : null,
      "names" : [ "Bob", "Bobby" ]
    }
  }
	
   */
  @Multiline private static String msg3;

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
            
            JsonQuery $ = JsonQuery.fromJson(msg);
            //out($.get("phoneNumbers[1]").str());
            
            //men.workers:*
            
            JSQLResultSet<JsonQuery> resultSet = $.jsql("select '*' from 'address:'");
            out("printing");
            for(JsonQuery result : resultSet){
            	out(result.getKey());
            }
           
           
        }catch(Exception e){System.out.println(e);};
        
    }
}
