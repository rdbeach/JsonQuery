package src;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import src.JSQL.JSQLEngine;
import src.JSQL.JSQLNode;
import src.JSQL.JSQLResultSet;
import src.JSQL.JSQLUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LazilyParsedNumber;


public class JsonQueryNode extends JsonQuery implements JSQLNode{

	private static final String EMPTY = "";

	private static final String PATH_DELIMITER_REGEX = "(?<!\\\\)[.]";
	private static final String DOUBLE_BACKSLASH = "\\";
	private static final String QUADRUPLE_BACKSLASH = "\\\\";
	private static final String DOLLARSIGN = "$";
	private static final String DOLLARSIGN_BACKSLASH = "\\$";
	private static final String BACKTICK = "`";
	private static final String DOUBLE_BACKSLASH_BACKTICK = "\\`";
	private static final String DOUBLE_BACKTICK = "``";
	private static final String KEYQUOTES = "``(?!`)|(?<!\\\\)`";
	private static final String KEY_PLACEHOLDER = "``";
	public transient String key = EMPTY;

	@Override
	public String getKey(){
		return key;
	}

	@Override
	public void setKey(String key) {
		// TODO Auto-generated method stub
		this.key = key;
	}

	public Object element;

	private transient JsonQueryNode antenode;

	@Override
	public void setAntenode(JSQLNode antenode) {
		// TODO Auto-generated method stub
		this.antenode = (JsonQueryNode)antenode;
	}

	public JsonQueryNode getAntenode(){
		return this.antenode;
	}

	private transient Gson gson;

	private Gson getGson(){
		gson = (gson!=null? gson : (ensureThreadSafety?new GsonBuilder().
				disableHtmlEscaping().
				registerTypeAdapter(JsonQueryNode.class, new JsonQueryDeserializer()).
				registerTypeAdapter(JsonQueryNode.class, new JsonQuerySerializer()).
				registerTypeAdapter(JsonQueryNumber.class, new JsonQueryNumberSerializer()).
				serializeNulls().
				create():static_gson));
		return gson;
	}

	public JsonQueryNode(){

	}

	public JsonQueryNode(Object node){
		this.element = node;
	}

	public JsonQueryNode(Object element, String key){
		this.element = element;
		this.key = key;
	}

	@Override
	public JsonQueryNode createNewNode(Object element, String key){
		return new JsonQueryNode(element,key);
	}
	
	// create an object
	/* (non-Javadoc)
	 * @see JsonQuery#obj()
	 */
	@Override
	public JsonQueryNode obj(){
		return new JsonQueryNode(new JsonQueryObject(),"");
	}
	
	// create an array
	/* (non-Javadoc)
	 * @see JsonQuery#arr()
	 */
	@Override
	public JsonQueryNode arr(){
		return new JsonQueryNode(new JsonQueryArray(),"");
	}

	// Node Traversal

	/* (non-Javadoc)
	 * @see JsonQuery#_(java.lang.String)
	 */
	@Override
	public JsonQueryNode _(String key){
		if(element instanceof JsonQueryObject){
			JsonQueryNode node = (JsonQueryNode)((JsonQueryObject)element).get(key);
			if(node!=null)return node;
		}
		return new JsonQueryNode(null,null);
	}

	/* (non-Javadoc)
	 * @see JsonQuery#_(int)
	 */
	@Override
	public JsonQueryNode _(int key){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQueryNode node = (JsonQueryNode)((JsonQueryArray)element).get(key);
				if(node!=null)return node;
			}
		}
		return new JsonQueryNode(null,null);
	}

	@Override
	public JsonQueryNode getNextNode(String key) {
		return _(key);
	}

	@Override
	public JsonQueryNode getNextNode(int key) {
		return _(key);
	}

	// Path traversal

	/* (non-Javadoc)
	 * @see JsonQuery#get(java.lang.String)
	 */
	@Override
	public JsonQueryNode get(String path) {
		JsonQueryToken<?>[] keys = getKeys(path);
		return path(this,keys,false);
	}

	private JsonQueryToken<?>[] getKeys(String path){

		List<String> tokensList = fragmentOnQuotedKeys(path);
		Object[] tokensStr=tokensList.toArray();

		if(tokensStr==null){
			return new JsonQueryToken[]{};
		}

		StringBuilder str = new StringBuilder(EMPTY);
		for (int i = 0; i < tokensStr.length; i+=2) {
			str.append((String)tokensStr[i]);
			if(i+2<tokensStr.length)
				str.append(KEY_PLACEHOLDER);
		}
		path=str.toString();

		String[] keysString = path.split(PATH_DELIMITER_REGEX);
		JsonQueryToken<?>[] keys = new JsonQueryToken<?>[keysString.length];
		int replacementCount=1;
		int i = 0;
		boolean isReplacement=false;
		for(Object keyObj:keysString){
			String key = (String)keyObj;
			while(key.contains(KEY_PLACEHOLDER)){
				key = key.replaceFirst(KEY_PLACEHOLDER,((String)tokensStr[replacementCount]).
						replace(DOUBLE_BACKSLASH, QUADRUPLE_BACKSLASH).
						replace(DOLLARSIGN, DOLLARSIGN_BACKSLASH));
				replacementCount+=2;
				isReplacement=true;
			}
			key=key.replace(DOUBLE_BACKSLASH_BACKTICK, BACKTICK);
			if(JSQLUtil.isInteger(key)&&!isReplacement){
				keys[i++]=new JsonQueryToken<Integer>(Integer.valueOf(key));
			}else{
				keys[i++]=new JsonQueryToken<String>(key);
			}
			isReplacement=false;
		}
		return keys;
	}

	private JsonQueryNode path(JsonQueryNode node, JsonQueryToken<?>[] keys, boolean addTreeInfo){
		try{
			int i=0;
			for(i=0;i<keys.length;i++){
				JsonQueryNode nextNode=null;
				if(keys[i].type.equals("Number")){
					nextNode = (JsonQueryNode) node._(keys[i].num());
					if(!nextNode.exists()){
						nextNode = (JsonQueryNode) node._(String.valueOf(keys[i].num()));
					}
				}else{
					nextNode = (JsonQueryNode) node._(keys[i].str());
				}
				if(nextNode.exists()&&addTreeInfo){
					nextNode.key=keys[i].str();
					nextNode.antenode=node;
				}
				node=nextNode;
				if(!nextNode.exists())break;
			}
			if(node.exists())return node;
		}catch(Throwable e){
			handleException(e);
		}
		return new JsonQueryNode(null,null);
	}
	
	private List<String> fragmentOnQuotedKeys(String path){
		Pattern pattern = Pattern.compile(KEYQUOTES);
		Matcher matcher = pattern.matcher(path);

		List<String> tokens = new ArrayList<String>();

		String lastFragment = EMPTY;
		int index=0;
		int lastIndex=0;
		int group=0;
		int capture_group=0;
		boolean someGroupIsCaptured=false;

		while (matcher.find()) {
			String groupStr = matcher.group(0);
			if(groupStr.equals(DOUBLE_BACKTICK)){
				group=1;
			}else{
				group=2;
			}
			if(!someGroupIsCaptured||group==capture_group){
				index=matcher.start();
				String token = path.substring(lastIndex,index);
				tokens.add(token);
				lastIndex=matcher.end();
				capture_group=group;
				someGroupIsCaptured = !someGroupIsCaptured;
				// special case 1 (captured ` found \``)
			}else if(group==1&&capture_group==2){  
				index=matcher.start()+1;
				String token = path.substring(lastIndex,index);
				tokens.add(token);
				lastIndex=index+1;
				someGroupIsCaptured = !someGroupIsCaptured;
			}
		}
		if(someGroupIsCaptured){
			return null;
		}
		if(lastIndex<path.length()){
			lastFragment = path.substring(lastIndex,path.length());
		}else{
			lastFragment=EMPTY;
		}
		tokens.add(lastFragment);
		return tokens;
	}

	// node: get or create a path
	/* (non-Javadoc)
	 * @see JsonQuery#node(java.lang.String)
	 */
	@Override
	public JsonQuery node(String path) {
		JsonQueryToken<?>[] keys = getKeys(path);
		JsonQueryNode node = this;
		try{
			int i=0;
			for(i=0;i<keys.length;i++){
				if(!(keys[i].type.equals("String"))){
					int index = keys[i].num();
					JsonQueryNode nextNode = (JsonQueryNode) node._(index);
					if(!nextNode.exists()){
						nextNode = (JsonQueryNode) node._(String.valueOf(index));
					}
					if(!nextNode.exists()){
						if(!(node.element instanceof JsonQueryArray)) {
							if(node.element==null){
								node.element = new JsonQueryArray();
							}else{
								return new JsonQueryNode(null,null);
							}
						}
						JsonQueryArray array = (JsonQueryArray) node.element;
						for(int k = array.size(); k<index+1; k++){
							array.add(new JsonQueryNode(null));
						}
						nextNode.key=EMPTY;
						array.set(index,nextNode);
						if(i+1<keys.length){
							if(!(keys[i+1].type.equals("String"))){
								nextNode.element=new JsonQueryArray();
							}else{
								nextNode.element=new JsonQueryObject();
							}
						}
					}
					node=nextNode;
				}else{
					JsonQueryNode nextNode = (JsonQueryNode) node._(keys[i].str());
					if(!nextNode.exists()){
						nextNode.key=keys[i].str();
						if(!(node.element instanceof JsonQueryObject)) {
							if(node.element==null){
								node.element = new JsonQueryObject();
							}else{
								return new JsonQueryNode(null,null);
							}
						}
						((JsonQueryObject) node.element).put(keys[i].str(),nextNode);
						if(i+1<keys.length){
							if(!(keys[i+1].type.equals("String"))){
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
		return new JsonQueryNode(null,null);
	}

	// JQL - JSON QUERY LANGUAGE


	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JSQLResultSet<JsonQuery> jsql(String queryString){
		return (JSQLResultSet)JSQLEngine.getJQL().execute(this,queryString);

	}

	// Get values

	/* (non-Javadoc)
	 * @see JsonQuery#val()
	 */
	@Override
	public Object val() {
		return element;
	}

	@Override
	public Object getElement() {
		return element;
	}

	/* (non-Javadoc)
	 * @see JsonQuery#str()
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see JsonQuery#bool()
	 */
	@Override
	public boolean bool() {
		if(element!=null){
			return (Boolean) element;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see JsonQuery#i()
	 */
	@Override
	public int i() {
		if(element!=null){
			return ((Number)element).intValue();
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see JsonQuery#l()
	 */
	@Override
	public long l() {
		if(element!=null){
			return ((Number)element).longValue();
		}
		return 0;
	}

	/* (non-Javadoc)
	 * @see JsonQuery#d()
	 */
	@Override
	public double d() {
		if(element!=null){
			return ((Number)element).doubleValue();
		}
		return 0;
	}


	/* (non-Javadoc)
	 * @see JsonQuery#val(java.lang.String)
	 */
	@Override
	public Object val(String path) {
		return this.get(path).val();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#val(int)
	 */
	@Override
	public Object val(int key) {
		return this.get(String.valueOf(key)).val();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#str(java.lang.String)
	 */
	@Override
	public String str(String path) {
		return this.get(path).str();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#str(int)
	 */
	@Override
	public String str(int key) {
		return this.get(String.valueOf(key)).str();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#bool(java.lang.String)
	 */
	@Override
	public boolean bool(String path){
		return this.get(path).bool();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#bool(int)
	 */
	@Override
	public boolean bool(int key){
		return this.get(String.valueOf(key)).bool();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#i(java.lang.String)
	 */
	@Override
	public int i(String path){
		return this.get(path).i();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#i(int)
	 */
	@Override
	public int i(int key){
		return this.get(String.valueOf(key)).i();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#l(java.lang.String)
	 */
	@Override
	public long l(String path){
		return this.get(path).l();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#l(int)
	 */
	@Override
	public long l(int key){
		return this.get(String.valueOf(key)).l();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#d(java.lang.String)
	 */
	@Override
	public double d(String path){
		return this.get(path).d();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#d(int)
	 */
	@Override
	public double d(int key){
		return this.get(String.valueOf(key)).d();
	}

	// Set value
	
	public void setElement(String value){
		set(JSQLUtil.formatElementFromString(value));
	}

	/* (non-Javadoc)
	 * @see JsonQuery#set(java.lang.Object)
	 */
	@Override
	public JsonQuery set(Object value){
		if(value instanceof JsonQueryNode){
			value = ((JsonQueryNode) value).element;
		}
		element = formatValue(value);
		return this;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#set(java.lang.String java.lang.Object)
	 */
	@Override
	public JsonQuery set(String path, Object value){
		JsonQueryNode node = this.get(path);
		node.set(value);
		return this;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#set(int, java.lang.Object)
	 */
	@Override
	public JsonQuery set(int key, Object value){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				((JsonQueryArray)element).jsonQueryArraySet(key,formatValue(value));
			}
		}
		return this;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#jset(java.lang.String)
	 */
	@Override
	public JsonQuery jset(String value){
		if(value==EMPTY)value="\"\"";
		String json = "{\"obj\":"+value+"}";
		try{
			JsonQueryNode node = getGson().fromJson(json,JsonQueryNode.class);
			element = node._("obj").element;
		}catch(Throwable e){
			handleException(e);
		}
		return this;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#jset(java.lang.String, java.lang.String)
	 */
	@Override
	public JsonQuery jset(String path, String value){
		JsonQueryNode node = this.get(path);
		node.jset(value);
		return this;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#jset(int, java.lang.String)
	 */
	@Override
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
	
	/* (non-Javadoc)
	 * @see JsonQuery#put(java.lang.String, java.lang.Object)
	 */
	@Override
	public JsonQuery put(String path, Object value){
		if(element == null)element = new JsonQueryObject();
		if(element instanceof JsonQueryObject){
			JsonQuery node = this.node(path);
			node.set(value);
		}
		return this;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#jput(java.lang.String, java.lang.String)
	 */
	@Override
	public JsonQuery jput(String path, String value){
		if(element == null)element = new JsonQueryObject();
		if(element instanceof JsonQueryObject){
			JsonQuery node = this.node(path);
			node.jset(value);
		}
		return this;
	}
	
	// add an array element
	/* (non-Javadoc)
	 * @see JsonQuery#add(java.lang.Object)
	 */
	@Override
	public JsonQuery add(Object value){
		if(element == null)element = new JsonQueryArray();
		if(element instanceof JsonQueryArray){
			if(value instanceof JsonQueryNode){
				((JsonQueryArray)element).add((JsonQueryNode) value);
			}else{
				((JsonQueryArray)element).add(new JsonQueryNode(formatValue(value)));
			}
		}
		return this;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#add(int, java.lang.Object)
	 */
	@Override
	public JsonQuery add(int i,Object value){
		if(element == null && i==0)element = new JsonQueryArray();
		if(element instanceof JsonQueryArray){
			try{
				if(i<=((JsonQueryArray)element).size()){
					if(value instanceof JsonQueryNode){
						((JsonQueryArray)element).add(i,(JsonQueryNode) value);
					}else{
						((JsonQueryArray)element).add(i,new JsonQueryNode(formatValue(value)));
					}
				}
			}catch(Throwable e){
				handleException(e);
			}
		}
		return this;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#jadd(java.lang.String)
	 */
	@Override
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
	
	/* (non-Javadoc)
	 * @see JsonQuery#jadd(int, java.lang.String)
	 */
	@Override
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
	
	/* (non-Javadoc)
	 * @see JsonQuery#clear()
	 */
	@Override
	public JsonQuery clear(){
		this.element = null;
		return this;
	}


	/* (non-Javadoc)
	 * @see JsonQuery#remove(java.lang.String)
	 */
	@Override
	public JsonQuery remove(String path){
		JsonQueryToken<?>[] keys = getKeys(path);
		JsonQueryNode node = path(this,keys,true);
		
		if(!node.exists()){
			return this;
		}
		JsonQueryNode anode = node.antenode;
		if(anode.element instanceof JsonQueryObject){
			if(((JsonQueryObject)anode.element).containsValue(node)){
				((JsonQueryObject)anode.element).remove(node);
			}
		}
		if(anode.element instanceof JsonQueryArray){
			if(((JsonQueryArray)anode.element).contains(node)){
				((JsonQueryArray)anode.element).remove(node);
			}
		}
		return this;
	}

	/* (non-Javadoc)
	 * @see JsonQuery#remove(int)
	 */
	@Override
	public JsonQuery remove(int key){
		if(element instanceof JsonQueryArray){
			((JsonQueryArray)element).remove(key);
		}
		return this;
	}

	// Iterating

	/* (non-Javadoc)
	 * @see JsonQuery#each()
	 */
	@Override
	public JsonQueryArray each(){
		if(element instanceof JsonQueryArray){
			return (JsonQueryArray) element;
		}
		if(element instanceof JsonQueryObject){
			JsonQueryArray  array = new JsonQueryArray();
			for (Entry<String, JsonQueryNode> entry : ((JsonQueryObject)element).entrySet()) {
				String key = entry.getKey();
				JsonQueryNode node = (JsonQueryNode)entry.getValue();
				node.key=key;
				array.add(node);
			}
			return array;
		}
		return new JsonQueryArray();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#each(java.lang.String)
	 */
	@Override
	public JsonQueryArray each(String path){
		JsonQueryNode node = this.get(path);
		return node.each();
	}

	@SuppressWarnings("unchecked")
	@Override
	public ArrayList<Object> getChildNodes() {
		if(element instanceof JsonQueryArray){
			return (ArrayList<Object>) element;
		}
		if(element instanceof JsonQueryObject){
			ArrayList<Object>  array = new ArrayList<Object>();
			for (Entry<String, JsonQueryNode> entry : ((JsonQueryObject)element).entrySet()) {
				String key = entry.getKey();
				JsonQueryNode node = (JsonQueryNode)entry.getValue();
				node.key=key;
				array.add(node);
			}
			return array;
		}
		return new ArrayList<Object>();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#iterator()
	 */
	@Override
	public Iterator<Object> iterator() {
		// TODO this is a stub
		return null;
	}

	/* (non-Javadoc)
	 * @see JsonQuery#hasNext()
	 */
	@Override
	public boolean hasNext(){
		// TODO
		if(element instanceof JsonQueryArray){
			//Iterator<Object> it = ((JsonQueryArrayList)node).iterator();
			//if()
		}
		return false;
	}

	// Type Determination

	/* (non-Javadoc)
	 * @see JsonQuery#exists()
	 */
	@Override
	public boolean exists(){
		if(key==null)return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see JsonQuery#exists(java.lang.String)
	 */
	@Override
	public boolean exists(String path){
		JsonQueryNode node = this.get(path);
		return node.exists();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#type()
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see JsonQuery#type(java.lang.String)
	 */
	@Override
	public String type(String path){
		JsonQueryNode node = this.get(path);
		return node.type();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#isLeaf()
	 */
	@Override
	public boolean isLeaf(){
		if(element instanceof JsonQueryObject || element instanceof JsonQueryArray)
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see JsonQuery#isLeaf(java.lang.String)
	 */
	@Override
	public boolean isLeaf(String path){
		JsonQueryNode node = this.get(path);
		if(!node.exists()){
			return false;
		}
		return node.isLeaf();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#isObject()
	 */
	@Override
	public boolean isObject(){
		if(element instanceof JsonQueryObject)
			return true;
		return false;
	}

	/* (non-Javadoc)
	 * @see JsonQuery#isObject(java.lang.String)
	 */
	@Override
	public boolean isObject(String path){
		JsonQueryNode node = this.get(path);
		return node.isObject();
	}

	/* (non-Javadoc)
	 * @see JsonQuery#isArray()
	 */
	@Override
	public boolean isArray(){
		if(element instanceof JsonQueryArray)
			return true;
		return false;
	}

	/* (non-Javadoc)
	 * @see JsonQuery#isArray(java.lang.String)
	 */
	@Override
	public boolean isArray(String path){
		JsonQueryNode node = this.get(path);
		return node.isArray();
	}

	private  Object formatValue(Object value){
		if(value instanceof Number){
			value = new JsonQueryNumber(new LazilyParsedNumber(value.toString()));
		}
		return  value;
	}

	// Exception Handling

	private void handleException(Throwable e){
		System.out.println("error");
		e.printStackTrace();
	}

	// Write to JSON string

	/* (non-Javadoc)
	 * @see JsonQuery#toJson()
	 */
	@Override
	public String toJson(){
		try{
			return (String) getGson().toJson(this.element);
		}catch(Throwable e){
			handleException(e);
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see JsonQuery#toJson()
	 */
	@Override
	public String toJson(String path){
		JsonQueryNode node = this.get(path);
		if(!node.exists()){
			return null;
		}
		return node.toJson();
	}

	private void out(Object o){
		System.out.println(o);
	}

	private void listIt(List list,String start,String loopStr){
		out(start);
		for (Object elem : list) {
			out(loopStr+ " "+ elem.toString());
		}
		out("---------------------------------");
		out("");
	}

}
