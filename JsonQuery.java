

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LazilyParsedNumber;


public class JsonQuery implements Iterable<Object>{

	public static final boolean ensureThreadSafety=true;
	
	public static final Gson static_gson = new GsonBuilder().
		disableHtmlEscaping().
		registerTypeAdapter(JsonQuery.class, new JsonQueryDeserializer()).serializeNulls().
		registerTypeAdapter(JsonQuery.class, new JsonQuerySerializer()).
		registerTypeAdapter(JsonQueryNumber.class, new JsonQueryNumberSerializer()).
		create();
	
	public transient String key="";
	public Object element;
	
	public transient JsonQuery antenode;

	
	public static JsonQuery fromJson(String json){
		Gson gson = new GsonBuilder().
				disableHtmlEscaping().
				registerTypeAdapter(JsonQuery.class, new JsonQueryDeserializer()).
				registerTypeAdapter(JsonQuery.class, new JsonQuerySerializer()).
				registerTypeAdapter(JsonQueryNumber.class, new JsonQueryNumberSerializer()).
				serializeNulls().
				create();
		try{
			return gson.fromJson(json,JsonQuery.class);
		}catch(Throwable e){
			e.printStackTrace();
		}
		return null;
		
	}
	

	private transient Gson gson;
	
	private Gson getGson(){
		gson = (gson!=null? gson : (ensureThreadSafety?new GsonBuilder().
					disableHtmlEscaping().
					registerTypeAdapter(JsonQuery.class, new JsonQueryDeserializer()).
					registerTypeAdapter(JsonQuery.class, new JsonQuerySerializer()).
					registerTypeAdapter(JsonQueryNumber.class, new JsonQueryNumberSerializer()).
					serializeNulls().
					create():static_gson));
		return gson;
	}
	
	public JsonQuery(){
		
	}
	
	public JsonQuery(Object node){
		this.element = node;
	}
	
	public JsonQuery(Object node, String key){
		this.element = node;
		this.key = key;
	}
	
	// Single node tree traversal operator
	
	public JsonQuery _(String key){
		if(element instanceof JsonQueryObject){
			JsonQuery node = (JsonQuery)((JsonQueryObject)element).get(key);
			if(node!=null)return node;
		}
		return new JsonQuery(null,null);
	}
	
	public JsonQuery _(int key){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQuery node = (JsonQuery)((JsonQueryArray)element).get(key);
				if(node!=null)return node;
			}
		}
		return new JsonQuery(null,null);
	}
	
	private String[] getKeys(String path){
		String[] keys = path.replace("[",".").replace("]","").split("(?<!\\\\)[.]");
		int count=0;
		for(String key:keys){
			keys[count]=key.replace("\\.", ".");
			count++;
		}
		return keys;
	}
	
	// Multinode: tree traversal operator
	
	public JsonQuery get(String path) {
		String[] keys = getKeys(path);
		return path(this,keys,false);
	}
	
	private JsonQuery path(JsonQuery node, Object[] keys, boolean addTreeInfo){
		try{
			int i=0;
			for(i=0;i<keys.length;i++){
				JsonQuery nextNode=null;
				if(JsonQueryUtil.isInteger((String)keys[i])){
					nextNode = (JsonQuery) node._(Integer.parseInt((String)keys[i]));
				}else{
					nextNode = (JsonQuery) node._((String)keys[i]);
				}
				if(nextNode!=null&&addTreeInfo){
					if(((Object)node.element instanceof JsonQueryObject)){
						nextNode.key=(String)keys[i];
					}
					nextNode.antenode=node;
				}
				node=nextNode;
				if(node==null)break;
			}
			if(node!=null)return node;
		}catch(Throwable e){
			handleException(e);
		}
		return new JsonQuery(null,null);
	}
	
	// Multinode: get or create a path
	
	public JsonQuery node(String path) {
		String[] keys = getKeys(path);
		JsonQuery node = this;
		try{
			int i=0;
			for(i=0;i<keys.length;i++){
				if(JsonQueryUtil.isInteger(keys[i])){
					int index = Integer.parseInt(keys[i]);
					JsonQuery nextNode = (JsonQuery) node._(index);
					if(!nextNode.exists()){
						if(!(node.element instanceof JsonQueryArray)) {
							if(node.element==null){
								node.element = new JsonQueryArray();
							}else{
								return new JsonQuery(null,null);
							}
						}
						JsonQueryArray array = (JsonQueryArray) node.element;
						for(int k = array.size(); k<index+1; k++){
							array.add(new JsonQuery(null));
						}
						nextNode.key="";
						array.set(index,nextNode);
						if(i+1<keys.length){
							if(JsonQueryUtil.isInteger(keys[i+1])){
								nextNode.element=new JsonQueryArray();
							}else{
								nextNode.element=new JsonQueryObject();
							}
						}
					}
					node=nextNode;
				}else{
					JsonQuery nextNode = (JsonQuery) node._(keys[i]);
					if(!nextNode.exists()){
						nextNode.key=keys[i];
						if(!(node.element instanceof JsonQueryObject)) {
							if(node.element==null){
								node.element = new JsonQueryObject();
							}else{
								return new JsonQuery(null,null);
							}
						}
						((JsonQueryObject) node.element).put(keys[i],nextNode);
						if(i+1<keys.length){
							if(JsonQueryUtil.isInteger(keys[i+1])){
								nextNode.element=new JsonQueryArray();
							}else{
								nextNode.element=new JsonQueryObject();
							}
						}
					}
					node=nextNode;
				}
			}
			return node;
		}catch(Throwable e){
			handleException(e);
		}
		return new JsonQuery(null,null);
	}
	
	// Returns a subset of the JsonQuery tree
	
	public JsonQueryArray jql(String queryString){
		HashMap<String,String> clauses = parseQueryString(queryString);
		if(clauses==null)return new JsonQueryArray();
		return executeJQL(clauses);
	}
	
	private JsonQueryArray executeJQL(HashMap<String,String> clauses){
		
		out("FROM clause");
		
		JsonQueryArray fromResultSet = new JsonQueryArray();
		String fromClause="*";
		if(clauses.containsKey("From"))fromClause=clauses.get("From");
		if(fromClause.equals("*")){
			this.key="root";
			fromResultSet.add(this);
		}else{
			ArrayList<JsonQueryTokens> queries = getTokens(fromClause);
			executeJQLClause(this,fromResultSet,queries);
		}
		
		out("WHERE clause");
		
		if(clauses.containsKey("Where")){
			String whereClause=clauses.get("Where");
			executeWhereClause(fromResultSet,whereClause);
		}
		
		out("SELECT clause");
		
		JsonQueryArray resultSet = new JsonQueryArray();
		String selectClause=clauses.get("Select");
		if(selectClause.equals("*")){
			resultSet=fromResultSet;
		}else{
			ArrayList<JsonQueryTokens> queries = getTokens(selectClause);
			for(JsonQuery node: fromResultSet){
					executeJQLClause(node,resultSet,queries);
			}
		}
		
		return resultSet;
	}
	
	private void executeJQLClause(JsonQuery node, 
			JsonQueryArray resultSet,
			ArrayList<JsonQueryTokens> queries
			){
		for(JsonQueryTokens tokens:queries){
			int branches_up=0;
			int i = tokens.path.length-1;
			while(i>0&&(tokens.path[i].equals(""))){
				branches_up++;
				i--;
			}
			getResultSet(node,resultSet,tokens,0,branches_up);
		}
	}
	
	private void executeWhereClause(JsonQueryArray array,String whereClause){
		
		int gtIndex = whereClause.lastIndexOf(">");
		int ltIndex = whereClause.lastIndexOf("<");
		int equalsIndex = whereClause.lastIndexOf("=");
		int operatorIndex = Math.max(Math.max(gtIndex, ltIndex), equalsIndex);
		System.out.println("operator index:"+operatorIndex);
		if(operatorIndex!=-1&&operatorIndex<whereClause.length()-1){
			String[] operands = new String[2];
			operands[0] = whereClause.substring(0,operatorIndex);
			operands[1] = whereClause.substring(operatorIndex+1,whereClause.length());
			System.out.println(operands[0]);
			System.out.println(operands[1]);
			Iterator<JsonQuery> iterator = array.iterator();
			while (iterator.hasNext()) {
			   JsonQuery node = iterator.next();
				boolean pass = false;
				for(JsonQuery subNode:node.jql("Select '*' From '"+operands[0]+"'")){
				    Object val = subNode.val();
					switch (whereClause.charAt(operatorIndex)){
						case '<':
							System.out.println("here");
							if(val instanceof JsonQueryNumber){
								if(((JsonQueryNumber)val).doubleValue()<Double.valueOf(operands[1]))pass=true;
							}
							break;
						case '>':
							System.out.println(">: "+ val + " " +operands[1]);
							if(val instanceof JsonQueryNumber){
								if(((JsonQueryNumber)val).doubleValue()>Double.valueOf(operands[1]))pass=true;
							}
							break;
						case '=':
							System.out.println("=: "+ val + " " +operands[1]);
							if(val instanceof JsonQueryNumber){
								if(((JsonQueryNumber)val).doubleValue()==Double.valueOf(operands[1]))pass=true;
							}else if(val instanceof Boolean){
								if((Boolean)val.equals(Boolean.valueOf(operands[1])))pass=true;
							}else if(val instanceof String){
								if(val.equals(operands[1]))pass=true;
							}
					}
					System.out.println("pass: "+ pass);
					if(!pass){
						iterator.remove();
						break;
					}
				}
			}
		}else{
			array.clear();
		}
	}
	
	private void getResultSet(
			JsonQuery node,
			JsonQueryArray resultSet,
			JsonQueryTokens tokens,
			int currentIndex,
			int branches_up){
		out("Starting resultSet process");
		ArrayList<String> searchPath= new ArrayList<String>();
		boolean next = false;
		boolean child = false;
		int index;
		int endOfPath = tokens.path.length-branches_up;
		for(index = currentIndex;index<endOfPath;index++){
			out("iterating path-index: "+index+ " value:" + tokens.path[index]);
			if(tokens.path[index].equals("*")||tokens.path[index].equals("")){
				next = true;
				break;
			}else if(tokens.path[index].equals(":")){
				child=true;
				break;
			}else{
				searchPath.add(tokens.path[index]);
			}
		}
		if(!searchPath.isEmpty()){
			out2("grabing path");
			JsonQuery nextNode = path(node,searchPath.toArray(),true);
			if(nextNode.element!=null){
				if(index == endOfPath){
					out2("adding from path: "+ nextNode.key);
					addToResultSet(nextNode,resultSet,branches_up);
				}else{
					out("continue");
					getResultSet(nextNode,resultSet,tokens,index,branches_up);
				}
			}
			return;
		}
		if(next){
			out("next");
			index++;
			if(node.element instanceof JsonQueryObject){
				for (JsonQuery nextNode:node.each()){
					nextNode.antenode=node;
					if(index == endOfPath){
						out("adding from next: "+ branches_up);
						addToResultSet(nextNode,resultSet,branches_up);
					}else{
						getResultSet(nextNode,resultSet,tokens,index,branches_up);
					}
				}
			}
			return;
		}
		if(child){
			index++;
			if(index<endOfPath&&tokens.path[index]!=""&&tokens.path[index]!=""){
				out("child search:"+index);
				keySearch(node,resultSet,tokens,index,branches_up);
			}
		}
	}
	
	private void addToResultSet(JsonQuery node,JsonQueryArray resultSet,int branches_up){
		for(int i = 0;i<branches_up;i++){
			node=node.antenode;
		}
		resultSet.add(node);
	}
	
	private void keySearch(
			JsonQuery node,
			JsonQueryArray resultSet,
			JsonQueryTokens tokens,
			int currentIndex,
			int branches_up){
		
		int endOfPath = tokens.path.length-branches_up;
		String key = tokens.path[currentIndex];
		if(node.element instanceof JsonQueryObject||node.element instanceof JsonQueryArray){
			for (JsonQuery nextNode:node.each()){
				out(nextNode.key);
				nextNode.antenode=node;
				if(nextNode.key.equals(key)){
					out("match found");
					if(currentIndex+1 == endOfPath){
						out(branches_up);
						out("adding from keysearch");
						addToResultSet(nextNode,resultSet,branches_up);
					}else{
						out("not the end");
						getResultSet(nextNode,resultSet,tokens,currentIndex+1,branches_up);
					}
				}else{
					keySearch(nextNode,resultSet,tokens,currentIndex,branches_up);
				}
			}
		}
	}
	
	private HashMap<String,String> parseQueryString(String queryString){
		String[] KEYWORDS = {"Select","From","Where","Order By","Limit"};
		String[] tokens = queryString.trim().split("(?<!\\\\)'");
		if(tokens.length<2){
			return null;
		}
		HashMap<String,String> tokenMap = new HashMap<String,String>();
		int precedence=0;
		boolean selectFound=false;
		for(int i = 0;i<tokens.length;i+=2){
			if(tokens[i].trim().toLowerCase().startsWith("limit")){
				tokens = tokens[i].split(" ");
				if(tokens.length>1){
					tokens[1].trim();
				}
				if(tokens.length!=2){
					return null;
				}
				out(KEYWORDS[precedence]+" "+tokens[1]);
				tokenMap.put("Limit",tokens[1]);
			}else{
				String keyword = tokens[i].trim();
				boolean found = false;
				int count = 0;
				for (String kw : KEYWORDS) {
				    if (keyword.equalsIgnoreCase(kw)) {
				        if(precedence > count){
				        	return null;
				        }
				        precedence=count;
				        found = true;
				        break;
				    }
				    count++;
				}
				if (!found||i+1>=tokens.length) {
					return null;
				}
				if(keyword.equalsIgnoreCase("Select"))selectFound=true;
				out(KEYWORDS[precedence]+" "+tokens[i+1]);
				tokenMap.put(KEYWORDS[precedence],tokens[i+1]);
			}
		}
		if(!selectFound){
			return null;
		}
		return tokenMap;
	}
	
	private ArrayList<JsonQueryTokens> getTokens(String queryString){
		ArrayList<JsonQueryTokens> queryList = new ArrayList<JsonQueryTokens>();
		String[] queries = queryString.split(",");
		for(String query:queries){
			JsonQueryTokens tokens = new JsonQueryTokens();
			String[] paths = query.split("!");
			int j = 0;
			for(String path:paths){
				out(path);
				path = path.replaceAll("(?<!\\\\):",".:.").replace("[",".").replace("]","");
				if(path.startsWith(".:"))path = path.substring(1);
				String[] keys = path.split("(?<!\\\\)[.]",-1);
				int k = 0;
				for(String key:keys){
					out(key);
					keys[k]=key.replace("\\.", "."). // path
							replace("\\*", "*"). // all
							replace("\\:",":").  // child
							replace("\\!","!").  // not
							replace("\\,",",").  // separator
							replace("\\[","[");  // keyword
					k++;
				}
				if(j==0){
					tokens.path=keys;
				}else{
					if(tokens.exceptionPaths==null)tokens.exceptionPaths = new ArrayList<String[]>();
					tokens.exceptionPaths.add(keys);
				}
			}
			queryList.add(tokens);
		}
		return queryList;
	}
	
	// Multinode gets
	
	public Object val() {
			return element;
	}
	
	public String str() {
		if(element!=null){
			if(element instanceof JsonQueryObject||element instanceof JsonQueryArray){
				return this.toJson();
			}else{
				return element.toString();
			}
		}
		return null;
	}
	
	public boolean bool() {
		if(element!=null){
			return (Boolean) element;
		}
		return false;
	}
	
	public int i() {
		if(element!=null){
			return ((Number)element).intValue();
		}
		return 0;
	}
	
	public long l() {
		if(element!=null){
			return ((Number)element).longValue();
		}
		return 0;
	}
	
	public double d() {
		if(element!=null){
			return ((Number)element).doubleValue();
		}
		return 0;
	}
	
	// Gets for the single node traversal queries
	
	public Object val(String key) {
		if(element instanceof JsonQueryObject){
			JsonQuery node = (JsonQuery)((JsonQueryObject)element).get(key);
			if(node!=null) return node.element;
		}
		return null;
	}
	
	public Object val(int key) {
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQuery node = (JsonQuery)((JsonQueryArray)element).get(key);
				if(node!=null) return node.element;
			}
		}
		return null;
	}
	
	public String str(String key) {
		if(element instanceof JsonQueryObject){
			JsonQuery node = (JsonQuery)((JsonQueryObject)element).get(key);
			if(node!=null){
				if(node.element instanceof JsonQueryObject||node.element instanceof JsonQueryArray){
					return node.toJson();
				}else if(node.element!=null){
					return node.element.toString();
				}
			}
		}
		return null;
	}
	
	public String str(int key) {
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQuery node = (JsonQuery)((JsonQueryArray)element).get(key);
				if(node!=null){
					if(node.element instanceof JsonQueryObject||node.element instanceof JsonQueryArray){
						return node.toJson();
					}else if(node.element!=null){
						return node.element.toString();
					}
				}
			}
		}
		return null;
	}
	
	public boolean bool(String key){
		if(element instanceof JsonQueryObject){
			JsonQuery node = (JsonQuery)((JsonQueryObject)element).get(key);
			if(node!=null) return (Boolean)node.element;
			
		}
		return false;
	}
	
	public boolean bool(int key){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQuery node = (JsonQuery)((JsonQueryArray)element).get(key);
				if(node!=null) return (Boolean)node.element;
			}
		}
		return false;
	}
	
	public int i(String key){
		if(element instanceof JsonQueryObject){
			JsonQuery node = (JsonQuery)((JsonQueryObject)element).get(key);
			if(node!=null&&node.element!=null) return ((Number)(node.element)).intValue();
		}
		return 0;
	}
	
	public int i(int key){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQuery node = (JsonQuery)((JsonQueryArray)element).get(key);
				if(node!=null&&node.element!=null) return ((Number)(node.element)).intValue();
			}
		}
		return 0;
	}
	
	public long l(String key){
		if(element instanceof JsonQueryObject){
				JsonQuery node = (JsonQuery)((JsonQueryObject)element).get(key);
				if(node!=null&&node.element!=null) return ((Number)(node.element)).longValue();
		}
		return 0;
	}
	
	public long l(int key){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
					JsonQuery node = (JsonQuery)((JsonQueryArray)element).get(key);
					if(node!=null&&node.element!=null) return ((Number)(node.element)).longValue();
			}
		}
		return 0;
	}
	
	public double d(String key){
		if(element instanceof JsonQueryObject){
			JsonQuery node = (JsonQuery)((JsonQueryObject)element).get(key);
			if(node!=null&&node.element!=null) return ((Number)(node.element)).doubleValue();
		}
		return 0;
	}
	
	public double d(int key){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQuery node = (JsonQuery)((JsonQueryArray)element).get(key);
				if(node!=null&&node.element!=null) return ((Number)(node.element)).doubleValue();
			}
		}
		return 0;
	}
	
	// Multinode sets
	
	
	public JsonQuery set(Object value){
		if(value instanceof JsonQuery){
			value = this;
		}else{
			element = formatValue(value);
		}
		return this;
	}
	
	public JsonQuery jset(String value){
		if(value=="")value="\"\"";
		String json = "{\"obj\":"+value+"}";
		try{
			JsonQuery node = getGson().fromJson(json,JsonQuery.class);
			element =  node._("obj").element;
		}catch(Throwable e){
			handleException(e);
		}
		return this;
	}
	
	// Sets for the single node traversal
	
	public JsonQuery set(String key, Object value){
		if(element == null)element = new JsonQueryObject();
		if(element instanceof JsonQueryObject){
			((JsonQueryObject)element).set(key,formatValue(value));
		}
		return this;
	}
	
	public JsonQuery set(int key, Object value){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				((JsonQueryArray)element).jsonQueryArraySet(key,formatValue(value));
			}
		}
		return this;
	}
	
	public JsonQuery jset(String key, String value){
		if(element == null)element = new JsonQueryObject();
		if(element instanceof JsonQueryObject){
			try{
				((JsonQueryObject)element).jset(key,value,getGson());
			}catch(Throwable e){
				handleException(e);
			}
		}
		return this;
	}
	
	public JsonQuery jset(int key, String value){
		if(element instanceof JsonQueryArray){
			try{
				if(key<((JsonQueryArray)element).size()){
					((JsonQueryArray)element).jset(key,value,getGson());
				}
			}catch(Throwable e){
				handleException(e);
			}
		}
		return this;
	}
	
	
	
	// adds for multinode and single node traversal
	
	
	
	public JsonQuery add(Object value){
		if(element == null)element = new JsonQueryArray();
		if(element instanceof JsonQueryArray){
			if(value instanceof JsonQuery){
				((JsonQueryArray)element).add((JsonQuery) value);
			}else{
				((JsonQueryArray)element).add(new JsonQuery(formatValue(value)));
			}
		}
		return this;
	}
	
	public JsonQuery add(int i,Object value){
		
		if(element == null && i==0)element = new JsonQueryArray();
		if(element instanceof JsonQueryArray){
			try{
				if(i<=((JsonQueryArray)element).size()){
					if(value instanceof JsonQuery){
						((JsonQueryArray)element).add(i,(JsonQuery) value);
					}else{
						((JsonQueryArray)element).add(i,new JsonQuery(formatValue(value)));
					}
				}
			}catch(Throwable e){
				handleException(e);
			}
		}
		return this;
	}
	
	public JsonQuery jadd(String value){
		if(element == null)element = new JsonQueryArray();
		if(element instanceof JsonQueryArray){
			try{
				((JsonQueryArray)element).jadd(value,getGson());
			}catch(Throwable e){
				handleException(e);
			}
		}
		return this;
	}
	
	public JsonQuery jadd(int key, String value){
		if(element == null&&key==0)element = new JsonQueryArray();
		if(element instanceof JsonQueryArray){
			try{
				if(key<=((JsonQueryArray)element).size()){
					((JsonQueryArray)element).jadd(key,value,getGson());
				}
			}catch(Throwable e){
				handleException(e);
			}
		}
		return this;
	}
	
	// Removing elements
	
	public JsonQuery toNull(){
		this.element=null;
		return this;
	}
	
	public JsonQuery remove(String key){
		if(element instanceof JsonQueryObject){
			((JsonQueryObject)element).remove(key);
		}
		return this;
	}
	
	public JsonQuery remove(int key){
		if(element instanceof JsonQueryArray){
			((JsonQueryArray)element).remove(key);
		}
		return this;
	}
	
	// Iterating
	
	public JsonQueryArray each(){
		if(element instanceof JsonQueryArray){
			return (JsonQueryArray) element;
		}
		if(element instanceof JsonQueryObject){
			JsonQueryArray  array = new JsonQueryArray();
			for (Entry<String, JsonQuery> entry : ((JsonQueryObject)element).entrySet()) {
			    String key = entry.getKey();
			    Object value = entry.getValue();
			    JsonQuery node = new JsonQuery(((JsonQuery)value).element,key);
			    array.add(node);
			}
			return array;
		}
		return new JsonQueryArray();
	}

	public Iterator<Object> iterator() {
		// TODO this is a stub
		return null;
	}
	
	public boolean hasNext(){
		// TODO
		if(element instanceof JsonQueryArray){
			//Iterator<Object> it = ((JsonQueryArrayList)node).iterator();
			//if()
		}
		return false;
	}
	
	// Type Determination
	
	public boolean exists(){
		if(key==null)return false;
		return true;
	}
	
	public String type(){
		if(element instanceof JsonQueryObject){
			return "object";
		}else if(element instanceof JsonQueryArray){
			return "array";
		}else if(element instanceof String){
			return "string";
		}else if(element instanceof JsonQueryNumber){
			return "number";
		}else if(element instanceof Boolean){
			return "boolean";
		}else{
			return "null";
		}
	}
	
	public boolean isLeaf(){
		if(element instanceof JsonQueryObject || element instanceof JsonQueryArray)
			return false;
		return true;
	}
	
	private  Object formatValue(Object value){
		if(value instanceof Number){
			value = new JsonQueryNumber(new LazilyParsedNumber(value.toString()));
		}
		return  value;
	}
		
	public boolean isObject(){
		if(element instanceof JsonQueryObject)
			return true;
		return false;
	}
		
	public boolean isArray(){
		if(element instanceof JsonQueryArray)
			return true;
		return false;
	}
	
	// Exception Handling
	
	private void handleException(Throwable e){
		System.out.println("error");
		e.printStackTrace();
	}
	
	// Write to JSON string
	
	public String toJson(){
		try{
			return (String) getGson().toJson(this.element);
		}catch(Throwable e){
			handleException(e);
		}
		return null;
	}
	
	private void out(Object o){
		//System.out.println(o);
	}
	private void out2(Object o){
		System.out.println(o);
	}
	
}
