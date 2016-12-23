package sb2.exceptions;

/**
 * Created by solvie_lee on 10/19/2016.
 */
public class ExcelReadError extends Exception {
    public ExcelReadError(String message){
        super (message);
    }

    public ExcelReadError (String message, Throwable cause){
        super (message, cause);
    }
}
