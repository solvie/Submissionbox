
public class calc {
	
	//Function to check if the string is an operator or not
	public static boolean is_operator(String s)
	{
		boolean answer;
		
		switch(s)
		{
		//List of operators
		case "^":
		case "/":
		case "x":
		case "+":
		case "-": answer = true;
			break;
		default : answer = false;
		}
		return answer;
	}

	//Function to check the precedence of operators
	public static int precedence(String s)
	{
		int answer;
		switch(s)
		{
		case "+":
		case "-": answer = 1;  //Lowest precedence
			break;
		case "/":
		case "x": answer = 2;
			break;
		case "^": answer = 3;  //Highest precedence
			break;
		default : answer = 0;
		}
		return answer;
	}	

	//Function to evaluate an expression
	public static String evaluate(String op1, String operator, String op2)
	{
		float answer = 0;  //initialization
		switch(operator)
		{
		case "+" : answer = Float.valueOf(op1) + Float.valueOf(op2);  //add
			break;
		case "-" : answer = Float.valueOf(op1) - Float.valueOf(op2);  //subtract
			break;
		case "x" : answer = Float.valueOf(op1) * Float.valueOf(op2);  //multiply
			break;
		case "/" : answer = Float.valueOf(op1) / Float.valueOf(op2);  //divide
			break;
		case "^" : answer = 1;   //Because multiplying by 0 is 0
					for(int i=0 ; i<Integer.valueOf(op2) ; i++)   //op1 raised to the power of op2
						answer *= Float.valueOf(op1);
			break;
		}
		return String.valueOf(answer);
	}
	
	//Main function
	public static void main(String[] args)
	{		
		//Defining and initializing the operator stack and the input and postfix queues.
		myStack operator = new myStack();
		myQueue input = new myQueue();
		myQueue postfix = new myQueue();
		
		//Copying data from command line arguments to input queue
		for(String s : args)
		{
			input.enqueue(s);
			System.out.print(s + " ");  //prints the input statement
		}
		
		//Go through each element of the input queue one by one
		while(!input.isEmpty())
		{
			//Dequeuing a token from input queue
			String a = input.dequeue();
			
			//If it is an operand
			if(!is_operator(a))
				postfix.enqueue(a);  //add to postfix queue
			//If operator
			else
			{
				//While top operator on stack has higher or equal precedence and is left associative
				while(!operator.isEmpty() && precedence(operator.top.element)>=precedence(a))
				{
					//Pop operators and add to postfix queue
					String s = operator.pop();
					postfix.enqueue(s);
				}
				operator.push(a);  //Then push new operator on stack
			}
		}
	
		//Pop all remaining operators in stack and add to the postfix queue
		while (!operator.isEmpty())
		{
		    String f = operator.pop();
		    postfix.enqueue(f);
		}
		
		//Initializing stack
		myStack stack = new myStack();
		
		//going through the postfix expression one by one from left to right
		while(!postfix.isEmpty())
		{
			String p = postfix.dequeue();
			if(!is_operator(p))  //if operand, add to stack
				stack.push(p);
			else  //if operator
			{
				String op2 = stack.pop();  //operand 2
				String op1 = stack.pop();  //operand 1
				String retVal = evaluate(op1, p, op2);  //1st pop->2nd operand, 2nd pop->1st operand
				stack.push(retVal);  //push the evaluated result back to the stack
			}
		}
		
		System.out.println(" = " + stack.pop());  //print out the result

	}
}
