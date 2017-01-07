package sb2.util;

import java.io.*;
import java.util.*;

import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import sb2.exceptions.BadConfigXlsxException;
import sb2.modelobjects.SbAssignment;
import sb2.modelobjects.SbUser;



/**
 * @author Solvie Lee
 * @version 1.0
 *
 *
 */

public class ExcelReadWriter {//debating whether this should be an implementation of an interface or not.

    public Sheet attemptGetSheet(String pathname, String sheetName)throws BadConfigXlsxException {
        File file = new File(pathname);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            Workbook spreadsheet = new XSSFWorkbook(inputStream);
            return spreadsheet.getSheet(sheetName);
        } catch (FileNotFoundException e){
            throw new BadConfigXlsxException("FileNotFound", e.getCause());
        } catch (IOException e){
            throw new BadConfigXlsxException("IOException", e.getCause());
        } catch (IllegalArgumentException e){
            throw new BadConfigXlsxException("IllegalArgumentException", e.getCause());
        }
    }


    public List<SbAssignment> readAssignments(Sheet sheet) throws BadConfigXlsxException{
        List<SbAssignment> assignmentList = new ArrayList<>();
        DualHashBidiMap<String, Integer> dict = mapTopRow(sheet.getRow(0));
        Iterator<Row> rowIterator = sheet.rowIterator();
        rowIterator.next(); //Skip the first row
        try {
            while (rowIterator.hasNext()) {
                Row nextRow = rowIterator.next();
                SbAssignment asst = new SbAssignment(fromDict(dict, nextRow, "Assignment"),
                        fromDict(dict, nextRow, "Language"), fromDict(dict, nextRow, "Test Format"));
                assignmentList.add(asst);
            }
        } catch (NoSuchFieldError e){
            throw new BadConfigXlsxException("NoSuchField: "+e.getMessage(), e.getCause());
        }
        return assignmentList;
    }


    public DualHashBidiMap<String, String> readAssignmentTests(Sheet sheet) throws BadConfigXlsxException{
        DualHashBidiMap<String, String> tests = new DualHashBidiMap<>();
        DualHashBidiMap<String, Integer> dict = mapTopRow(sheet.getRow(0));
        Iterator<Row> rowIterator = sheet.rowIterator();
        rowIterator.next(); //Skip the first row
        try {
            while (rowIterator.hasNext()) { //TODO: more error handling here
                Row nextRow = rowIterator.next();
                tests.put(fromDict(dict, nextRow, "Input"), fromDict(dict, nextRow, "Output"));
            }
        } catch (NoSuchFieldError e){
            throw new BadConfigXlsxException("NoSuchField; tests sheet not configured properly "+e.getMessage(), e.getCause());
        }
        return tests;
    }


    public List<SbUser> readClasslist(Sheet sheet) throws BadConfigXlsxException{
        List<SbUser> classlist = new ArrayList<>();
        DualHashBidiMap<String, Integer> dict = mapTopRow(sheet.getRow(0));
        Iterator<Row> rowIterator = sheet.rowIterator();
        rowIterator.next(); //Skip the first row
        try {
            while (rowIterator.hasNext()) {
                Row nextRow = rowIterator.next();
                SbUser student = new SbUser(fromDict(dict, nextRow, "Student Name"),fromDict(dict, nextRow, "ID"));
                classlist.add(student);
            }
        } catch (NoSuchFieldError e){
            throw new BadConfigXlsxException("NoSuchField: "+e.getMessage(), e.getCause());
        }
        return classlist;
    }

    /**
     * Maps the top row of the spreadsheet's values to its column indexes.
     * @param toprow The spreadsheet's top row
     * @return A dictionary with the name of the row as the key and the index of the column as a value
     */
    public DualHashBidiMap<String, Integer> mapTopRow(Row toprow){
        DualHashBidiMap<String, Integer> dict = new DualHashBidiMap();
        Iterator<Cell> cellIterator = toprow.cellIterator();
        while (cellIterator.hasNext()){
            Cell cell = cellIterator.next();
            String entry = cell.getStringCellValue();
            dict.put(entry, cell.getColumnIndex());
        }
        return dict;
    }

    /**
     * Returns a row's cell corresponding to the requested column.
     * @param dict Dictionary that maps the column names to their indices
     * @param row The row in question
     * @param key The column name
     * @return String value of the cell
     */
    private String fromDict(DualHashBidiMap<String, Integer> dict, Row row, String key) throws NoSuchFieldError{
        if (dict.get(key)==null) throw new NoSuchFieldError("no such column \""+key+"\"");
        else {
            Cell cell = row.getCell(dict.get(key), Row.CREATE_NULL_AS_BLANK );
            return cell.toString();
        }
    }


}
