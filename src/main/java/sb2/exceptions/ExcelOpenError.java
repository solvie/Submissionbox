package sb2.exceptions;
/**
 * Created by solvie_lee on 10/19/2016.
 */
public class ExcelOpenError extends Exception {

    public ExcelOpenError(String message){
        super (message);
    }

    public ExcelOpenError (String message, Throwable cause){
        super (message, cause);
    }
}
