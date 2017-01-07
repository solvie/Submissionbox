package sb2.util;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import sb2.modelobjects.SbUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by solvie on 2016-11-20.
 */
public class DBReadWriter {

    private JdbcTemplate jdbcTemplate;
    private static final String classlisttable =  "classlist";

    public DBReadWriter(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addToClassList(SbUser user)throws DataAccessException, SQLException{
        final String INSERT_SQL = "insert into "+ classlisttable +"(id, username, password) values(?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(
                new PreparedStatementCreator() {
                    public PreparedStatement createPreparedStatement(Connection connection) throws SQLException {
                        PreparedStatement ps = connection.prepareStatement(INSERT_SQL, new String[] {"id"});
                        ps.setString(1, user.getId());
                        ps.setString(2, user.getUsername());
                        ps.setString(3, user.getPassword());
                        return ps;}}, keyHolder);
    }

    public SbUser retrieveSbUser(String id) throws DataAccessException, NullPointerException{
        String query = String.format("SELECT * FROM %s WHERE id=%s ",classlisttable, id);
        List<SbUser> r = this.jdbcTemplate.query(
                query,
                (rs, rowNum) -> {
                    return new SbUser(
                            rs.getString("id"),
                            rs.getString("username"),
                            rs.getString("password"));
                });
        if (r.size()==1) return r.get(0);
        else return null;//TODO: throw error rather than return something.
    }

}
