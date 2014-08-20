package src.JSQL;

import java.util.ArrayList;
import java.util.List;

public class JSQLTokenMap<K,V> {
		List<V> tokens;
		List<K> type;
		public JSQLTokenMap(){
			this.tokens = new ArrayList<V>();
			this.type = new ArrayList<K>();
		}
}
