package src.JSQL;


public class JSQLToken<K,V> {
		K index;
		K type;
		V value;
		public JSQLToken(K type,V token){
			this.type = type;
			this.value = token;
		}
		public JSQLToken(K type,K count,V token){
			this.type = type;
			this.index= count;
			this.value = token;
		}
}
