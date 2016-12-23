package sb2;

import org.springframework.jdbc.core.JdbcTemplate;
import sb2.exceptions.ExcelOpenError;
import sb2.exceptions.ExcelReadError;
import sb2.modelobjects.SbUser;
import sb2.util.DBReadWriter;
import sb2.util.ExcelReadWriter;

import java.util.List;

/**
 * Created by solvie on 2016-11-20.
 */
public class Model {
    private DBReadWriter dbReadWriter;
    private ExcelReadWriter excelReadWriter;
    private final String pathToResources = "./src/main/resources/";

    public Model(JdbcTemplate jdbcTemplate) {
        this.dbReadWriter = new DBReadWriter(jdbcTemplate);
        this.excelReadWriter = new ExcelReadWriter();
    }

    public List<SbUser> init()throws ExcelOpenError, ExcelReadError{
        initAssignmentConfig();
        return initClasslist();

    }

    private List<SbUser> initClasslist() throws ExcelOpenError, ExcelReadError{
        return excelReadWriter.attemptReadClasslist(pathToResources+"classlist.xlsx");
        //for (SbUser student : classlist)
          //  System.out.println(student);

    }

    private void initAssignmentConfig(){

    }




}
