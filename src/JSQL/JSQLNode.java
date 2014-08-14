package src.JSQL;

import java.util.ArrayList;

public interface JSQLNode {
	
	JSQLNode createNewNode(Object node, String key);
	
	Object getElement();

	String getKey();
	
	void setKey(String key);
	
	JSQLNode getAntenode();
	
	void setAntenode(JSQLNode antenode);
	
	JSQLNode getNextNode(String key);
	
	JSQLNode getNextNode(int key);
	
	ArrayList<Object> getChildNodes();

}
