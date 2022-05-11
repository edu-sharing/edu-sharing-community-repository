package org.edu_sharing.service.util;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVStrategy;
import org.apache.log4j.Logger;
import org.edu_sharing.service.foldertemplates.LoggingErrorHandler;

import java.io.*;
import java.util.*;

public class CSVTool {

	private static Logger logger= Logger.getLogger(CSVTool.class);

	public static class CSVResult{
		List<String> headers;
		List<Map<String,String>> lines;

		public List<String> getHeaders() {
			return headers;
		}

		public void setHeaders(List<String> headers) {
			this.headers = headers;
		}

		public List<Map<String, String>> getLines() {
			return lines;
		}

		public void setLines(List<Map<String, String>> lines) {
			this.lines = lines;
		}
	}
	
	
	public static String ENC_ISO = "ISO-8859-1";
	
	public static String ENC_UTF8 = "UTF-8";

	public static CSVResult readCSV(BufferedReader in, char delimiter) {
		try {
			CSVStrategy tmpl = CSVStrategy.DEFAULT_STRATEGY;
			CSVStrategy csvStrategy = new CSVStrategy(delimiter, tmpl.getEncapsulator(), tmpl.getCommentStart());
			CSVParser parser = new CSVParser(in, csvStrategy);

			String[][] values = parser.getAllValues();
			CSVResult result = new CSVResult();
			result.setHeaders(Arrays.asList(values[0].clone()));
			ArrayList<Map<String, String>> lines = new ArrayList<>();
			for (int i = 1; i < values.length; i++) {
				Map<String, String> entry = new HashMap<>();
				try {
					for (int j = 0; j < values[i].length; j++) {
						entry.put(values[0][j], values[i][j]);
					}
				}catch(IndexOutOfBoundsException e){
					throw new IllegalArgumentException("The given csv has invalid structure at line " + i + ": The line rows do not match the heading rows");
				}
				lines.add(entry);
			}
			result.setLines(lines);
			return result;
		}catch(IOException e){
			logger.warn(e);
		}finally {
			try {
				in.close();
			} catch (IOException e) {
				logger.error(e);
			}
		}
		return null;
	}


	public List<List<String>> getRecords(String file, String enc){
		try {
			return this.getRecords(new FileInputStream(file),enc);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public List<List<String>> getRecords(InputStream input, String enc){
		List<List<String>> result = new ArrayList<>();
		try {

			BufferedReader in = new BufferedReader(
					new InputStreamReader(
							input, enc));

			CSVStrategy tmpl = CSVStrategy.DEFAULT_STRATEGY;
			CSVStrategy csvStrategy = new CSVStrategy(';',tmpl.getEncapsulator(),tmpl.getCommentStart());
			CSVParser parser = new CSVParser(in,csvStrategy);
			for(String[] val : parser.getAllValues()) {
				result.add(Arrays.asList(val));
			}

			//remove header
            result.remove(0);

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result;
	}


	public static void main(String[] args){
		String file = "/Users/mv/git/Edu-Sharing-Internal/Backend/extensions/Mebis/schoolList.csv";

		new CSVTool().getRecords(file,ENC_UTF8);
	}

}
