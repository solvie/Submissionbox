package sb2;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;
import sb2.exceptions.BadConfigXlsxException;
import sb2.exceptions.FileSystemException;
import sb2.exceptions.ShellException;
import sb2.modelobjects.Message;
import sb2.modelobjects.SbAssignment;
import sb2.modelobjects.SbUser;
import sb2.util.DBReadWriter;
import sb2.util.ExcelReadWriter;

import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


/**
 * Created by solvie on 2016-11-20.
 */

public class Model {
    private List<SbAssignment> assignments;
    private DBReadWriter dbReadWriter;
    private ExcelReadWriter excelReadWriter;
    private final String pathToResources = "./src/main/resources/";

    public Model(JdbcTemplate jdbcTemplate) {
        this.dbReadWriter = new DBReadWriter(jdbcTemplate);
        this.excelReadWriter = new ExcelReadWriter();
    }

    public List<SbUser> init()throws BadConfigXlsxException{
        return initClasslist();
    }

    public boolean initFileSystem() throws FileSystemException, BadConfigXlsxException, InterruptedException {
        //TODO: If the submissions folder exists, throw an error if it doesn't look like what the assignments-config says it should look like
        //If the submission folder doesn't exist, create it from scratch with the assignments-config information.
        this.assignments = readAssignmentConfig();

        try {
            java.lang.Runtime.getRuntime().exec("mkdir temp").waitFor();
            java.lang.Runtime.getRuntime().exec("mkdir submissions").waitFor();
            for (SbAssignment asst : this.assignments) {
                java.lang.Runtime.getRuntime().exec("mkdir ./submissions/assignment-" + asst.getAssignmentNum()).waitFor();
            }
            return false;
        } catch(IOException e){
            throw new FileSystemException("Error trying to make dirs and such");
        }
    }

    private List<SbAssignment> readAssignmentConfig() throws BadConfigXlsxException{
        String path = pathToResources + "assignments-config.xlsx";
        List<SbAssignment> assts = excelReadWriter.readAssignments(excelReadWriter.attemptGetSheet(path, "Sheet1"));
        for (SbAssignment asst : assts)
            if (asst.getTestFormat() == SbAssignment.TestFormat.OUTPUT)
                asst.setOutputTests(excelReadWriter.readAssignmentTests(excelReadWriter.attemptGetSheet(path, "A" + asst.getAssignmentNum())));
        return assts;
    }

    private List<SbUser> initClasslist() throws BadConfigXlsxException{
        return excelReadWriter.readClasslist(excelReadWriter.attemptGetSheet(pathToResources+"classlist.xlsx", "Sheet1"));
    }

    //HELPER FOR acceptFile
    //makes directory if it doesn't exist, if it does exist, delete everything inside the directory.
    //The path is assumed to be cd-able from where you are making it.
    private void makeDirectory(String path) throws FileSystemException{
        try { //Make a temp directory if there isn't already
            executeShellCommands(Arrays.asList(("mkdir " + path)));
        } catch (ShellException e){
            System.out.println("There was already the dir ");
            try { //If there was stuff in it, delete everything inside the directory.
                executeShellCommands(Arrays.asList(String.format("rm -r %s/*", path)));
            } catch (ShellException e2){
                System.out.println("Nothing to delete in there?:"+ e2.getMessage());
            }
        }
    }

    //TODO: need to handle if student already has directory/ if they already have code in it.

    public Message acceptFile(MultipartFile file, String username, int asstnum) throws FileSystemException{

        if (!file.isEmpty()) {
            String name = file.getOriginalFilename();
            String tempDir = "./temp/"+username;
            String submissionDir = "./submissions/assignment-"+asstnum+"/"+ username;
            String submissionDirUnitTestsC = "./submissions/assignment-"+asstnum+"/src/UnderTest/"+ username;
            String submissionDirUnitTestsJava = "./submissions/assignment-"+asstnum+"/"+ username;

            makeDirectory(tempDir);

            try {
                byte[] bytes = file.getBytes();
                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(tempDir + "/" + name)));
                stream.write(bytes);
                stream.close();
            } catch (IOException e) {
                throw new FileSystemException("Failed to upload the file because of IOException " + e.getMessage());
            }

            try{
                SbAssignment currAsst = SbAssignment.findAsst(this.assignments, asstnum);

                if (currAsst.getTestFormat()== SbAssignment.TestFormat.OUTPUT && currAsst.getLanguage()== SbAssignment.Language.JAVA) {
                    makeDirectory(submissionDir);
                    if (name.contains(".zip")) {
                        executeShellCommands(Arrays.asList(
                                String.format("unzip %s/%s -d %s/", tempDir, name, tempDir)));
                        executeShellCommands(Arrays.asList(
                                String.format(
                                "for entry in $(find %s/. -name *.java); " +
                                        " do if [[ $entry != *_MACOSX* ]]; then mv $entry %s; fi; "+
                                        " done;", tempDir, submissionDir)));
                    }
                    else
                        executeShellCommands(Arrays.asList(("mv " + tempDir + "/" + name + " " + submissionDir)));

                } else if (currAsst.getTestFormat()== SbAssignment.TestFormat.OUTPUT && currAsst.getLanguage()== SbAssignment.Language.C){
                    executeShellCommands(Arrays.asList(("mv " + tempDir + "/" + name + " " + submissionDir)));

                }else if (currAsst.getTestFormat()== SbAssignment.TestFormat.UNIT_TEST ){
                    //TODO: for now this takes care of only single c file submssions; assuming there's no makefile
                    if (currAsst.getLanguage()== SbAssignment.Language.C) {
                        makeDirectory(submissionDirUnitTestsC);
                        executeShellCommands(Arrays.asList(
                                String.format("mv %s %s", tempDir + "/" + name, submissionDirUnitTestsC),
                                String.format("cd %s", submissionDirUnitTestsC)
                        ));

                    } else if (currAsst.getLanguage()== SbAssignment.Language.JAVA){
                        makeDirectory(submissionDirUnitTestsJava);
                        if (name.contains(".zip")) {
                            executeShellCommands(Arrays.asList(String.format("unzip %s/%s -d %s/", tempDir, name, tempDir)));
                            executeShellCommands(Arrays.asList(String.format(
                                    "for entry in $(find %s/. -name *.java); " +
                                            " do if [[ $entry != *_MACOSX* ]]; then mv $entry %s; fi; "+
                                            " done;", tempDir, submissionDirUnitTestsJava),
                                    String.format("cd %s", submissionDirUnitTestsJava)
                            ));

                        }else {
                            executeShellCommands(Arrays.asList(String.format("mv %s %s", tempDir + "/" + name, submissionDirUnitTestsJava),
                                    String.format("cd %s", submissionDirUnitTestsJava)
                            ));
                        }
                    }

                }
            } catch (ShellException e){
                throw new FileSystemException("Failed to upload file: "+ e.getMessage());
            }
            return new Message(Message.Mtype.SUCCESS, name);
        } else {
            throw new FileSystemException("Failed to upload file because it was empty.");
        }
    }


    public Message runTests(String mainclassname, String username, int asstnum){
        SbAssignment assignment = SbAssignment.findAsst(this.assignments, asstnum);
        if (assignment.getTestFormat()== SbAssignment.TestFormat.OUTPUT){
            return runOutputTest(mainclassname, username, asstnum);
        } else if (assignment.getTestFormat()== SbAssignment.TestFormat.UNIT_TEST){
            if (assignment.getLanguage()==SbAssignment.Language.C)
                return runUnitTestsC(mainclassname, username, asstnum); //TODO: return results obvs. But when running, just do the run.sh script call.
            else if (assignment.getLanguage()== SbAssignment.Language.JAVA)
                return unitTestJava(mainclassname, username, asstnum);
        }
        return new Message(Message.Mtype.WARNING, "Something went wrong;");
    }

    private Message runOutputTest(String mainclassname,  String username, int asstnum){
        DualHashBidiMap<String, String> tests = SbAssignment.findAsst(this.assignments, asstnum).getOutputTests();
        //for each test, run them and see the expected value
        List<String> failedtests= new ArrayList<>();
        if (testCompiles(asstnum, username, mainclassname)) //if it compiles, run the tests.
            tests.forEach(
                    (k, v) -> {
                        String ans;
                        try {
                            ans = testOutput(asstnum, username, mainclassname, k, v);
                        } catch (ShellException e) {
                            ans = "ERROR." + e.getMessage();
                        }
                        if (ans.length()>0)
                            failedtests.add(ans);
                    }
            );
        String resultsFraction = tests.size()-failedtests.size()+"/"+tests.size();
        return new Message(Message.Mtype.SUCCESS, resultsFraction, failedtests.toString());
    }

    private Message runUnitTestsC(String mainclassname, String username, int asstnum){
        String result;
        //System.out.println("Unit testing with C");
        if (testCompiles(asstnum, username, mainclassname)) { //if it compiles, run the tests.
            List<String> runUnitTests = Arrays.asList(
                    String.format("sed -i 's/main\\(.*argv\\)/studentmain\\1/' ./submissions/assignment-%d/src/UnderTest/%s/%s", asstnum, username,mainclassname+".c"),
                    String.format("bash run.sh -f %s -n %d -u %s", mainclassname+".c", asstnum, username )
            );
            try {
                result = executeShellCommands(runUnitTests);
                String[]results=result.split("@");
                String passrate = results[1];
                String failures = results[3];
                return new Message(Message.Mtype.SUCCESS, passrate, failures);

            } catch (ShellException e){
                result= "ERROR WHILE COMPILING" + e.getMessage();
                return new Message(Message.Mtype.FAIL, result);
            }
        }
        return new Message(Message.Mtype.FAIL, "Doesn't compile!");

    }

    private Message unitTestJava(String mainclassname,  String username, int asstnum){
        String result;
        System.out.println("Unit testing with Java");

        if (testCompiles(asstnum, username, mainclassname)){

                List<String> runUnitTests = Arrays.asList(
                    String.format("cd ./submissions/assignment-%s/%s", asstnum, username),
                        String.format("sed -i 's/main\\(.*public static void main\\)/studentmain\\1/' %s", mainclassname+".java"),
                        String.format("javac %s.java && java %s", "TestRunner", "TestRunner")
                );
                try {
                    result = executeShellCommands(runUnitTests);
                    String[]results=result.split("@");
                    System.out.println("Splitting");
                    String passrate = results[1];
                    String failures = results[2];
                    return new Message(Message.Mtype.SUCCESS, passrate, failures);
                }
                catch (ShellException e){
                    result= "ERROR WHILE RUNNING" + e.getMessage();
                    return new Message(Message.Mtype.FAIL, result);
                }
        }
        return new Message(Message.Mtype.FAIL, "Doesn't compile!");

    }

    private boolean testCompiles(int asstnum, String username, String mainclassname){

        SbAssignment asst = SbAssignment.findAsst(this.assignments, asstnum);
        List<String> commands = new ArrayList<>();
        if (asst.getLanguage()== SbAssignment.Language.JAVA && asst.getTestFormat()== SbAssignment.TestFormat.OUTPUT) {
            commands.add(String.format("cd ./submissions/assignment-%s/%s", asstnum, username));
            commands.add(String.format("javac %s.java", mainclassname));
        }else if (asst.getLanguage()== SbAssignment.Language.JAVA && asst.getTestFormat()== SbAssignment.TestFormat.UNIT_TEST) {
            commands.add(String.format("cd ./submissions/assignment-%s/", asstnum));
            commands.add(String.format("cp %s %s && cp %s %s ", "TestRunner.java", "./"+username,
                    "TestJunit.java", "./"+username));
            commands.add(String.format("cd %s", "./"+username));
            commands.add(String.format("javac %s", "TestRunner.java"));
        }
        else if (asst.getLanguage()== SbAssignment.Language.C && asst.getTestFormat()== SbAssignment.TestFormat.OUTPUT) {
            commands.add(String.format("cd ./submissions/assignment-%s/%s", asstnum, username));
            commands.add(String.format("gcc -o target %s", mainclassname+".c"));
        } else if (asst.getLanguage()== SbAssignment.Language.C && asst.getTestFormat()== SbAssignment.TestFormat.UNIT_TEST){
            //TODO:
            return true;
        }

        try {
            System.out.println("Compiling...");
            String message=executeShellCommands(commands);
            return true;
        } catch (ShellException e) {
            System.out.println("ShellException: " + e.getMessage());
            return false;
        }
    }

    //TODO this should throw (more specific) errors if timeout, and other such stuff.
    private String testOutput(int asstnum, String username, String mainclassname, String input, String expectedoutput) throws ShellException{
        SbAssignment asst = SbAssignment.findAsst(this.assignments, asstnum);
        List<String> commands = new ArrayList<>();

        if (asst.getLanguage()== SbAssignment.Language.JAVA) {
            commands.add(String.format("cd ./submissions/assignment-%s/%s", asstnum, username));
            commands.add(String.format("java %s %s", mainclassname, input));
        }
        else if (asst.getLanguage()== SbAssignment.Language.C) {
            commands.add(String.format("cd ./submissions/assignment-%s/%s", asstnum, username));
            commands.add(String.format("gcc -o target %s", mainclassname+".c")); //compiles is here for now because something's wrong
            commands.add(String.format("./target %s", input)); //fixme need to catch errorstream
        }
        String actualOutput = executeShellCommands(commands);
        if (actualOutput.equals(expectedoutput)) return "";
        else return String.format("Input: %s, output: %s, expected output: %s", input, actualOutput.toString(), expectedoutput);

    }



    private String executeShellCommands(List<String> commands) throws ShellException {
    //Doesn't timeout, just force exits for now
        String output = "";
        ProcessBuilder pb = new ProcessBuilder("/bin/bash");
        try {
            Process p = pb.start();
            BufferedWriter p_stdin = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
            for (int i = 0; i < commands.size(); i++) {
                putCommand(p_stdin, commands.get(i));
            }
            putCommand(p_stdin, "exit");
            p_stdin.close();        //Close to force quit (so that nothing ends up hanging)

            InputStream error = p.getErrorStream();
            InputStreamReader isrerror = new InputStreamReader(error);
            BufferedReader bre = new BufferedReader(isrerror);
            String allerrors="", errorline;

            while ((errorline = bre.readLine()) != null)
                allerrors=allerrors+errorline;
            if (allerrors.length()>0) throw new ShellException(String.format("bash error: %s", allerrors));

            Scanner s = new Scanner(p.getInputStream());
            while (s.hasNext()) output = output + s.nextLine();
            s.close();
            return output;

        } catch (IOException e){
            throw new ShellException(String.format("IO error while executing shell commands: %s", e.getMessage()));
        }
    }

    private void putCommand(BufferedWriter p_stdin, String commd) throws IOException{
        //System.out.println(commd);
        p_stdin.write(commd);
        p_stdin.newLine();
        p_stdin.flush();
    }

    public Message queryPreviousResults(SbUser user, int asstno) throws DataAccessException, SQLException {
        return dbReadWriter.queryPreviousResults(user, asstno);
    }

    public void addScore(String user, int asstno, Message results)throws DataAccessException, SQLException{
        dbReadWriter.addScore(user, asstno, results);
    }


        //##################### TEST

    public String addUser(SbUser user){
        try {
            this.dbReadWriter.addToClassList(user);
            return "SUCCESS";
        } catch (DataAccessException e){
            return "Data Access Exception!: "+ e.getMessage();
        } catch (SQLException e2){
            return "SQL Exception!: "+ e2.getMessage();
        }
    }


}
