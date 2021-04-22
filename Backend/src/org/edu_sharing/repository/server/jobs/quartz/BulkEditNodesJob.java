/**
 *
 *  
 * 
 * 
 *	
 *
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 *
 */
package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.repository.server.tools.ApplicationInfo;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.edu_sharing.service.provider.Provider;
import org.edu_sharing.service.util.CSVTool;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Batch edit property for multiple nodes
 * Required parameters:
 * property: The property name to replace
 * value: the target value to set
 * OR copy: the source property to copy the value of
 * startFolder: The id of the folder to start (recursively processing all children)
 * mode: The mode, see enum
 * types: the types of nodes to process, e.g. ccm:io (comma seperated string)
 *
 */
@JobDescription(description = "Bulk change metadata of nodes")
public class BulkEditNodesJob extends AbstractJob{
	protected Logger logger = Logger.getLogger(BulkEditNodesJob.class);
	private org.alfresco.service.cmr.repository.NodeService nodeService;
	@JobFieldDescription(description = "folder id to start from")
	private String startFolder;
	@JobFieldDescription(description = "Lucene query to fetch the nodes that shall be processed. When used, the 'startFolder' parameter is ignored")
	private String lucene;
	@JobFieldDescription(description = "Mode to use")
	private Mode mode;
	@JobFieldDescription(description = "property to modify, e.g. cm:name", sampleValue = "cm:name")
	private String property;
	@JobFieldDescription(description = "Value to replace target property with")
	private Serializable value;
	@JobFieldDescription(description = "property to copy value from, if mode == Copy", sampleValue = "cclom:title")
	private String copy;
	@JobFieldDescription(description = "token to replace, if mode == ReplaceToken")
	private String searchToken;
	@JobFieldDescription(description = "Token to replace with, if mode == ReplaceToken")
	private String replaceToken;
	@JobFieldDescription(description = "Element types to modify (comma seperated list), e.g. ccm:map,ccm:io", sampleValue = "ccm:map,ccm:io")
	private List<String> types;
	@JobFieldDescription(description = "RecurseMode to use")
	private RecurseMode recurseMode;
	@JobFieldDescription(file = true, description = "Mapping list (csv with oldValue + newValue headers), only if mode == ReplaceMapping")
	private String data;
	private CSVTool.CSVResult csv;
	@JobFieldDescription(description = "classname, if mode == Custom. Must be a subclass of java.util.function.Consumer<NodeRef>")
	private String customClass;

	private enum Mode{
		@JobFieldDescription(description = "Replace a property with a fixed string")
		Replace,
		@JobFieldDescription(description = "Search and Replace a string value inside the properties")
		ReplaceToken,
		@JobFieldDescription(description = "Find specific keys in the list and map it to new ones (list must contain oldValue and newValue as csv-headings!)")
		ReplaceMapping,
		@JobFieldDescription(description = "Currently Unsupported")
		Append,
		@JobFieldDescription(description = "Remove the property. Use with searchtoken: one value must be equal, than the property is removed.")
		Remove,
		@JobFieldDescription(description = "Remove Duplicates in multivalue properties.")
		RemoveDuplicates,
		@JobFieldDescription(description = "Use a class that implements custom handling. defined by customClass param")
		Custom
	};

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {

		ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

		ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

		nodeService = serviceRegistry.getNodeService();

		try {
			mode = Mode.valueOf((String) context.getJobDetail().getJobDataMap().get("mode"));
		} catch (Throwable t) {
			throw new IllegalArgumentException("Missing or invalid value for required parameter 'mode'", t);
		}

		if(!mode.equals(Mode.Custom)) {
			property = prepareParam(context, "property", true);
			property = CCConstants.getValidGlobalName(property);
		}

		copy = prepareParam(context, "copy", false);
		if (copy != null) {
			copy = CCConstants.getValidGlobalName(copy);
		}
		value = prepareParam(context, "value", false);
		if (mode.equals(Mode.Replace)) {
			if (copy == null && value == null) {
				throwMissingParam("'value' or 'copy'");
			}
			if (copy != null && value != null) {
				throw new IllegalArgumentException("Only one of parameters 'value' and 'copy' may be set");
			}
		}
		if (mode.equals(Mode.ReplaceToken)) {
			searchToken = prepareParam(context, "searchToken", true);
			replaceToken = prepareParam(context, "replaceToken", true);
		}

		if (mode.equals(Mode.Remove)) {
			searchToken = prepareParam(context, "searchToken", true);
		}

		lucene = prepareParam(context, "lucene", false);

		startFolder = prepareParam(context, "startFolder", true);
		try {
			types = Arrays.stream(((String) context.getJobDetail().getJobDataMap().get("types")).
					split(",")).map(String::trim).map(CCConstants::getValidGlobalName).
					collect(Collectors.toList());
		} catch (Throwable t) {
		}
		if (types == null || types.isEmpty()) {
			throwMissingParam("types");
		}
		recurseMode = RecurseMode.Folders;
		try {
			if (context.getJobDetail().getJobDataMap().get("recurseMode") != null) {
				recurseMode = RecurseMode.valueOf((String) context.getJobDetail().getJobDataMap().get("recurseMode"));
			}
		} catch (Throwable t) {
			throw new IllegalArgumentException("Missing or invalid value for parameter 'recurseMode'", t);
		}
		data = (String) context.getJobDetail().getJobDataMap().get(JobHandler.FILE_DATA);
		if (mode.equals(Mode.ReplaceMapping)) {
			csv = readCSVMapping(data);
		}

		NodeRunner runner = new NodeRunner();

		if (mode.equals(Mode.Custom)) {
			customClass = prepareParam(context, "customClass", true);
			Class clazz = null;
			try {
				clazz = Class.forName(customClass);
				Consumer<NodeRef> consumer = (Consumer<NodeRef>) clazz.getConstructor(new Class[]{}).newInstance(new Object[]{});
				runner.setTask(consumer);
			} catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
				throw new JobExecutionException(e);
			}
		}else{
			runner.setTask((ref) -> {
				org.alfresco.service.cmr.repository.NodeRef nodeRef = new org.alfresco.service.cmr.repository.NodeRef(ref.getStoreRef(), ref.getId());
				logger.info("Bulk edit metadata for node " + ref.getId());
				if (copy != null) {
					value = nodeService.getProperty(nodeRef, QName.createQName(copy));
				}
				if (mode.equals(Mode.Replace)) {
					nodeService.setProperty(nodeRef, QName.createQName(property), value);
				} else if (mode.equals(Mode.Remove)) {
					if (searchToken != null) {
						Serializable current = nodeService.getProperty(nodeRef, QName.createQName(property));
						if (current != null) {
							boolean remove = false;
							if (current instanceof String) {
								if (searchToken.equals((String) current)) {
									remove = true;
								}
							} else if (current instanceof List) {
								for (Object o : (List) current) {
									if (searchToken.equals(0)) {
										remove = true;
									}
								}
							}
							if (remove) {
								nodeService.removeProperty(nodeRef, QName.createQName(property));
							}
						}
					} else {
						nodeService.removeProperty(nodeRef, QName.createQName(property));
					}
				} else if (mode.equals(Mode.ReplaceToken) || mode.equals(Mode.ReplaceMapping)) {
					Serializable current = nodeService.getProperty(nodeRef, QName.createQName(property));
					if (current != null) {
						if (current instanceof String) {
							nodeService.setProperty(nodeRef, QName.createQName(property), processPropertyValue((String) current));
						} else if (current instanceof List) {
							nodeService.setProperty(nodeRef, QName.createQName(property), (Serializable) ((List) current).stream().map((v) -> {
								if (v instanceof String) {
									return processPropertyValue((String) v);
								} else {
									return v;
								}
							}).collect(Collectors.toList()));
						} else {
							logger.info("Can not replace property " + property + "for node " + nodeRef + ": current data is not of type String/List");
						}
					}
				} else if (mode.equals(Mode.RemoveDuplicates)) {
					Serializable current = nodeService.getProperty(nodeRef, QName.createQName(property));
					if (current != null && current instanceof List) {
						if (((List) current).stream().distinct().count() != ((List) current).size()) {
							nodeService.setProperty(nodeRef, QName.createQName(property), (Serializable) ((List) current).stream().distinct().collect(Collectors.toList()));
						}
					}
				} else {
					throw new IllegalArgumentException("Mode " + mode + " is currently not supported");
				}
			});
		}
		runner.setTypes(types);
		runner.setRunAsSystem(true);
		runner.setThreaded(false);
		runner.setRecurseMode(recurseMode);
		runner.setStartFolder(startFolder);
		runner.setLucene(lucene);
		runner.setKeepModifiedDate(true);
		runner.setTransaction(NodeRunner.TransactionMode.Local);
		int count=runner.run();
		logger.info("Processed "+count+" nodes");
	}

	public static CSVTool.CSVResult readCSVMapping(String data) {
		if(data==null) {
			throw new IllegalArgumentException("Missing or invalid value for parameter 'data'");
		}
		CSVTool.CSVResult csv = CSVTool.readCSV(new BufferedReader(new StringReader(data)), ',');
		if(csv == null){
			throw new IllegalArgumentException("Could not read csv");
		}
		if(!csv.getHeaders().contains("oldValue") || !csv.getHeaders().contains("newValue")){
			throw new IllegalArgumentException("Provided csv must contain oldValue and newValue headers");
		}
		return csv;
	}

	private Serializable processPropertyValue(String value) {
		if(mode.equals(Mode.ReplaceToken)) {
		return value.replace(searchToken, replaceToken);
		} else if (mode.equals(Mode.ReplaceMapping)) {
			List<Map<String, String>> filtered = csv.getLines().stream().
					filter((l) -> !l.get("oldValue").isEmpty()).
					filter((l) -> l.get("oldValue").trim().equals(value.trim())).collect(Collectors.toList());
			if(filtered.size() == 0){
				logger.warn("No mapping found for '" + value + "'! Check your csv data if this value should be mapped");
				return value;
			}
			if(filtered.size() == 1){
				return filtered.get(0).get("newValue");
			}
			logger.warn("Multiple matches for value '" + value + "'! No mapping applied. Make sure your csv data is valid");
			return value;
		}
		return null;
	}

	private String prepareParam(JobExecutionContext context, String param, boolean required) {
		String value = (String) context.getJobDetail().getJobDataMap().get(param);
		if(value==null && required) {
			throwMissingParam(param);
		}
		return value;

	}

	private void throwMissingParam(String param) {
		throw new IllegalArgumentException("Missing required parameter(s) '" + param + "'");
	}

	public void run() {

	}

	@Override
	public Class[] getJobClasses() {
		return allJobs;
	}
}
