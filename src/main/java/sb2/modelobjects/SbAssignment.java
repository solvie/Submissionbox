package sb2.modelobjects;

import lombok.Data;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import sb2.exceptions.BadConfigXlsxException;

import java.util.List;

/**
 * Created by solvie on 2016-12-11.
 */


@Data
public class SbAssignment {
    private int assignmentNum;
    private Language language;
    private TestFormat testFormat;
    private DualHashBidiMap<String, String> outputTests;

    public SbAssignment(){}

    public SbAssignment(String assignmentNum, String language, String testFormat) throws BadConfigXlsxException {
        //this.assignmentNum = assignmentNum;
        this.assignmentNum =(int) Double.parseDouble(assignmentNum);
        this.language = convertStringToLanguage(language);
        this.testFormat = convertStringToTestFormat(testFormat);
    }

    public enum Language{
        C, JAVA;
    }

    public enum TestFormat{
        UNIT_TEST, OUTPUT;
    }

    public Language convertStringToLanguage(String stringvers) throws BadConfigXlsxException {
        if (stringvers.equalsIgnoreCase("C")) return Language.C;
        if (stringvers.equalsIgnoreCase("Java")) return Language.JAVA;
        else throw new BadConfigXlsxException("Cannot recognize language: \""+ stringvers+ "\"");
    }

    public TestFormat convertStringToTestFormat(String stringvers) throws BadConfigXlsxException {
        if (stringvers.equalsIgnoreCase("Output")) return TestFormat.OUTPUT;
        if (stringvers.equalsIgnoreCase("Unit")) return TestFormat.UNIT_TEST;
        else throw new BadConfigXlsxException("Cannot recognize testFormat: \""+ stringvers+ "\"");
    }

    public static SbAssignment findAsst(List<SbAssignment> assts, int asstnum){
        for (SbAssignment asst: assts)
            if (asst.getAssignmentNum()==asstnum)
                return asst;
        return null;
    }

}
