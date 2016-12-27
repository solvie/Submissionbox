package sb2.exceptions;

/**
 * Created by solvie on 2016-12-24.
 */
public class FileSystemException extends Exception {
    public FileSystemException(String message){
        super (message);
    }

    public FileSystemException (String message, Throwable cause){
        super (message, cause);
    }

}
