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

import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class OAIConst {
	
	public static final String PARAM_OAI_BASE_URL = "oai_base_url";
	public static final String PARAM_OAI_METADATA_PREFIX = "oai_metadata_prefix";
	public static final String PARAM_METADATASET_ID = "metadataset_id";
	public static final String PARAM_OAI_SETS = "sets";
	public static final String PARAM_OAI_CATALOG_IDS = "catalogids";
	public static final String PARAM_RECORDHANDLER = "record_handler";
	public static final String PARAM_BINARYHANDLER = "binary_handler";
	public static final String PARAM_PERSISTENTHANDLER = "persistent_handler";
	public static final String PARAM_XMLDATA = "xml_data";
	public static final String PARAM_IMPORTERCLASS = "importer_class";
	public static final String PARAM_USERNAME = "username";
	public static final String PARAM_OAI_IDS = "oai_ids";
	// forcefully update all data, also override any existing ones
	public static final String PARAM_FORCE_UPDATE = "force_update";
	// do not create versions (basically, the node will always stay at 1.0)

	public static final String PARAM_NO_VERSION = "no_version";

	public static final String PARAM_FROM = "from";
	public static final String PARAM_UNTIL = "until";

	public static final String PARAM_PERIOD_IN_DAYS = "period_in_days";

	public static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
}
