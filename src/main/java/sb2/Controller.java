package sb2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sb2.exceptions.FileSystemException;
import sb2.modelobjects.Message;
import sb2.modelobjects.SbAssignment;
import sb2.modelobjects.SbUser;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.System.exit;
import static sb2.modelobjects.Message.Mtype.ERROR;

/**
 * Created by solvie on 2016-11-20.
 */


@RestController
@Component
public class Controller {

    private final Model model;
    private final InMemoryUserDetailsManager inMemoryUserDetailsManager;

    @Autowired
    public Controller(@Qualifier("jdbcMaster") JdbcTemplate jdbcTemplate, InMemoryUserDetailsManager inMemoryUserDetailsManager)
        throws Exception{
        this.model = new Model(jdbcTemplate);
        this.inMemoryUserDetailsManager = inMemoryUserDetailsManager;
        init();
    }

    private void init() throws Exception{
        System.out.println(">> Initializing classlist on an empty database? (y/n) ");
        Scanner sc = new Scanner(System.in);
        String resp = sc.nextLine();
        if (!resp.equalsIgnoreCase("y")) if (!resp.equalsIgnoreCase("n")) { System.out.println("Invalid."); exit(0);}
        List<SbUser> classlist = model.init();
        initUserList(classlist, resp.equalsIgnoreCase("y"));
        model.initFileSystem();
    }

    private void initUserList(List<SbUser> classlist, boolean initclasslist) throws Exception{
        if (classlist==null) throw new Exception("Classlist was empty");
        for (SbUser user: classlist){
            if (initclasslist) {//assumes that db is empty
                if (!model.addUser(user).equals("SUCCESS"))
                    System.out.println("ERROR!");
                else System.out.println(user.getFullname());
            }
            inMemoryUserDetailsManager.createUser(new User(user.getUsername(), user.getPassword(), new ArrayList<GrantedAuthority>()));
        }
    }

    @RequestMapping("/")
    public String home(){
        return "home";
    }

    @RequestMapping(value ="/login", method = RequestMethod.POST)
    public String login() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String name = auth.getName();
        return "Hello, "+ name;
    }

    @RequestMapping(value ="/logout", method = RequestMethod.POST)
    public void logout() {   }

    @RequestMapping("exists/{username}")
    public boolean userExists(@PathVariable("username") String username ) {
        return inMemoryUserDetailsManager.userExists(username);
    }

    @RequestMapping("add/{username}/{password}")
    public String add(@PathVariable("username") String username, @PathVariable("password") String password) {
        inMemoryUserDetailsManager.createUser(new User(username, password, new ArrayList<GrantedAuthority>()));
        return "added";
    }

    @RequestMapping(value="/upload", method=RequestMethod.POST)
    public @ResponseBody String handleFileUpload (
            @RequestParam("file") MultipartFile file, @RequestParam("mainfilename") String mainclassname,@RequestParam("asstnum") int asstnum) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        try {
            Message acceptFileMessage = this.model.acceptFile(file, username, asstnum);
        } catch (FileSystemException e){
            return "ERROR: "+ e.getMessage();
        }

        Message runResults = this.model.runTests(mainclassname, username, asstnum);
        return "PASSRATE: "+ runResults.getValue()+" FAILURES: "+runResults.getDetails();
    }

    @ExceptionHandler(value = Exception.class)
    public Message exceptionHandler(Exception e){
        return new Message(ERROR, e.getClass()+": " + e.getMessage() + e.getStackTrace().toString());
    }

    //------------------------- TEST METHODS ------------------------------------------------

    @RequestMapping("/testfullnametousername")
    public void splice(@RequestParam("fullname") String fullname ) {
        //SbUser user = new SbUser();
        //user.fullnameToUsername(fullname);
        // user.getUsername();

    }

    @RequestMapping("/testadd")
    public String testaddstudent(@RequestParam("username") String username,@RequestParam("password") String password, @RequestParam("fullname") String fullname) {
        SbUser user = new SbUser(fullname, username, password);
        return this.model.addUser(user);
    }

    @RequestMapping("/testSaveScore")
    public String testSaveScore(@RequestParam("asstnum") int asstnum, @RequestParam("mainclassname") String mainclassname){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        Message runResults = this.model.runTests(mainclassname, username, asstnum);
        System.out.println(runResults);
        //Todo: save this
        try {
            model.addScore(username, asstnum, runResults);
            return "Saved: "+ runResults.getValue()+" for assignment "+asstnum;
        } catch (SQLException e){
            return "Error trying to save score: "+ e.getMessage();
        }
    }

    @RequestMapping("/testGetAssignmentDetails")
    public List<SbAssignment> getAvailableAssignments(){
        return model.getAssignments();
    }


        @RequestMapping("/sayhay")
    public String sayHay(){
        return "hay";
    }

}
