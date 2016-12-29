package sb2.exceptions;

/**
 * Created by solvie on 2016-12-29.
 */
public class ShellException extends Exception{
    public ShellException(String message){
        super (message);
    }

    public ShellException (String message, Throwable cause){
        super (message, cause);
    }
}
