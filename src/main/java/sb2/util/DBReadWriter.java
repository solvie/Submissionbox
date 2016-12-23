package sb2.util;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Created by solvie on 2016-11-20.
 */
public class DBReadWriter {

    private JdbcTemplate jdbcTemplate;


    public DBReadWriter(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }


}
