package src;

import src.JSQL.JSQLNumber;

@SuppressWarnings("serial")
public class JsonQueryNumber extends Number implements JSQLNumber{
	
	private transient Number n;
    public JsonQueryNumber(Number n){
    	this.n = n;
    }
    
    public String toString(){
    	return n.toString();
    }

	@Override
	public double doubleValue() {
		return n.doubleValue();
	}

	@Override
	public float floatValue() {
		return n.floatValue();
	}

	@Override
	public int intValue() {
		return n.intValue();
	}

	@Override
	public long longValue() {
		return n.longValue();
	}
}
