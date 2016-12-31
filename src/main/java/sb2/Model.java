package sb2;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;
import sb2.exceptions.BadConfigXlsxException;
import sb2.exceptions.ExcelOpenError;
import sb2.exceptions.FileSystemException;
import sb2.exceptions.ShellException;
import sb2.modelobjects.Message;
import sb2.modelobjects.SbAssignment;
import sb2.modelobjects.SbUser;
import sb2.util.DBReadWriter;
import sb2.util.ExcelReadWriter;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static java.lang.Thread.sleep;

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

    public List<SbUser> init()throws ExcelOpenError, BadConfigXlsxException{
        return initClasslist();
    }

    public boolean initFileSystem() throws FileSystemException, ExcelOpenError, BadConfigXlsxException, InterruptedException {
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

    private List<SbAssignment> readAssignmentConfig() throws ExcelOpenError, BadConfigXlsxException{
        String path = pathToResources + "assignments-config.xlsx";
        List<SbAssignment> assts = excelReadWriter.readAssignments(excelReadWriter.attemptGetSheet(path, "Sheet1"));
        for (SbAssignment asst : assts) {
            if (asst.getTestFormat() == SbAssignment.TestFormat.OUTPUT) {
                try {
                    asst.setOutputTests(excelReadWriter.readAssignmentTests(excelReadWriter.attemptGetSheet(path, "A" + asst.getAssignmentNum())));
                } catch (Exception e) {
                    throw new BadConfigXlsxException("Couldn't read the output tests for asst" + asst.getAssignmentNum());
                }
            }
        }
        System.out.println("\n all is well");
        return assts;
    }

    private List<SbUser> initClasslist() throws ExcelOpenError, BadConfigXlsxException{
        return excelReadWriter.readClasslist(excelReadWriter.attemptGetSheet(pathToResources+"classlist.xlsx", "Sheet1"));
    }

    //TODO: need to handle if student already has directory/ if they already have code in it.

    public Message acceptFile(MultipartFile file, String username, int asstnum) throws IOException, InterruptedException, FileSystemException{

        if (!file.isEmpty()) {
            String name = file.getOriginalFilename();
            String tempDir = "./temp/"+username;
            String submissionDir = "./submissions/assignment-"+asstnum+"/"+ username+"/";
            String submissionDirUnitTests = "./submissions/assignment-"+asstnum+"/src/UnderTest/"+ username+"/";

            try {
                byte[] bytes = file.getBytes();
                java.lang.Runtime.getRuntime().exec("mkdir -p "+tempDir).waitFor();
                BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(new File(tempDir + "/" + name)));
                stream.write(bytes);
                stream.close();
                //TODO: clean if there already is stuff
            } catch (IOException e) {
                throw new IOException("Failed to upload the file => " + e.getMessage());
            }
            try{
                SbAssignment currAsst = SbAssignment.findAsst(this.assignments, asstnum);
                System.out.println("Assignment found, "+ currAsst.getTestFormat());
                //
                if (currAsst.getTestFormat()== SbAssignment.TestFormat.OUTPUT) {
                    java.lang.Runtime.getRuntime().exec("mkdir -p "+submissionDir).waitFor();
                    if (name.contains(".zip")) {
                        List<String> unzip = Arrays.asList(String.format("unzip %s/%s -d %s/", tempDir, name, tempDir));
                        executeShellCommands(unzip);
                        List<String> moveJava = Arrays.asList(String.format(
                                "for entry in $(find %s/. -name *.java); " +
                                        " do if [[ $entry != *_MACOSX* ]]; then mv $entry %s; fi; "+
                                        " done;", tempDir, submissionDir));
                        executeShellCommands(moveJava);
                    }
                    else
                        java.lang.Runtime.getRuntime().exec("mv " + tempDir + "/" + name + " " + submissionDir).waitFor();
                        System.out.println("moved single file input");
                } else if (currAsst.getTestFormat()== SbAssignment.TestFormat.UNIT_TEST){
                    //TODO need to figure out how the canonical way to upload unit tests should be.
                    //initial thoughts: classlist (maybe with an  "instructor-list sheet") should be initialized with the server
                    // instructors have the additional power to set assignments config, after the server is up and running
                    //TODO: for now this takes care of only single c file submssions
                    List<String> moveC = Arrays.asList(String.format("mkdir -p %s", submissionDirUnitTests),
                            String.format("mv %s %s", tempDir+"/"+name, submissionDirUnitTests),
                            String.format("cd %s", submissionDirUnitTests),
                            String.format("sed -i 's/main\\(.*argv\\)/studentmain\\1/' %s", name)
                    );
                    executeShellCommands(moveC);

                }
            } catch (Exception e){
                //todo
            }
            return new Message(Message.Mtype.SUCCESS, name);

        } else {
            throw new IOException("Failed to upload file because it was empty.");
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
                unitTestJava();
        }
        return new Message(Message.Mtype.WARNING, "not done yet, TODO");
    }

    private Message runOutputTest(String mainclassname,  String username, int asstnum){
        DualHashBidiMap<String, String> tests = SbAssignment.findAsst(this.assignments, asstnum).getOutputTests();
        //for each test, run them and see the expected value
        List<String> results= new ArrayList<>();
        try {
            if (testCompiles(asstnum, username, mainclassname)) //if it compiles, run the tests.
                tests.forEach(
                        (k, v) -> {
                            String ans;
                            try {
                                ans = testOutput(asstnum, username, mainclassname, k, v);
                            } catch (ShellException e) {
                                ans = "ERROR." + e.getMessage();
                            }
                            results.add(ans);
                        }
                );
        } catch (ShellException e){
            results.add( "ERROR WHILE COMPILING" + e.getMessage());
        }
        return new Message(Message.Mtype.SUCCESS, results.toString());
    }

    private Message runUnitTestsC(String mainclassname,  String username, int asstnum){
        String submissionDirUnitTests = "./submissions/assignment-"+asstnum+"/src/UnderTest/"+ username+"/";
        String result="";
        System.out.println("Unit testing with C");
        try {
            if (testCompiles(asstnum, username, mainclassname)) { //if it compiles, run the tests.
                System.out.println("do stuff");
                //DOSTUFF
                List<String> runUnitTests = Arrays.asList(
                        String.format("bash run.sh -f %s -n %d -u %s", mainclassname, asstnum, username )
                );
                result = executeShellCommands(runUnitTests);
                System.out.println("result was: "+ result);
                return new Message(Message.Mtype.SUCCESS, result);
            }
        } catch (ShellException e){
            result= "ERROR WHILE COMPILING" + e.getMessage();
        } catch (IOException e){
            result= "IOException :"+ e.getMessage();
        }
        return new Message(Message.Mtype.SUCCESS, result);

    }

    private void unitTestJava(){
        System.out.println("Unit testing with Java");
    }

    private boolean testCompiles(int asstnum, String username, String mainclassname) throws ShellException{
        SbAssignment asst = SbAssignment.findAsst(this.assignments, asstnum);
        List<String> commands = new ArrayList<>();
        if (asst.getLanguage()== SbAssignment.Language.JAVA) {
            commands.add(String.format("cd ./submissions/assignment-%s/%s", asstnum, username));
            commands.add(String.format("javac %s.java", mainclassname));
        }
        else if (asst.getLanguage()== SbAssignment.Language.C && asst.getTestFormat()== SbAssignment.TestFormat.OUTPUT) {
            commands.add(String.format("cd ./submissions/assignment-%s/%s", asstnum, username));
            commands.add(String.format("gcc -o target %s", mainclassname)); //fixme didn't test.
        } else if (asst.getLanguage()== SbAssignment.Language.C && asst.getTestFormat()== SbAssignment.TestFormat.UNIT_TEST){
            //TODO:
            return true;
        }

        try {
            System.out.println("Compiling...");
            executeShellCommands(commands);//TODO: needs to return false if doesn't compile
            sleep(100);
            return true;
        } catch (IOException e) {
            throw new ShellException("IOException while compiling java");
        } catch (InterruptedException e) {
            throw new ShellException("just for sleep");//fixme
        }
    }

    //TODO this should throw (more specific) errors if timeout, and other such stuff.
    private String testOutput(int asstnum, String username, String mainclassname, String input, String output) throws ShellException{
        String runLine="";
        SbAssignment asst = SbAssignment.findAsst(this.assignments, asstnum);
        List<String> commands = new ArrayList<>();

        if (asst.getLanguage()== SbAssignment.Language.JAVA) {
            commands.add(String.format("cd ./submissions/assignment-%s/%s", asstnum, username));
            commands.add(String.format("java %s %s", mainclassname, input));
        }
        else if (asst.getLanguage()== SbAssignment.Language.C) {
            commands.add(String.format("cd ./submissions/assignment-%s/%s", asstnum, username));
            commands.add(String.format("gcc -o target %s", mainclassname)); //compiles is here for now because something's wrong
            commands.add(String.format("./target %s", input)); //fixme need to catch errorstream
        }

        try {

            String actualOutput = executeShellCommands(commands);
            return String.format("input: %s, output: %s, expected output: %s", input, actualOutput.toString(), output);

        } catch (IOException e) {
            throw new ShellException("IOException while compiling and running java");
        }
    }



    private String executeShellCommands(List<String> commands) throws IOException {
//TODO: Doesn't timeout, just force exits for now
        String output = "";
        ProcessBuilder pb = new ProcessBuilder("/bin/bash");
        Process p = pb.start();
        BufferedWriter p_stdin = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
        for (int i = 0; i < commands.size(); i++) {
            putCommand(p_stdin, commands.get(i));
        }
        putCommand(p_stdin, "exit");

        p_stdin.close();        //Close to force quit (so that nothing ends up hanging)


        Scanner s = new Scanner(p.getInputStream());
        while (s.hasNext()) output = output + s.next();
        s.close();
        return output;

    }

    private void putCommand(BufferedWriter p_stdin, String commd) throws IOException{
        System.out.println(commd);
        p_stdin.write(commd);
        p_stdin.newLine();
        p_stdin.flush();

    }

}
