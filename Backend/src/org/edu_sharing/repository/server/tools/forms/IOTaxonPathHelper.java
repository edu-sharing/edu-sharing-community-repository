package org.edu_sharing.repository.server.tools.forms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.fileupload.FileItem;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValueKatalog;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.client.tools.forms.CCForms;
import org.edu_sharing.repository.server.MCAlfrescoBaseClient;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.tools.metadataset.MetadataReader;
import org.edu_sharing.repository.server.tools.metadataset.TaxonPathTool;

/**
 * 
 * @author rudi
 * 
 *         This class is used to safe parent values of an taxonid to make search
 *         and faccette counting possible i.e. ddc: 530 = Physik should be found
 *         and counted for value 500 (Naturwissenschaften)
 * 
 */
public class IOTaxonPathHelper extends HelperAbstract {

	Logger logger = Logger.getLogger(IOTaxonPathHelper.class);
	static List<MetadataSetValueKatalog> taxonKatalog = null;

	@Override
	public HashMap<String, Object> execute(HashMap<String, Object> params, HashMap<String, String> authenticatioInfo) {

		String nodeId = (String) params.get(CCConstants.NODEID);
		List items = (List) params.get("ITEMS");
		List<FileItem> fileItems = getFileItems(
				CCForms.getFormEleNameByProp(CCConstants.CCM_TYPE_IO, CCConstants.CCM_PROP_IO_REPL_TAXON_ID), items);

		String repositoryId = (String) params.get(CCConstants.REPOSITORY_ID);

		List<MetadataSetValueKatalog> tc = IOTaxonPathHelper.getTaxonKatalog();

		try {
			if (tc != null) {

				if (fileItems != null && fileItems.size() > 0) {
					for (FileItem fi : fileItems) {
						String taxonId = fi.getString();
						if (taxonId != null) {
							// for solr facette counting also save the parent
							// values
							HashSet<String> uniqueTaxonIdsWithParent = new HashSet<String>();
							uniqueTaxonIdsWithParent.add(taxonId);
							uniqueTaxonIdsWithParent.addAll(new TaxonPathTool().getTaxonParentIds(tc, taxonId));

							MCAlfrescoBaseClient repoClient = (MCAlfrescoBaseClient) RepoFactory.getInstance(
									repositoryId, authenticatioInfo);
							HashMap props = new HashMap();
							props.put(CCConstants.CCM_PROP_IO_REPL_TAXON_ID_PATH, new ArrayList(
									uniqueTaxonIdsWithParent));
							repoClient.updateNode(nodeId, props);
						}
					}
				}

			}
		} catch (Throwable e) {
			logger.error(e.getMessage(), e);
		}

		return null;
	}

	public static List<MetadataSetValueKatalog> getTaxonKatalog() {

		if (taxonKatalog == null) {
			String file = RepoFactory
					.getEdusharingProperty(CCConstants.EDU_SHARING_PROPERTIES_PROPERTY_TAXONID_VALUESPACEFILE);

			if (file != null) {
				String i18nPrefix = RepoFactory
						.getEdusharingProperty(CCConstants.EDU_SHARING_PROPERTIES_PROPERTY_TAXONID_VALUESPACE_I18N_PREFIX);

				try {
					MetadataReader metadataReader = new MetadataReader();
					taxonKatalog = metadataReader.getValuespace(file, "org.edu_sharing.metadataset.valuespaces_i18n",
							i18nPrefix, "{http://www.campuscontent.de/model/1.0}taxonid");
				} catch (Throwable e) {
					e.printStackTrace();
				}

			}
		}

		return taxonKatalog;
	}

}
