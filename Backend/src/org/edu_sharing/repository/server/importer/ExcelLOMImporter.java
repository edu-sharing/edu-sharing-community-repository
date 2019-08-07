package org.edu_sharing.repository.server.importer;

import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
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
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.MimeTypes;
import org.edu_sharing.repository.client.tools.StringTool;
import org.edu_sharing.repository.server.MCAlfrescoAPIClient;
import org.edu_sharing.repository.server.tools.forms.DuplicateFinder;


public class ExcelLOMImporter {

	Log logger = LogFactory.getLog(ExcelLOMImporter.class);
	
	HashMap<String,String> excelAlfMap = null; 
	
	
	ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
	DictionaryService dictionaryService = serviceRegistry.getDictionaryService(); 
	NodeService nodeService = serviceRegistry.getNodeService();
	
	String targetFolder = null;
	
	InputStream is = null;
	
	int maxNodesInFolder = 100;
	
	MCAlfrescoAPIClient apiClient = new MCAlfrescoAPIClient();
	
	private HashMap<String, HashMap<String, Object>> currentLevelObjects = null;

	private int rowCount;
	
	public int getRowCount() {
		return rowCount;
	}

	public ExcelLOMImporter(String targetFolder, InputStream is) throws Exception {
		
		this.targetFolder = targetFolder;
		this.is = is;
		
		
		HashMap<Integer,String> IdxColumnMap = new HashMap<Integer,String>();
		
		try{
			Workbook workbook = WorkbookFactory.create(this.is);
			
			Sheet sheet = workbook.getSheetAt(0);
			
			rowCount = 0;
			String parentFolder = targetFolder;
			
			NodeRef targetFolderNodeRef = new NodeRef(MCAlfrescoAPIClient.storeRef,targetFolder);
			QName assocTypeContains = QName.createQName(CCConstants.CM_ASSOC_FOLDER_CONTAINS);
			for(Row row : sheet){
				
			
				
				String folderName = new Integer(rowCount / maxNodesInFolder).toString();
					
					
				NodeRef currentFolder = nodeService.getChildByName(targetFolderNodeRef, assocTypeContains, folderName);
				
				if(currentFolder == null){
					Map<QName,Serializable> folderProps = new HashMap<QName,Serializable>();
					folderProps.put(QName.createQName(CCConstants.CM_NAME), folderName);
					folderProps.put(QName.createQName(CCConstants.CM_PROP_C_TITLE), folderName);
					parentFolder = nodeService.createNode(targetFolderNodeRef,assocTypeContains, QName.createQName(folderName),  QName.createQName(CCConstants.CCM_TYPE_MAP),folderProps).getChildRef().getId();
				}
				
				try{
					currentLevelObjects = apiClient.getChildren(parentFolder);
				}catch(Throwable e){
					e.printStackTrace();
				}
				
				if(IdxColumnMap.size() > 0){
					//we got the headers
					HashMap<QName,Serializable> toSafe = new HashMap<QName,Serializable>();
					String nodeName = null;
					
					String contentUrl = null;
					for(Cell cell : row){
						
						int colIdxIdx = cell.getColumnIndex();
						
						if(Cell.CELL_TYPE_STRING != cell.getCellType()){
							continue;
						}
						
						String columnName = IdxColumnMap.get(colIdxIdx);
						//System.out.println(columnName + " " + toSafe.get(QName.createQName(CCConstants.CM_NAME)) + " " + cell.getStringCellValue() + " colIdx:" + colIdxIdx);
						String alfrescoProperty = null;
						String value = cell.getStringCellValue();
						if(value == null) continue;
						value = value.trim();
						if(value.equals("")) continue;
						
						if(columnName != null){
							alfrescoProperty = getExcelAlfMap().get(columnName);
						}
						
						
						
						if(alfrescoProperty != null){
							if(alfrescoProperty != null && 
									alfrescoProperty.equals(CCConstants.CM_PROP_CONTENT)){
								contentUrl = value;
							}else{
								PropertyDefinition propDef = dictionaryService.getProperty(QName.createQName(alfrescoProperty));
								
								if(propDef != null) {
									if(propDef.isMultiValued() && !alfrescoProperty.contains("contributer")){
										ArrayList<String> multival = new ArrayList<String>();
										
										//String[] vals = value.split(",");   StringTool.escape(CCConstants.MULTIVALUE_SEPARATOR)
										String[] vals = value.split(StringTool.escape(CCConstants.MULTIVALUE_SEPARATOR));
										multival.addAll(Arrays.asList(vals));
										
										toSafe.put(QName.createQName(alfrescoProperty), multival);
									}else{
										toSafe.put(QName.createQName(alfrescoProperty), value);
									}
								}else {
									logger.error("unkown property: " + alfrescoProperty);
									continue;
								}
								
								if(alfrescoProperty.equals(CCConstants.LOM_PROP_GENERAL_TITLE)){
									nodeName = value;
								
									
									
									HashMap<String,Object> eduProps = new HashMap<String,Object>();
									eduProps.put(CCConstants.CM_NAME, nodeName);
									eduProps.put(CCConstants.LOM_PROP_GENERAL_TITLE, nodeName);
									
									
									new DuplicateFinder().transformToSafeName(currentLevelObjects, eduProps);
									
									
									toSafe.put(QName.createQName(CCConstants.CM_NAME), (String)eduProps.get(CCConstants.CM_NAME));
								}
							}
						}
						
					}
					
					if(toSafe.size() > 0 && nodeName != null && !nodeName.trim().equals("")){
						
						//check for valid thumbnail url
						boolean createNode = true;
						String thumbUrl = (String)toSafe.get(QName.createQName(CCConstants.CCM_PROP_IO_THUMBNAILURL));
						if(thumbUrl == null || !thumbUrl.startsWith("http")) {
							logger.error("invalid thumbnail url:" + thumbUrl +" for:" +toSafe.get(QName.createQName(CCConstants.CM_NAME))+" will not safe object");
							createNode = false;
						}
						if(createNode) {
							ChildAssociationRef newNode = nodeService.createNode(new NodeRef(MCAlfrescoAPIClient.storeRef,parentFolder),QName.createQName(CCConstants.CM_ASSOC_FOLDER_CONTAINS), QName.createQName(nodeName),  QName.createQName(CCConstants.CCM_TYPE_IO),toSafe);
							
							HashMap<String,Object> versProps = new HashMap<String,Object> ();
							for(Map.Entry<QName, Serializable> entry : toSafe.entrySet()){
								versProps.put(entry.getKey().toString(), entry.getValue());
							}
							
							if(contentUrl != null && !contentUrl.trim().equals("")){
								String mimetype = MimeTypes.guessMimetype(contentUrl);
								InputStream inputStream = new URL(contentUrl).openConnection().getInputStream();
								apiClient.writeContent(MCAlfrescoAPIClient.storeRef, 
										newNode.getChildRef().getId(), 
										inputStream, 
										mimetype, 
										null, 
										CCConstants.CM_PROP_CONTENT);
							}
						
							apiClient.createVersion(newNode.getChildRef().getId(), versProps);
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
				
				rowCount++;
			}
			
			
			
		}catch(Exception e){
			throw e;
		}
		
	}
	
	
	public HashMap<String, String> getExcelAlfMap() {
		if(excelAlfMap == null){
			excelAlfMap = new HashMap<String, String>();
			excelAlfMap.put("catalog", CCConstants.CCM_PROP_IO_REPLICATIONSOURCE);
			excelAlfMap.put("identifier", CCConstants.CCM_PROP_IO_REPLICATIONSOURCEID);
			excelAlfMap.put("datestamp", CCConstants.CCM_PROP_IO_REPLICATIONSOURCETIMESTAMP);
			excelAlfMap.put("title", CCConstants.LOM_PROP_GENERAL_TITLE);
			excelAlfMap.put("language", CCConstants.LOM_PROP_GENERAL_LANGUAGE);
			excelAlfMap.put("description", CCConstants.LOM_PROP_GENERAL_DESCRIPTION);
			excelAlfMap.put("keyword", CCConstants.LOM_PROP_GENERAL_KEYWORD);
			excelAlfMap.put("context", CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_CONTEXT);
			excelAlfMap.put("educationalIntendedenduserrole", CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_INTENDEDENDUSERROLE);
			excelAlfMap.put("educationalTypicalAgeRangeFrom", CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGEFROM);
			excelAlfMap.put("educationalTypicalAgeRangeTo", CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGETO);
			excelAlfMap.put("educationalTypicalAgeRange", CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_TYPICALAGERANGE);
			excelAlfMap.put("lifeCycleContributerAuthor", CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_AUTHOR);
			excelAlfMap.put("lifeCycleContributerPublisher", CCConstants.CCM_PROP_IO_REPL_LIFECYCLECONTRIBUTER_PUBLISHER);
			excelAlfMap.put("metadataContributerProvider", CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_PROVIDER);
			excelAlfMap.put("metadataContributerCreator", CCConstants.CCM_PROP_IO_REPL_METADATACONTRIBUTER_CREATOR);
			excelAlfMap.put("technicalFormat", CCConstants.LOM_PROP_TECHNICAL_FORMAT);
			excelAlfMap.put("technicalLocation", CCConstants.LOM_PROP_TECHNICAL_LOCATION);
			excelAlfMap.put("wwwurl", CCConstants.CCM_PROP_IO_WWWURL);
			excelAlfMap.put("educationalLearningResourceType", CCConstants.CCM_PROP_IO_REPL_EDUCATIONAL_LEARNINGRESSOURCETYPE);
			excelAlfMap.put("RightsCopyrightAndOtherRestrictions", CCConstants.LOM_PROP_RIGHTS_COPY_RIGHT);
			excelAlfMap.put("RightsDescription", CCConstants.LOM_PROP_RIGHTS_RIGHTS_DESCRIPTION);
			excelAlfMap.put("thumbnailUrl", CCConstants.CCM_PROP_IO_THUMBNAILURL);
			excelAlfMap.put("taxonId",CCConstants.CCM_PROP_IO_REPL_TAXON_ID);
			excelAlfMap.put("taxonEntry",CCConstants.CCM_PROP_IO_REPL_TAXON_ENTRY);
			excelAlfMap.put("licenseKey",CCConstants.CCM_PROP_IO_COMMONLICENSE_KEY);
			excelAlfMap.put("licenseVersion",CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_VERSION);
			excelAlfMap.put("licenseLocale",CCConstants.CCM_PROP_IO_COMMONLICENSE_CC_LOCALE);
			excelAlfMap.put("licenseSourceUrl",CCConstants.CCM_PROP_IO_LICENSE_SOURCE_URL);
			excelAlfMap.put("licenseTitleOfWork",CCConstants.CCM_PROP_IO_LICENSE_TITLE_OF_WORK);
			excelAlfMap.put("licenseTo",CCConstants.CCM_PROP_IO_LICENSE_TO);
			excelAlfMap.put("licenseValid",CCConstants.CCM_PROP_IO_LICENSE_VALID);
		}
		return excelAlfMap;
	}
	
	
	
}
