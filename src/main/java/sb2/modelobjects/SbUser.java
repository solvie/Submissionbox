package sb2.modelobjects;

import lombok.Data;

/**
 * Created by solvie on 2016-11-20.
 */

@Data
public class SbUser {

    private String id;
    private String fullname;
    private String username;
    private String password;

    public SbUser(){}

    public SbUser(String fullname, String password){
        this.fullname = fullname;
        fullnameToUsername(fullname);
        this.password = password;
    }

    public SbUser(String id, String username, String password){
        this.id = id;
        this.username = username;
        this.password = password;
    }

    //public for test should be private later
    private void fullnameToUsername(String fullname){
        String[] spliced = fullname.split(",");
        this.username =spliced[1].toLowerCase().replace(" ", "") +"_"+spliced[0].toLowerCase();
    }


}
