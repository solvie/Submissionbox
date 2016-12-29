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

    public List<SbUser> init()throws ExcelOpenError, BadConfigXlsxException{
        return initClasslist();
    }

    /* Directory structure  should look like:
     SubmissionBox2.0
     |-> submissions
          |-> assignment-1
               |-> nikola_tesla
               |-> albert_einstein
                      ...
          |-> assignment-2
              ...
          |-> assignment-n  */
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

    //TODO: after extracting info, use the info to sort it into the right directory.

    public Message acceptFile(MultipartFile file, String username, int asstnum) throws IOException, InterruptedException, FileSystemException{
        if (!file.isEmpty()) {
            String name = file.getOriginalFilename();
            String tempDir = "./temp/"+username;
            String submissionDir = "./submissions/assignment-"+asstnum+"/"+ username+"/";
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
                    System.out.println(submissionDir);
                    if (name.contains(".zip")) {

                        String[] unzip = {String.format("unzip %s/%s -d %s/", tempDir, name, tempDir)};
                        executeShellCommands(unzip);

                        String[] moveJava = {String.format(
                                "for entry in $(find %s/. -name *.java); " +
                                        " do if [[ $entry != *_MACOSX* ]]; then mv $entry %s; fi; "+
                                        " done;", tempDir, submissionDir)};
                        executeShellCommands(moveJava);
                    }
                    else
                        java.lang.Runtime.getRuntime().exec("mv " + tempDir + "/" + name + " " + submissionDir).waitFor();
                }
            } catch (Exception e){
                //todo
            }
            return new Message(Message.Mtype.SUCCESS, name);

        } else {
            throw new IOException("Failed to upload file because it was empty.");
        }
    }


    public Message runTests(String filename, String mainclassname, String username, int asstnum){
        SbAssignment assignment = SbAssignment.findAsst(this.assignments, asstnum);
        if (assignment.getTestFormat()== SbAssignment.TestFormat.OUTPUT){
            if (assignment.getLanguage()== SbAssignment.Language.C)
                outputTestC();
            else if (assignment.getLanguage()==SbAssignment.Language.JAVA)
                return runOutputTestJava(mainclassname, username, asstnum);
        } else if (assignment.getTestFormat()== SbAssignment.TestFormat.UNIT_TEST){
            if (assignment.getLanguage()==SbAssignment.Language.C)
                unitTestC();
            else if (assignment.getLanguage()== SbAssignment.Language.JAVA)
                unitTestJava();
        }
        return new Message(Message.Mtype.WARNING, "not done yet, TODO");
    }

    private void outputTestC(){
        System.out.println("Testing output with C");
    }

    private Message runOutputTestJava(String mainclassname,  String username, int asstnum){
        System.out.println("Testing output with Java");
        DualHashBidiMap<String, String> tests = SbAssignment.findAsst(this.assignments, asstnum).getOutputTests();
        //for each test, run them and see the expected value
        List<String> results= new ArrayList<>();
        try {
            if (testJavaCompile(asstnum, username, mainclassname)) //if it compiles, run the tests.
                tests.forEach(
                        (k, v) -> {
                            String ans;
                            try {
                                ans = testJavaOutput(asstnum, username, mainclassname, k, v);
                            } catch (ShellException e) { //TODO: this shouldn't be an excelopenerror.
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

    private void unitTestC(){
        System.out.println("Unit testing with C");
    }

    private void unitTestJava(){
        System.out.println("Unit testing with Java");
    }

    private boolean testJavaCompile(int asstnum, String username, String mainclassname) throws ShellException{
        try {
            String[] commands =
                    { String.format("cd ./submissions/Assignment-%s/%s", asstnum, username),
                            String.format("javac %s.java", mainclassname),
                    };
            executeShellCommands(commands);//TODO: needs to return false if doesn't compile
            return true;
        } catch (IOException e) {
            throw new ShellException("IOException while compiling java");
        }
    }

    //TODO this should throw (more specific) errors if timeout, and other such stuff.
    private String testJavaOutput(int asstnum, String username, String mainclassname, String input, String output) throws ShellException{
        try {
            String[] commands =
                    { String.format("cd ./submissions/Assignment-%s/%s", asstnum, username),
                            String.format("java %s %s", mainclassname, input),
                    };
            String actualOutput = executeShellCommands(commands);
            return String.format("input: %s, output: %s, expected output: %s", input, actualOutput.toString(), output);

        } catch (IOException e) {
            throw new ShellException("IOException while compiling and running java");
        }
    }

    private String executeShellCommands(String[] commands) throws IOException {
        String output = "";
        ProcessBuilder pb = new ProcessBuilder("/bin/bash");
        Process p = pb.start();
        BufferedWriter p_stdin = new BufferedWriter(new OutputStreamWriter(p.getOutputStream()));
        for (int i = 0; i < commands.length; i++)
            putCommand(p_stdin, commands[i]);
        putCommand(p_stdin, "exit");
        Scanner s = new Scanner(p.getInputStream());
        while (s.hasNext()) output = output + s.next();
        s.close();
        return output;
    }

    private void putCommand(BufferedWriter p_stdin, String commd) throws IOException{
        p_stdin.write(commd);
        p_stdin.newLine();
        p_stdin.flush();
    }



    //-- helpers --


}
