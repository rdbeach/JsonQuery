package src.JSQL;

public class JSQLUtil {
	public static boolean isInteger( String input ) {
	    try {
	        Integer.parseInt( input );
	        return true;
	    }
	    catch( Exception e ) {
	        return false;
	    }
	}
	
	public static boolean isNumeric(String str)
	{
	  return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
	}

	public static Object formatElementFromString(String value)
	{
		Object val;
		if(JSQLUtil.isNumeric(value)){
			if(Math.floor(Double.valueOf(value))==Double.valueOf(value)){
				val = Integer.valueOf(value);
			}else{
				val = Double.valueOf(value);
			}
		}else if(value.equalsIgnoreCase("true")||value.equalsIgnoreCase("false")){
			val = new Boolean(value);
		}else if(value.equalsIgnoreCase("null")){
			val = null;
		}else{
			val = (String)value;
		};
		return val;
	}
}