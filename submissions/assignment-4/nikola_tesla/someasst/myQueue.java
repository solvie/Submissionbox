//Definition of Queue
public class myQueue {

	node rear;  //Last entry in queue
	node front;  //First entry in queue
	
	//Function to add entry to queue (Entry added from behind)
	public void enqueue(String elem)
	{
		node new_node = new node();  //Make a new node
		new_node.element = elem;  
		new_node.next = null;  //at rear
		
		if(front==null)  //if empty, front is also the new node
			front = new_node;		
		else  //if not empty, rear points to the new node
			rear.next = new_node;
			
		rear = new_node;  //rear is the new node
	}
	
	//Function to remove first entry from queue
	public String dequeue()
	{
		if(front!=null)  //if not empty
		{
			node pelem = front;
			front = front.next;  //front = 2nd entry (Since first entry is removed)
			if(pelem==rear) //if it was the last entry
				rear=null;
			return pelem.element;  //return the removed data entry
		}
		else
			return null;		
	}
	
	//Function to check if Queue is empty
	public boolean isEmpty()
	{
		return (front == null);
	}
}
