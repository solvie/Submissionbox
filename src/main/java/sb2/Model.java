package sb2;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;
import sb2.exceptions.BadConfigXlsxException;
import sb2.exceptions.ExcelOpenError;
import sb2.exceptions.FileSystemException;
import sb2.modelobjects.Message;
import sb2.modelobjects.SbAssignment;
import sb2.modelobjects.SbUser;
import sb2.util.DBReadWriter;
import sb2.util.ExcelReadWriter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
            } catch (IOException e) {
                throw new IOException("Failed to upload the file => " + e.getMessage());
            }
            try{
                SbAssignment currAsst = SbAssignment.findAsst(this.assignments, asstnum);
                System.out.println("Assignment found, "+ currAsst.getTestFormat());
                //
                if (currAsst.getTestFormat()== SbAssignment.TestFormat.OUTPUT) {
                    java.lang.Runtime.getRuntime().exec("mkdir -p "+submissionDir).waitFor();
                    if (name.contains(".zip"))
                        java.lang.Runtime.getRuntime().exec("unzip " + tempDir + "/" + name + " -d " + submissionDir).waitFor();
                    else
                        java.lang.Runtime.getRuntime().exec("mv " + tempDir + "/" + name + " " + submissionDir).waitFor();
                }
            } catch (Exception e){

            }
            return new Message(Message.Mtype.SUCCESS, "You successfully uploaded " + name + "!");

        } else {
            throw new IOException("Failed to upload file because it was empty.");
        }
    }


    public Message runTests(String username, int asstnum){
        /*
            different types of run scripts:
                - output checking run scripts, just input output
                    - javac or
                    - C
                - unit testing
                    - junit
                    - cutest
         */
        SbAssignment assignment = SbAssignment.findAsst(this.assignments, asstnum);
        if (assignment.getTestFormat()== SbAssignment.TestFormat.OUTPUT){
            if (assignment.getLanguage()== SbAssignment.Language.C)
                outputTestC();
            else if (assignment.getLanguage()==SbAssignment.Language.JAVA)
                return outputTestJava(asstnum);
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

    private Message outputTestJava(int asstnum){
        System.out.println("Testing output with Java");
        DualHashBidiMap<String, String> tests = SbAssignment.findAsst(this.assignments, asstnum).getOutputTests();
        //for each test, run them and see the expected value
        List<String> results= new ArrayList<>();
        tests.forEach((k,v) -> results.add(dosomething(k, v)));
        return new Message(Message.Mtype.SUCCESS, results.toString());
    }

    private void unitTestC(){
        System.out.println("Unit testing with C");
    }

    private void unitTestJava(){
        System.out.println("Unit testing with Java");
    }

    //test
    private String dosomething(String k, String v){
        return k+" "+v;
    }

    //-- helpers --


}
