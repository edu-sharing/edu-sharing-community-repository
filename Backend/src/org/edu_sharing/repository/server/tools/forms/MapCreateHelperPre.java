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
package org.edu_sharing.repository.server.tools.forms;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;

public class MapCreateHelperPre implements Helper {
	
	Logger logger = Logger.getLogger(MapCreateHelperPre.class);
	
	public HashMap<String, Object> execute(HashMap<String, Object> params) {
		return null;
	}
	
	@Override
	public FileItem getFileItem(String property, List items) {
		return null;
	}
	
	@Override
	public HashMap<String, Object> execute(HashMap<String, Object> params,
			HashMap<String, String> authenticatioInfo) {
		return null;
	}
	
	public List execute(List items){
		for (int i = 0; i < items.size(); i++) {
			FileItem item = (FileItem) items.get(i);
			if(item.getFieldName().equals(CCConstants.CCM_PROP_MAP_ICON)){
				if(item instanceof DiskFileItem){
					logger.info("disk file Item found");
					DiskFileItem dfi = (DiskFileItem)item;
					byte[] scaledImage = getScaledImage(dfi.get());
					
					if(scaledImage != null){
						logger.info("scaledImage[] length:" +scaledImage.length);
					}else logger.info("scaledImage[] is null");
					
					String tmpDirPath = System.getProperty("java.io.tmpdir");
					try{
						String filename = dfi.getStoreLocation().getAbsolutePath()+".jpg";
						OutputStream out = new FileOutputStream(filename);
						out.write(scaledImage);
						out.close();
						
						logger.info("dfi.isInMemory():"+dfi.isInMemory());
						
						logger.info("dfi.getContentType():"+dfi.getContentType());
						
						logger.info("dfi.getContentType():"+dfi.getContentType());
						
						DiskFileItemFactory dfiFac = new DiskFileItemFactory();
						DiskFileItem newDfi = (DiskFileItem)dfiFac.createItem(dfi.getFieldName(), "image/jpeg", dfi.isFormField(), filename);
											
						newDfi.getOutputStream();
						newDfi.getInputStream();
						
						logger.info("newlength:"+newDfi.get().length);
						logger.info("newDfi name"+newDfi.getFieldName());
						
						items.set(i, newDfi);
						
					}catch(IOException e){
						logger.error(e.getMessage(),e);
					}
				}
				
			}
		}
		
		return items;
	}
	
	public HttpServletRequest execute(HttpServletRequest request){
		Map paramMap = request.getParameterMap();
		logger.info("Param Map Size:"+paramMap.size());
		for (Object key:paramMap.keySet()) {
			logger.info("key:"+key+" value:"+paramMap.get(key));
		}
		return request;
	}
	
	
	public byte[] getScaledImage(byte[] content) {
        byte[] scaledContent = null;
        int maxheight = 50;
        int maxwidth = 50;
        try {
             if (content != null) {
                BufferedImage bi = ImageIO.read(new ByteArrayInputStream(content));
                int scaleFactor = 1;
                if ((bi.getHeight() - maxheight) <= (bi.getWidth() - maxwidth)) {
                    scaleFactor = (maxwidth * 100 / bi.getWidth());
                } else {
                    scaleFactor = (maxheight * 100 / bi.getHeight());
                }
                Image tmpImg = bi.getScaledInstance((bi.getWidth() * scaleFactor) / 100,
                        (bi.getHeight() * scaleFactor) / 100, Image.SCALE_SMOOTH);
                 
                BufferedImage scaledImg = new BufferedImage(tmpImg.getWidth(null), tmpImg.getHeight(null),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = scaledImg.createGraphics();
                g2d.drawImage(tmpImg, null, null);
                g2d.dispose();
                ByteArrayOutputStream os_tmp = new ByteArrayOutputStream();
                ImageIO.write(scaledImg, "jpg", ImageIO.createImageOutputStream(os_tmp));
                scaledContent = os_tmp.toByteArray();
                
            }
        } catch (IOException e) {
           e.printStackTrace();
        }
        return scaledContent;
    }
}
