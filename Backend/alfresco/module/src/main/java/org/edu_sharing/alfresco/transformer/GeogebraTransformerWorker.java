package org.edu_sharing.alfresco.transformer;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.content.transform.ContentTransformerHelper;
import org.alfresco.repo.content.transform.ContentTransformerWorker;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.TransformationOptions;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.log4j.Logger;
import org.edu_sharing.alfresco.action.RessourceInfoExecuter;
import org.edu_sharing.repository.server.tools.ImageTool;
import org.springframework.util.StreamUtils;

import java.io.InputStream;
import java.io.OutputStream;

public class GeogebraTransformerWorker extends ContentTransformerHelper implements ContentTransformerWorker  {

	static Logger logger = Logger.getLogger(GeogebraTransformerWorker.class);

	NodeService nodeService = null;

	@Override
	public String getComments(boolean available) {
		return "geogebra image converter";
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
				if(name.endsWith("geogebra_thumbnail.png")){
					OutputStream os = writer.getContentOutputStream();
					InputStream is = ImageTool.autoRotateImage(zip, ImageTool.MAX_THUMB_SIZE);
					StreamUtils.copy(is,os);
					os.close();
					return;
				}
			}
		}
		catch(Throwable t){
			logger.debug(t.getMessage(),t);
		}
		throw new AlfrescoRuntimeException("No image found in geogebra file");
	}

	public String getVersionString() {
		return "1.0";
	};

	public boolean isAvailable() {
		return true;
	};

	@Override
	public boolean isTransformable(String sourceMimetype, String targetMimetype, TransformationOptions options) {
		return AuthenticationUtil.runAsSystem(() ->
				(sourceMimetype.equals("application/zip") || sourceMimetype.equals("application/octet-stream"))
						&& (MimetypeMap.MIMETYPE_IMAGE_PNG.equals(targetMimetype) || MimetypeMap.MIMETYPE_IMAGE_JPEG.equals(targetMimetype))
						&& RessourceInfoExecuter.CCM_RESSOURCETYPE_GEOGEBRA.equals(nodeService.getProperty(options.getSourceNodeRef(), QName.createQName(RessourceInfoExecuter.CCM_PROP_IO_RESSOURCETYPE)))
		);
	}


	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}


}
