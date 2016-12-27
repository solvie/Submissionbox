//Definition of Stack
public class myStack {

	node top;  //Top entry in stack
	
	//Function to push new entry on top of stack
	public void push(String elem)
	{
		node new_node = new node();  //Make a new node
		new_node.next = top;
		new_node.element = elem;
		top = new_node;  //Top is the new node
	}
	
	//Function to remove top entry from stack
	public String pop()
	{
		if(top!=null)  //If stack is not empty
		{
			node pelem = top;
			top = pelem.next;  //top is the next entry
			return pelem.element;  //return the removed data entry
		}
		else
			return null;
	}
	
	//Function to check if stack is empty
	public boolean isEmpty()
	{
		return (top==null);
	}
}