package sb2.exceptions;

/**
 * Created by solvie on 2016-12-24.
 */
public class BadConfigXlsxException extends Exception {

    public BadConfigXlsxException(String message){
        super (message);
    }

    public BadConfigXlsxException(String message, Throwable cause){
        super (message, cause);
    }
}
