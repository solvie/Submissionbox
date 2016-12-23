package sb2.modelobjects;

import lombok.Data;

/**
 * Created by solvie on 2016-12-11.
 */

@Data
public class SbAssignment {
    private String originalFilename;
    private SbUser user;

    SbAssignment(){}

    SbAssignment(String filename, SbUser user ){
        this.originalFilename = filename;
        this.user = user;
    }

}
