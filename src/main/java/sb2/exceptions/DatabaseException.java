package sb2.exceptions;

/**
 * Created by solvie on 2017-01-06.
 */
public class DatabaseException extends Exception {

    public DatabaseException(String message){
        super (message);
    }

    public DatabaseException(String message, Throwable cause){
        super (message, cause);
    }
}
