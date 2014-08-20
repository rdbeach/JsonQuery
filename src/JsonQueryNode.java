package src;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import src.JSQL.JSQLEngine;
import src.JSQL.JSQLNode;
import src.JSQL.JSQLResultSet;
import src.JSQL.JSQLUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LazilyParsedNumber;


public class JsonQueryNode extends JsonQuery implements JSQLNode{
	
	private static final String PATH_DELIMETER = ".";
	
	private static final String NOT_BACKSLASH = "(?<!\\\\)";
	private static final String PATH_DELIMETER_REGEX = "[.]";
	private static final String DOUBLE_BACKSLASH = "\\";
	
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
	
	// Single node tree traversal operator
	
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
	
	// Multinode: tree traversal operator
	
	/* (non-Javadoc)
	 * @see JsonQuery#get(java.lang.String)
	 */
	@Override
	public JsonQueryNode get(String path) {
		String[] keys = getKeys(path);
		return path(this,keys,false);
	}
	
	private String[] getKeys(String path){
		String[] keys = path.
				split(NOT_BACKSLASH+PATH_DELIMETER_REGEX);
		int count=0;
		for(String key:keys){
			keys[count]=key.
					replace(DOUBLE_BACKSLASH+PATH_DELIMETER, PATH_DELIMETER);
			count++;
		}
		return keys;
	}
	
	private JsonQueryNode path(JsonQueryNode node, Object[] keys, boolean addTreeInfo){
		try{
			int i=0;
			for(i=0;i<keys.length;i++){
				JsonQueryNode nextNode=null;
				if(JSQLUtil.isInteger((String)keys[i])){
					nextNode = (JsonQueryNode) node._(Integer.parseInt((String)keys[i]));
				}else{
					nextNode = (JsonQueryNode) node._((String)keys[i]);
				}
				if(nextNode!=null&&addTreeInfo){
						nextNode.key=(String)keys[i];
					nextNode.antenode=node;
				}
				node=nextNode;
				if(node==null)break;
			}
			if(node!=null)return node;
		}catch(Throwable e){
			handleException(e);
		}
		return new JsonQueryNode(null,null);
	}
	
	// Multinode: get or create a path
	
	/* (non-Javadoc)
	 * @see JsonQuery#node(java.lang.String)
	 */
	@Override
	public JsonQuery node(String path) {
		String[] keys = getKeys(path);
		JsonQueryNode node = this;
		try{
			int i=0;
			for(i=0;i<keys.length;i++){
				if(JSQLUtil.isInteger(keys[i])){
					int index = Integer.parseInt(keys[i]);
					JsonQueryNode nextNode = (JsonQueryNode) node._(index);
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
							if(JSQLUtil.isInteger(keys[i+1])){
								nextNode.element=new JsonQueryArray();
							}else{
								nextNode.element=new JsonQueryObject();
							}
						}
					}
					node=nextNode;
				}else{
					JsonQueryNode nextNode = (JsonQueryNode) node._(keys[i]);
					if(!nextNode.exists()){
						nextNode.key=keys[i];
						if(!(node.element instanceof JsonQueryObject)) {
							if(node.element==null){
								node.element = new JsonQueryObject();
							}else{
								return new JsonQueryNode(null,null);
							}
						}
						((JsonQueryObject) node.element).put(keys[i],nextNode);
						if(i+1<keys.length){
							if(JSQLUtil.isInteger(keys[i+1])){
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
	
	// Multinode gets
	
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
	
	// Gets for the single node traversal queries
	
	/* (non-Javadoc)
	 * @see JsonQuery#val(java.lang.String)
	 */
	@Override
	public Object val(String key) {
		if(element instanceof JsonQueryObject){
			JsonQueryNode node = (JsonQueryNode)((JsonQueryObject)element).get(key);
			if(node!=null) return node.element;
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#val(int)
	 */
	@Override
	public Object val(int key) {
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQueryNode node = (JsonQueryNode)((JsonQueryArray)element).get(key);
				if(node!=null) return node.element;
			}
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#str(java.lang.String)
	 */
	@Override
	public String str(String key) {
		if(element instanceof JsonQueryObject){
			JsonQueryNode node = (JsonQueryNode)((JsonQueryObject)element).get(key);
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
	
	/* (non-Javadoc)
	 * @see JsonQuery#str(int)
	 */
	@Override
	public String str(int key) {
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQueryNode node = (JsonQueryNode)((JsonQueryArray)element).get(key);
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
	
	/* (non-Javadoc)
	 * @see JsonQuery#bool(java.lang.String)
	 */
	@Override
	public boolean bool(String key){
		if(element instanceof JsonQueryObject){
			JsonQueryNode node = (JsonQueryNode)((JsonQueryObject)element).get(key);
			if(node!=null) return (Boolean)node.element;
			
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#bool(int)
	 */
	@Override
	public boolean bool(int key){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQueryNode node = (JsonQueryNode)((JsonQueryArray)element).get(key);
				if(node!=null) return (Boolean)node.element;
			}
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#i(java.lang.String)
	 */
	@Override
	public int i(String key){
		if(element instanceof JsonQueryObject){
			JsonQueryNode node = (JsonQueryNode)((JsonQueryObject)element).get(key);
			if(node!=null&&node.element!=null) return ((Number)(node.element)).intValue();
		}
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#i(int)
	 */
	@Override
	public int i(int key){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQueryNode node = (JsonQueryNode)((JsonQueryArray)element).get(key);
				if(node!=null&&node.element!=null) return ((Number)(node.element)).intValue();
			}
		}
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#l(java.lang.String)
	 */
	@Override
	public long l(String key){
		if(element instanceof JsonQueryObject){
				JsonQueryNode node = (JsonQueryNode)((JsonQueryObject)element).get(key);
				if(node!=null&&node.element!=null) return ((Number)(node.element)).longValue();
		}
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#l(int)
	 */
	@Override
	public long l(int key){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
					JsonQueryNode node = (JsonQueryNode)((JsonQueryArray)element).get(key);
					if(node!=null&&node.element!=null) return ((Number)(node.element)).longValue();
			}
		}
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#d(java.lang.String)
	 */
	@Override
	public double d(String key){
		if(element instanceof JsonQueryObject){
			JsonQueryNode node = (JsonQueryNode)((JsonQueryObject)element).get(key);
			if(node!=null&&node.element!=null) return ((Number)(node.element)).doubleValue();
		}
		return 0;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#d(int)
	 */
	@Override
	public double d(int key){
		if(element instanceof JsonQueryArray){
			if(key<((JsonQueryArray)element).size()){
				JsonQueryNode node = (JsonQueryNode)((JsonQueryArray)element).get(key);
				if(node!=null&&node.element!=null) return ((Number)(node.element)).doubleValue();
			}
		}
		return 0;
	}
	
	// Multinode sets
	
	
	/* (non-Javadoc)
	 * @see JsonQuery#set(java.lang.Object)
	 */
	@Override
	public JsonQuery set(Object value){
		if(value instanceof JsonQueryNode){
			value = this;
		}else{
			element = formatValue(value);
		}
		return this;
	}
	
	public void setElement(String value){
		set(JSQLUtil.formatElementFromString(value));
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
			element =  node._("obj").element;
		}catch(Throwable e){
			handleException(e);
		}
		return this;
	}
	
	// Sets for the single node traversal
	
	/* (non-Javadoc)
	 * @see JsonQuery#set(java.lang.String, java.lang.Object)
	 */
	@Override
	public JsonQuery set(String key, Object value){
		if(element == null)element = new JsonQueryObject();
		if(element instanceof JsonQueryObject){
			((JsonQueryObject)element).set(key,formatValue(value));
		}
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
	 * @see JsonQuery#jset(java.lang.String, java.lang.String)
	 */
	@Override
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
	
	
	
	// adds for multinode and single node traversal
	
	
	
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
	 * @see JsonQuery#toNull()
	 */
	@Override
	public JsonQuery toNull(){
		this.element=null;
		return this;
	}
	
	/* (non-Javadoc)
	 * @see JsonQuery#remove(java.lang.String)
	 */
	@Override
	public JsonQuery remove(String key){
		if(element instanceof JsonQueryObject){
			((JsonQueryObject)element).remove(key);
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
	 * @see JsonQuery#isLeaf()
	 */
	@Override
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
	 * @see JsonQuery#isArray()
	 */
	@Override
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
	
	private void out(Object o){
		System.out.println(o);
	}
	
}
