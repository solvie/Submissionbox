package sb2.util;

import java.io.*;
import java.util.*;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.commons.collections4.BidiMap;
import sb2.exceptions.ExcelOpenError;
import sb2.exceptions.ExcelReadError;
import sb2.modelobjects.SbUser;



/**
 * @author Solvie Lee
 * @version 1.0
 *
 * This is the version that corresponds to AutoRef1
 *
 */

public class ExcelReadWriter {//debating whether this should be an implementation of an interface or not.

    public List<SbUser> attemptReadClasslist(String pathname)throws ExcelOpenError, ExcelReadError {
        File file = new File(pathname);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            Workbook spreadsheet = new XSSFWorkbook(inputStream);
            Sheet sheet = spreadsheet.getSheetAt(0);
            return readClasslist(sheet);
        } catch (FileNotFoundException e){
            throw new ExcelOpenError("FileNotFound", e.getCause());
        } catch (IOException e){
            throw new ExcelOpenError("IOException", e.getCause());
        } catch (IllegalArgumentException e){
            throw new ExcelOpenError("IllegalArgumentException", e.getCause());
        }
    }


    /**
     * Reads from an excel spreadsheet to a list of TestCaseRawDatas.
     * @return
     */
    public List<SbUser> readClasslist(Sheet sheet) throws ExcelReadError{
        //TODO: read test case number if there are any.
        List<SbUser> classlist = new ArrayList<>();
        DualHashBidiMap<String, Integer> dict = mapTopRow(sheet.getRow(0));
        Iterator<Row> rowIterator = sheet.rowIterator();
        rowIterator.next(); //Skip the first row
        try {
            while (rowIterator.hasNext()) {
                Row nextRow = rowIterator.next();
                SbUser student = new SbUser(fromDict(dict, nextRow, "Student Name"),fromDict(dict, nextRow, "Password"));
                classlist.add(student);
            }
        } catch (NoSuchFieldError e){
            throw new ExcelReadError("NoSuchField: "+e.getMessage(), e.getCause());
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
