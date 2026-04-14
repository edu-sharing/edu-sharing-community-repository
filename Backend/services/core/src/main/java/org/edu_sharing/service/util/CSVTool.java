package org.edu_sharing.service.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class CSVTool {

    @Data
    public static class CSVResult {
        List<String> headers;
        List<Map<String, String>> lines;
    }


    public static String ENC_ISO = "ISO-8859-1";

    public static String ENC_UTF8 = "UTF-8";

    public static CSVResult readCSV(BufferedReader in, char delimiter) {
        try (in) {
            CSVFormat csvFormat = CSVFormat.Builder.create()
                    .setDelimiter(delimiter)
                    .setQuote('"')
                    .setHeader()
                    .build();

            CSVParser parser = CSVParser.parse(in, csvFormat);
            CSVResult result = new CSVResult();
            result.setHeaders(parser.getHeaderNames());
            ArrayList<Map<String, String>> lines = new ArrayList<>();
            for (CSVRecord record : parser.getRecords()) {
                lines.add(record.toMap());
            }
            result.setLines(lines);
            return result;
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }


    public List<List<String>> getRecords(String file, String enc) {
        try {
            return this.getRecords(new FileInputStream(file), enc);
        } catch (FileNotFoundException e) {
            log.error("File not found: {}", e.getMessage(), e);
            return null;
        }
    }

    public List<List<String>> getRecords(InputStream input, String enc) {
        List<List<String>> result = new ArrayList<>();
        try {

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            input, enc));

            CSVParser parser = CSVParser.parse(in, CSVFormat.newFormat(';'));
            for (CSVRecord record : parser.getRecords()) {
                result.add(record.toList());
            }

        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            log.error("File not found: {}", e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
            log.error("Unsupported Encoding: {}", e.getMessage());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            log.error("Cant read file: {}", e.getMessage(), e);
        }

        return result;
    }


    public static void main(String[] args) {
        String file = "/Users/mv/git/Edu-Sharing-Internal/Backend/extensions/Mebis/schoolList.csv";

        new CSVTool().getRecords(file, ENC_UTF8);
    }

}
