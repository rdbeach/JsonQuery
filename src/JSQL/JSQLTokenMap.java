package src.JSQL;

import java.util.ArrayList;
import java.util.List;

public class JSQLTokenMaps<K,V> {
		List<V> tokens;
		List<K> index;
		List<K> type;
		public JSQLTokenMap(){
			this.tokens = new ArrayList<V>();
			this.type = new ArrayList<K>();
		}
}
