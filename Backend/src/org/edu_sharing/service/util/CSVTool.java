package org.edu_sharing.service.util;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVStrategy;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSVTool {
	
	
	public static String ENC_ISO = "ISO-8859-1";
	
	public static String ENC_UTF8 = "UTF-8";


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
