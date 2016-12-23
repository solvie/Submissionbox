package sb2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import sb2.modelobjects.Message;
import sb2.modelobjects.SbUser;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

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
        List<SbUser> classlist = model.init();
        initUserList(classlist);
        initFileSystem();
    }

    private void initFileSystem(){

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

    //TODO: extract assignment information from file name)
    @RequestMapping(value="/upload", method=RequestMethod.POST)
    public @ResponseBody String handleFileUpload(
            @RequestParam("file") MultipartFile file){
        if (!file.isEmpty()) {
            try {
                String name = file.getOriginalFilename();
                byte[] bytes = file.getBytes();
                BufferedOutputStream stream =
                        new BufferedOutputStream(new FileOutputStream(new File(name)));
                stream.write(bytes);
                stream.close();
                return "You successfully uploaded " + name + " into " + name + "-uploaded !";
            } catch (Exception e) {
                return "Failed to upload the file => " + e.getMessage();
            }
        } else {
            return "Failed to upload file because it was empty.";
        }
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
