package sb2.util;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import sb2.modelobjects.Message;
import sb2.modelobjects.SbUser;


import java.sql.SQLException;
import java.util.List;

/**
 * Created by solvie on 2016-11-20.
 */
public class DBReadWriter {

    private JdbcTemplate jdbcTemplate;
    private static final String classlisttable =  "classlist";
    private static final String assignmentstable =  "assignments";


    public DBReadWriter(JdbcTemplate jdbcTemplate){
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addToClassList(SbUser user)throws DataAccessException, SQLException{
        String query = "INSERT INTO " + classlisttable+ "(username, fullname, password) VALUES (?, ?, ?)";
        jdbcTemplate.update(query, user.getUsername(), user.getFullname(), user.getPassword());
    }

    public void updatePassword(SbUser user)throws DataAccessException, SQLException{
        String query = "UPDATE "+ classlisttable +" SET "+ "password=? " + "WHERE username=?";
        jdbcTemplate.update(query, user.getPassword(), user.getUsername());
    }

    public void addScore(String user, int asstno, Message results)throws DataAccessException, SQLException{
        String query = String.format("INSERT INTO %s (username, assignment, score, failures) VALUES (?,?,?,?) " +
                "ON DUPLICATE KEY UPDATE score=?, failures=?", assignmentstable);
        jdbcTemplate.update(query, user, asstno, results.getValue(), results.getDetails(), results.getValue(), results.getDetails());
    }

    public Message queryPreviousResults(SbUser user, int asstno) throws DataAccessException, SQLException{
        String query = String.format("SELECT score, failures FROM %s WHERE username=\"%s\" AND assignment=%d ", assignmentstable, user.getUsername(), asstno);
        List<Message> r = this.jdbcTemplate.query(
                query,
                (rs, rowNum) -> {
                    return new Message(
                            Message.Mtype.SUCCESS,
                            rs.getString("score"),
                            rs.getString("failures"));
                });
        if (r.size()==0) return new Message(Message.Mtype.DNE); //DNE is does not exist. First score save.
        if (r.size()==1) return r.get(0);
        return new Message(Message.Mtype.ERROR, "ERROR: than one result came back");
    }

    public SbUser retrieveSbUser(String username) throws DataAccessException, NullPointerException{
        String query = String.format("SELECT * FROM %s WHERE username=%s ",classlisttable, username);
        List<SbUser> r = this.jdbcTemplate.query(
                query,
                (rs, rowNum) -> {
                    return new SbUser(
                            rs.getString("fullname"),
                            rs.getString("username"),
                            rs.getString("password")
                            );
                });
        if (r.size()==1) return r.get(0);
        else return null;//TODO: throw error rather than return something.
    }

}
