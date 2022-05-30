/*
 * Copyright (C) 2005-2007 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have recieved a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */
package org.edu_sharing.alfresco.action;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

/**
 * Logger action executer.
 * 
 * This action will log a message to the application log file at the level
 * specified.
 * 
 * @author Daniel Rudolph
 */
public class CCPDFActionExecuter extends ActionExecuterAbstractBase {
	/** The logger */
	private static Log logger = LogFactory.getLog("org.edu_sharing.alfresco.CCPDFActionExecuter");

	/** The name of the action */
	public static final String NAME = "cc-pdf-action";

	/**
	 * the node service
	 */
	private NodeService nodeService;

	private ContentService contentService;

	private Image osImage;
	
	private int maxheight = 400;
	private int maxwidth = 400;

	/**
	 * This action will take the log message and log it at the provided log
	 * level.
	 * 
	 * If the log level is not provided the default will be INFO.
	 * 
	 * @see org.alfresco.repo.action.executer.ActionExecuterAbstractBase#executeImpl(org.alfresco.service.cmr.action.Action,
	 *      org.alfresco.service.cmr.repository.NodeRef)
	 */
	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		logger.info("CC Action called:" + actionedUponNodeRef.getId());
		/*
		 * Map props = this.nodeService.getProperties(actionedUponNodeRef); Set
		 * keys = props.keySet(); for (Object key : keys) {
		 * System.out.println("MC Action property key: " +
		 * key.getClass().getSimpleName()); }
		 */
		
		Object obj = nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_CONTENT);
		ContentData content = (ContentData) obj;
		logger.info(content.getContentUrl());
		logger.info(content.getMimetype());
		logger.info(content.getSize());
		ContentReader contentreader = this.contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
		logger.info(contentreader.getMimetype());
		InputStream input = contentreader.getContentInputStream();
		
		

		try {
			
				
				logger.info("Type:" + nodeService.getType(actionedUponNodeRef).toString());
				ContentWriter writer = null;
				NodeRef nodeToWriteTo = null;
				
				if (nodeService.getType(actionedUponNodeRef).toString().equals("{http://www.campuscontent.de/model/1.0}io")) {
					
		
					//ChildAssociationRef childref = nodeService.createNode(actionedUponNodeRef,QName.createQName("{http://www.campuscontent.de/model/1.0}preview"), QName.createQName("{http://www.campuscontent.de/model/1.0}preview") ,ContentModel.TYPE_CONTENT );
					ChildAssociationRef childref = nodeService.createNode(actionedUponNodeRef,QName.createQName("{http://www.campuscontent.de/model/1.0}containspreview"), QName.createQName("{http://www.campuscontent.de/model/1.0}containspreview") ,QName.createQName("{http://www.campuscontent.de/model/1.0}preview") );
					nodeToWriteTo = childref.getChildRef();
					writer = contentService.getWriter(nodeToWriteTo, ContentModel.PROP_CONTENT, true);
					

				}
				if(nodeService.getType(actionedUponNodeRef).toString().equals("{http://www.campuscontent.de/model/1.0}preview")){
					nodeToWriteTo = actionedUponNodeRef;
					writer = contentService.getWriter(actionedUponNodeRef, ContentModel.PROP_CONTENT, true);
				}
				
				if(writer != null){
					writer.setMimetype("image/jpeg");
					OutputStream out = writer.getContentOutputStream();
					logger.info("writing to preview start");
					boolean result = false;
					String mimeType = contentreader.getMimetype();
					if (mimeType.equals("application/pdf")) {
						ArrayList<BufferedImage> imageList = pdf2Image(input);
						result = ImageIO.write(imageList.get(0), "jpeg", out);
					}
					if(mimeType.equals("image/pjpeg") || mimeType.equals("image/jpeg") || mimeType.equals("image/png") || mimeType.equals("image/x-png") || mimeType.equals("image/tiff") || mimeType.equals("image/gif")){
						ImageInputStream iIS = ImageIO.createImageInputStream(input);
						BufferedImage bi = ImageIO.read(iIS);
						BufferedImage scaledImg = this.scaleImage(bi);
						result = ImageIO.write(scaledImg, "jpeg", out);
					}
					System.out.println("6");
					out.close();
					logger.info("wrote to preview result:" + result);
					System.out.println("7");
					//setting Name
					nodeService.setProperty(nodeToWriteTo, ContentModel.PROP_NAME,"Vorschau.jpg"+System.currentTimeMillis());
					System.out.println("8");
					nodeService.setProperty(nodeToWriteTo, ContentModel.PROP_TITLE,"Vorschau"+System.currentTimeMillis());
					System.out.println("11");
				}
				
				
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

	}

	/**
	 * @see org.alfresco.repo.action.ParameterizedItemAbstractBase#addParameterDefinitions(java.util.List)
	 */
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		// Specify the parameters
		// paramList.add(new ParameterDefinitionImpl(PARAM_LOG_MESSAGE,
		// DataTypeDefinition.TEXT, true,
		// getParamDisplayLabel(PARAM_LOG_MESSAGE)));
		// paramList.add(new ParameterDefinitionImpl(PARAM_LOG_LEVEL,
		// DataTypeDefinition.TEXT, false,
		// getParamDisplayLabel(PARAM_LOG_LEVEL)));
	}

	/**
	 * Set the node service
	 * 
	 * @param nodeService
	 *            the node service
	 */
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	/**
	 * Set the content service
	 * 
	 * @param contentService
	 *            the content service
	 */
	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	/**
	 * Helper enum to differentiate log levels
	 */
	private enum LogLevel {
		DEBUG, ERROR, FATAL, INFO, WARN, TRACE
	}

	/**
	 * @author Farrukh Aziz
	 * @email desparado482@...
	 * @purpose To convert a pdf file to corresponding jpg images.
	 * @classpath PDFBox-0.7.3.jar;FontBox-0.1.0-dev.jar;bcmail-jdk14-132.jar;bcprov-jdk14-132.jar\;
	 *            lucene-core-2.0.0.jar;lucene-demos-2.0.0.jar;ant.jar;checkstyle-all-4.2.jar;junit.jar
	 *            checkstyle-all-4.2.jar: nicht mit alfresco wbapp nutzen da
	 *            sonst Klassenkonflikte auftreten
	 * @param inputFilePath
	 * @param outputPrefix
	 * @throws IOException
	 */
	public static void pdf2Jpg(InputStream _inputStream, String outputPrefix) throws IOException {
		String imageType = "jpg";
		PDDocument document = null;
		try {

			document = PDDocument.load(_inputStream);

			if (document.isEncrypted()) {
				// Could include decryption capability with password provided.
				// Check org.pdfbox.PDFToImage.java in source.
				System.err.println("Please remove password from PDF file");
			} else {
				List pages = document.getDocumentCatalog().getAllPages();
				for (int i = 0; i < pages.size(); i++) {
					ImageOutputStream output = null;
					ImageWriter imageWriter = null;
					try {
						PDPage page = (PDPage) pages.get(i);
						BufferedImage image = page.convertToImage();
						String fileName = outputPrefix + (i + 1) + "." + imageType;
						output = ImageIO.createImageOutputStream(new File(fileName));

						boolean foundWriter = false;
						Iterator writerIter = ImageIO.getImageWritersByFormatName(imageType);
						while (writerIter.hasNext() && !foundWriter) {
							try {
								imageWriter = (ImageWriter) writerIter.next();
								ImageWriteParam writerParams = imageWriter.getDefaultWriteParam();
								if (writerParams.canWriteCompressed()) {
									writerParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
									writerParams.setCompressionQuality(1.0f);
								}

								imageWriter.setOutput(output);
								imageWriter.write(null, new IIOImage(image, null, null), writerParams);
								foundWriter = true;
							} catch (IIOException io) {
								// ignore exception
							} finally {
								if (imageWriter != null) {
									imageWriter.dispose();
								}
							}
						}
						if (!foundWriter) {
							throw new RuntimeException("No writer found for jpg.");
						}
					} finally {
						if (output != null) {
							output.flush();
							output.close();
						}
					}
				}
			}
		} finally {
			if (document != null) {
				document.close();
			}
		}
	}

	public ArrayList<BufferedImage> pdf2Image(InputStream _inputStream) throws IOException {
		String imageType = "jpg";
		PDDocument document = null;
		ArrayList<BufferedImage> result = new ArrayList<BufferedImage>();
		try {

			document = PDDocument.load(_inputStream);

			if (document.isEncrypted()) {
				// Could include decryption capability with password provided.
				// Check org.pdfbox.PDFToImage.java in source.
				System.err.println("Please remove password from PDF file");
			} else {
				List pages = document.getDocumentCatalog().getAllPages();
				//only first page:
				if(pages != null && pages.size() > 0){
					ImageOutputStream output = null;
					ImageWriter imageWriter = null;
					try {
						PDPage page = (PDPage) pages.get(0);
						
						BufferedImage image = page.convertToImage();
						image = scaleImage(image);
						result.add(image);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				//all pages in future:
				/*
				for (int i = 0; i < pages.size(); i++) {
					ImageOutputStream output = null;
					ImageWriter imageWriter = null;
					try {
						PDPage page = (PDPage) pages.get(i);
						
						BufferedImage image = page.convertToImage();
						image = scaleImage(image);
						result.add(image);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}*/
			}
		} finally {
			if (document != null) {
				document.close();
			}
		}
		return result;

	}
	public BufferedImage scaleImage(BufferedImage _img){
		BufferedImage bi = _img;
		
		if(bi.getHeight() < maxheight && bi.getWidth() < maxwidth) return bi;
		
        int scaleFactor = 1;
        if ((bi.getHeight() - maxheight) <= (bi.getWidth() - maxwidth)) {
            scaleFactor = (maxwidth * 100 / bi.getWidth());
        } else {
            scaleFactor = (maxheight * 100 / bi.getHeight());
        }
        logger.info("scaleFactor:" + scaleFactor);
        Image tmpImg = bi.getScaledInstance((bi.getWidth() * scaleFactor) / 100,
                (bi.getHeight() * scaleFactor) / 100, Image.SCALE_SMOOTH);
         
        BufferedImage scaledImg = new BufferedImage(tmpImg.getWidth(null), tmpImg.getHeight(null),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = scaledImg.createGraphics();
        g2d.drawImage(tmpImg, null, null);
        g2d.dispose();
        return scaledImg;
	}
}
