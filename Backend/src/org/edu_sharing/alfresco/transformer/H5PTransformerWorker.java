package org.edu_sharing.alfresco.transformer;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.ContentTransformerHelper;
import org.alfresco.repo.content.transform.ContentTransformerWorker;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport.TxnReadState;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.action.RessourceInfoExecuter;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.ImageTool;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.TimeZone;

public class H5PTransformerWorker extends ContentTransformerHelper implements ContentTransformerWorker  {

	static Logger logger = Logger.getLogger(H5PTransformerWorker.class);
	
	NodeService nodeService = null;
	TransactionService transactionService = null;

	@Override
	public String getComments(boolean available) {
		return "h5p image converter";
	}

	@Override
	public void transform(ContentReader reader, ContentWriter writer, TransformationOptions options) throws Exception {
		// check if file contains an image to use as thumbnail
		try {
			ArchiveInputStream zip = RessourceInfoExecuter.getZipInputStream(reader);
			while (true) {
				ArchiveEntry entry = zip.getNextEntry();
				if(entry==null)
					break;
				String name=entry.getName().toLowerCase();
				if(name.startsWith("content/images") && (name.endsWith(".jpg") || name.endsWith(".png"))){
					OutputStream os = writer.getContentOutputStream();
					InputStream is = ImageTool.autoRotateImage(zip, ImageTool.MAX_THUMB_SIZE);
					StreamUtils.copy(is,os);
					os.close();
					return;
				}
			}
		}
		catch(Throwable t){

		}
		throw new AlfrescoRuntimeException("No image found in h5p file");
	}

	public String getVersionString() {
		return "1.0";
	};
	
	public boolean isAvailable() {
		return true;
	};
	
	@Override
	public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options) {
			boolean convert=AuthenticationUtil.runAsSystem(() ->
					sourceMimetype.equals("application/zip")
					&& (MimetypeMap.MIMETYPE_IMAGE_PNG.equals(targetMimetype) || MimetypeMap.MIMETYPE_IMAGE_JPEG.equals(targetMimetype))
					&& RessourceInfoExecuter.CCM_RESSOURCETYPE_H5P.equals(nodeService.getProperty(options.getSourceNodeRef(), QName.createQName(RessourceInfoExecuter.CCM_PROP_IO_RESSOURCETYPE)))
				);
			return convert;
		}


	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setTransactionService(TransactionService transactionService) {
		this.transactionService = transactionService;
	}
	
	
}
