package org.edu_sharing.service.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSVTool {
	
	
	public static String ENC_ISO = "ISO-8859-1";
	
	public static String ENC_UTF8 = "UTF-8";
	
	public List<List<String>> getRecords(InputStream inputStream, String enc){
		List<List<String>> records = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				inputStream,enc ))) {
		    String line;
		    while ((line = br.readLine()) != null) {
		        String[] values = line.split(";");
		        records.add(Arrays.asList(values));
		    }
		    
		    //remove headline
		    records.remove(0);
		   
		    
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return records;
	}

}
