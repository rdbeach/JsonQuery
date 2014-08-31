package src;

public class JsonQueryToken<T> {
	String type;
	T value;
	public JsonQueryToken(T value){
		this.value = value;
		if(value instanceof String){
			type="String";
		}else{
			type="Number";
		}
	}
	public int num(){
		if(type.equals("Number"))
			return (Integer)value;
		else return 0;
	}
	public String str(){
		if(type.equals("String"))
		return (String)value;
		else return String.valueOf(value);
	}
	public T getValue(){
		return value;
	}
}
