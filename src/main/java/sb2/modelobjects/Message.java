package sb2.modelobjects;
import lombok.Data;

/**
 * Created by solvie_lee on 9/29/2016.
 *
 * Simple class to shuttle messages to be printed - usually for displaying error messages.
 **
 */

@Data
public class Message{
    private Mtype messagetype; //Enum saying what kind of message this is
    private String value;
    private String details;

    public Message(){}

    public Message (Mtype m){
        this.messagetype = m;
    }

    public Message (Mtype m, String v){
        this.messagetype = m;
        this.value = v;
    }

    public Message (Mtype m, String v, String d){
        this.messagetype = m;
        this.value = v;
        this.details = d;
    }

    public enum Mtype{
        ERROR, TIMEOUT, DNE, WARNING, SUCCESS, FAIL
    }

}
