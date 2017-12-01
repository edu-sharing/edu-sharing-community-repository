package org.edu_sharing.repository.server.tools.metadataset;

import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValueKatalog;

public class TaxonPathTool {

	Logger logger = Logger.getLogger(TaxonPathTool.class);
	
	public HashSet<String> getTaxonParentIds(List<MetadataSetValueKatalog> taxonKatalog, String taxonId){
		HashSet<String> parentIds = new  HashSet<String>();
		
		boolean taxonidfound=false;
		
		MetadataSetValueKatalog parent = null;
		for (MetadataSetValueKatalog cataEntry : taxonKatalog) {
			if(cataEntry.getKey().equals(taxonId)){
				parent = (MetadataSetValueKatalog)cataEntry.getParentValue();
				taxonidfound = true;
				break;
			}
		}
		
		/**
		 * we have some entries that don't exist in valuespace file, so we try to cut the last two digits
		 * and try to find a parent one more time
		 */
		if(!taxonidfound){
			logger.error("could not find an entry in eaf catalog for taxonId:"+taxonId+" will cut the last two digits and try again");
			if(taxonId.length() > 5){
				String newTaxonId = taxonId.substring(0, taxonId.length() - 2);
				for (MetadataSetValueKatalog cataEntry : taxonKatalog) {
					if(cataEntry.getKey().equals(newTaxonId)){
						parent = (MetadataSetValueKatalog)cataEntry.getParentValue();
						taxonidfound = true;
						break;
					}
				}
			}
		}
		
		
		if(parent != null){
			do{
				parentIds.add(parent.getKey());
			}while((parent = (MetadataSetValueKatalog)parent.getParentValue()) != null);
		}
		
		if(!taxonidfound){
			logger.error("still no parent found for:"+taxonId+" without the last two digits");
		}
		
		return parentIds;
	}
	
}
