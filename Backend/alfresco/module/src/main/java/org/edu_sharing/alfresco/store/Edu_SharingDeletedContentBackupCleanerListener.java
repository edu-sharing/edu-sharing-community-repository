package org.edu_sharing.alfresco.store;

import org.alfresco.repo.content.ContentContext;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.repo.content.cleanup.ContentStoreCleanerListener;
import org.alfresco.repo.content.cleanup.DeletedContentBackupCleanerListener;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Edu_SharingDeletedContentBackupCleanerListener  implements ContentStoreCleanerListener{

	ContentStore allowedSourceStore;
	
	private static Log logger = LogFactory.getLog(DeletedContentBackupCleanerListener.class);
    
    private ContentStore store;
	
	public Edu_SharingDeletedContentBackupCleanerListener() {
		super();
	}
	
    
    public void setAllowedSourceStore(ContentStore allowedSourceStore) {
		this.allowedSourceStore = allowedSourceStore;
	}

    /**
     * Set the store to copy soon-to-be-deleted content into
     *  
     * @param store the deleted content backup store
     */
    public void setStore(ContentStore store)
    {
        this.store = store;
    }

    public void beforeDelete(ContentStore sourceStore, String contentUrl) throws ContentIOException
    {
    	/**
    	 * edu-sharing customization check source store
    	 */
    	
    	logger.error("sourceStore.getClass().getName():" + sourceStore.getClass().getName() + " allowedSourceStore:" + allowedSourceStore.getClass().getName());
    	
    	if(!sourceStore.equals(allowedSourceStore)){
    		return;
		}
    	
        if (store.isContentUrlSupported(contentUrl))
        {
            ContentContext context = new ContentContext(null, contentUrl);
            ContentReader reader = sourceStore.getReader(contentUrl);
            if (!reader.exists())
            {
                // Nothing to copy over
                return;
            }
            // write the content into the target store
            ContentWriter writer = store.getWriter(context);
            // copy across
            writer.putContent(reader);
            // done
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Moved content before deletion: \n" +
                        "   URL:    " + contentUrl + "\n" +
                        "   Source: " + sourceStore + "\n" +
                        "   Target: " + store);
            }
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug(
                        "Content cannot be moved during deletion.  A backup will not be made: \n" +
                        "   URL:    " + contentUrl + "\n" +
                        "   Source: " + sourceStore + "\n" +
                        "   Target: " + store);
            }
        }
    }
	
}
