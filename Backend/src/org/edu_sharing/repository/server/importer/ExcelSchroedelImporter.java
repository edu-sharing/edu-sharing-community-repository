package org.edu_sharing.repository.server.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.rpc.SchoolContextValues;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.SchoolContextServiceImpl;
import org.edu_sharing.repository.server.tools.ApplicationInfoList;

public class ExcelSchroedelImporter {
	
	Log logger = LogFactory.getLog(ExcelLOMImporter.class);
	
	HashMap<String,String> excelAlfMap = null; 
	
	
	ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
	DictionaryService dictionaryService = serviceRegistry.getDictionaryService(); 
	NodeService nodeService = serviceRegistry.getNodeService();
	
	String targetFolder = null;
	
	InputStream is = null;
	
	public ExcelSchroedelImporter(String targetFolder, InputStream is) {
		
		this.targetFolder = targetFolder;
		this.is = is;
		
		
		HashMap<Integer,String> IdxColumnMap = new HashMap<Integer,String>();
		
		SchoolContextServiceImpl scs = new SchoolContextServiceImpl( new MCAlfrescoAPIClient());
		
		try{
			Workbook workbook = WorkbookFactory.create(this.is);
			
			Sheet sheet = workbook.getSheetAt(0);
			for(Row row : sheet){
				
				
				
				if(IdxColumnMap.size() > 0){
					
					ArrayList<String> productNumbers = new ArrayList<String>();
					
					
					//we got the headers
					HashMap<QName,Serializable> toSafe = new HashMap<QName,Serializable>();
					String nodeName = null;
					
					List<String> faecher = new ArrayList<String>();
					List<String> klassen = new ArrayList<String>();
					
					for(Cell cell : row){
						if(Cell.CELL_TYPE_STRING != cell.getCellType()){
							continue;
						}
						int colIdxIdx = cell.getColumnIndex();
						String columnName = IdxColumnMap.get(colIdxIdx);
						String alfrescoProperty = null;
						String value = cell.getStringCellValue();
						
						if(columnName == null){
							//uninteresting column
							continue;
						}
						
						if(columnName != null){
							alfrescoProperty = getExcelAlfMap().get(columnName);
						}
						
						if(columnName.equals("Produktnummer")){
							productNumbers.add(value);
						}
						
						if(columnName.equals("Fächer")){
							String[] mvs = value.split(",");
							for(String val:mvs){
								faecher.add(val.trim());
							}
							continue;
						}
						
						
						if(columnName.equals("Klassen")){
							String[] ks = value.split("bis");
							for(String val:ks){
								klassen.add(val.trim());
							}
						}
						
						
						if(alfrescoProperty != null){
							
							
							if(alfrescoProperty.equals(CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP)){
								String date = value;
								//"2014-10-31 16:56:50.000000" --> to edu-sharing PersistenHandlerFormat
								SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
								Date dateObj = sdf.parse(date);
								SimpleDateFormat sdfEdu = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:sss");
								value = sdfEdu.format(dateObj);
							}
							
							
							toSafe.put(QName.createQName(alfrescoProperty), value);
							
							
							if(alfrescoProperty.equals(CCConstants.LOM_PROP_GENERAL_TITLE)){
								nodeName = value.replaceAll(ApplicationInfoList.getHomeRepository().getValidatorRegexCMName(), "_");
								toSafe.put(QName.createQName(CCConstants.CM_NAME), nodeName);
							}
						}
						
					}
					
					/**
					 * get schoolcontext references for faecher
					 */
					List<Map.Entry<String,String>> eduFaecher = new ArrayList<Map.Entry<String,String>>();
					SchoolContextValues scv= scs.getSchoolContextValues();
					for(Map.Entry<String,String> entry : scv.getSchoolSubject().entrySet()){
						for(String fach: faecher){
							if(entry.getValue().contains(fach)){
								eduFaecher.add(entry);
							}
						}
					}
					
					/**
					 * get schoolcontext references for klassen
					 */
					List<Map.Entry<String,String>> eduKlassen = new ArrayList<Map.Entry<String,String>>();
					for(Map.Entry<String,String> entry : scv.getAgeGroup().entrySet()){
						
						for(String klasse : klassen){
							if(entry.getValue().equals(klasse)){
								eduKlassen.add(entry);
							}
						}
						
					}
					
					/**
					 * combine fächer and klassen
					 */
					
					String schoolContextChain = ""; 
					
					for(Map.Entry<String,String> eduFach : eduFaecher){
						for(Map.Entry<String,String> eduKlassenStufe : eduKlassen){
							
							
							String schoolContext  = "";
							//bundesland
							schoolContext += CCConstants.SCHOOLCONTEXT_PATH_SEPARATOR;
							
							//schulart
							schoolContext += CCConstants.SCHOOLCONTEXT_PATH_SEPARATOR;
							
							//fach
							if(eduFach != null) schoolContext += eduFach.getKey();
							schoolContext += CCConstants.SCHOOLCONTEXT_PATH_SEPARATOR;
								
							
							//Jahrgang
							if(eduKlassenStufe != null) schoolContext += eduKlassenStufe.getKey();
							schoolContext += CCConstants.SCHOOLCONTEXT_PATH_SEPARATOR;
							
							
							
							schoolContextChain += schoolContext + CCConstants.MULTIVALUE_SEPARATOR;
							
							
						}
					}
					
					if(schoolContextChain.length() > 0){
						toSafe.put(QName.createQName(CCConstants.CCM_PROP_IO_SCHOOLCONTEXT), schoolContextChain);
					}
					
					toSafe.put(QName.createQName(CCConstants.CCM_PROP_IO_REPLICATIONSOURCE), "Schroedel");
					
					if(toSafe.size() > 0 && nodeName != null && !nodeName.trim().equals("")){
						String newNodeId = nodeService.createNode(new NodeRef(MCAlfrescoAPIClient.storeRef,targetFolder),QName.createQName(CCConstants.CM_ASSOC_FOLDER_CONTAINS), QName.createQName(nodeName),  QName.createQName(CCConstants.CCM_TYPE_IO),toSafe).getChildRef().getId();
					
						
						String schroedelContent = System.getProperty("java.io.tmpdir");
						schroedelContent += File.separator+"schroedel";
						for(String produktNr : productNumbers){
							String filePath = schroedelContent+File.separator+produktNr+".pdf";
							File file = new File(filePath); 
							if(file.canRead() && file.exists()){
								logger.info("reading content for file" + filePath);
								
								MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();
								InputStreamReader r = new InputStreamReader(new FileInputStream(file));
								System.out.println(""+r.getEncoding());
								
								//apiClient.
								apiClient.writeContent(MCAlfrescoAPIClient.storeRef, newNodeId, new File(filePath), apiClient.guessMimetype(filePath), r.getEncoding(), CCConstants.CM_PROP_CONTENT);
								
							}else{
								logger.error("can not read "+filePath);
							}
						}
					}
					
					
					
					
				}else{
				//build the headers
					for(Cell cell : row){
						if(Cell.CELL_TYPE_STRING == cell.getCellType() && getExcelAlfMap().containsKey(cell.getStringCellValue())){
							IdxColumnMap.put(cell.getColumnIndex(),cell.getStringCellValue());
						}
					}
				}
			}
			
			
			
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}
		
		public HashMap<String, String> getExcelAlfMap() {
			if(excelAlfMap == null){
				excelAlfMap = new HashMap<String, String>();
				excelAlfMap.put("Schroedel", CCConstants.CCM_PROP_IO_REPLICATIONSOURCE);
				excelAlfMap.put("Produktnummer", CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID);
				excelAlfMap.put("Datum", CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP);
				excelAlfMap.put("Titel", CCConstants.LOM_PROP_GENERAL_TITLE);
				excelAlfMap.put("Fächer", CCConstants.CCM_PROP_IO_REPL_TAXON_ENTRY);
				excelAlfMap.put("Inhalt", CCConstants.LOM_PROP_GENERAL_DESCRIPTION);
				excelAlfMap.put("Klassen", CCConstants.LOM_PROP_EDUCATIONAL_TYPICALAGERANGE);
			}
			return excelAlfMap;
		}
		

}
