package sb2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.parsing.FailFastProblemReporter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sb2.modelobjects.Message;
import sb2.modelobjects.SbUser;

import java.util.ArrayList;
import java.util.List;

import static sb2.modelobjects.Message.Mtype.ERROR;
import static sb2.modelobjects.Message.Mtype.FAIL;

/**
 * Created by solvie on 2016-11-20.
 */

//TODO: when running from the jar, we get a file not found (doesn't find resources)

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
        List<SbUser> classlist = model.init();
        initUserList(classlist);
        model.initFileSystem();
    }

    private void initUserList(List<SbUser> classlist) throws Exception{
        if (classlist==null) throw new Exception("Classlist was empty");
        for (SbUser user: classlist)
            inMemoryUserDetailsManager.createUser(new User(user.getUsername(), user.getPassword(), new ArrayList<GrantedAuthority>()));
    }

    @RequestMapping("/")
    public String home(){
        return "home";
    }

    @RequestMapping(value ="/login", method = RequestMethod.POST)
    public String login() {
        return "logged in";
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
            @RequestParam("file") MultipartFile file, @RequestParam("username") String username, @RequestParam("mainfilename") String mainclassname,@RequestParam("asstnum") int asstnum)
            throws Exception{
        System.out.println("uploading...");
        Message acceptFileMessage = this.model.acceptFile(file, username, asstnum);
        System.out.println("file was accepted");
        if (acceptFileMessage.getMessagetype()==FAIL) return acceptFileMessage.getValue();
        System.out.println("Running tests");
        String output = this.model.runTests(acceptFileMessage.getValue(), mainclassname, username, asstnum).getValue();
        System.out.println("Done running tests");
        return "message says "+ output;

        //return acceptFileMessage;
    }

    @ExceptionHandler(value = Exception.class)
    public Message exceptionHandler(Exception e){
        return new Message(ERROR, e.getClass()+": " + e.getMessage() + e.getStackTrace().toString());
    }

    //------------------------- TEST METHODS ------------------------------------------------

    @RequestMapping("testfullnametousername")
    public void splice(@RequestParam("fullname") String fullname ) {
        //SbUser user = new SbUser();
        //user.fullnameToUsername(fullname);
        // user.getUsername();

    }


    @RequestMapping("/sayhay")
    public String sayHay(){
        return "hay";
    }

}
