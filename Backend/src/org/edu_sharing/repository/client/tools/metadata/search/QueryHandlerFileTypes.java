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
package org.edu_sharing.repository.client.tools.metadata.search;

import java.util.ArrayList;
import java.util.HashMap;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQuery;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;
import org.edu_sharing.repository.client.tools.MimeTypes;


public class QueryHandlerFileTypes extends QueryHandlerAbstract {
	
	@Override
	public String getStatement(MetadataSetQuery mdsq, HashMap<MetadataSetQueryProperty, String[]> propValue) {

		String chameleon = getValue("chameleon", propValue);
		String audio = getValue("audio",propValue);
		String video = getValue("video",propValue);
		String docs = getValue("docs",propValue);
		String pics = getValue("pics",propValue);
		
		
		ArrayList<String[]> mimetypesList = new ArrayList<String[]>();
		if (pics != null && pics.equals("true")) {
			mimetypesList.add(MimeTypes.mime_pic);
		}
		if (audio != null && audio.equals("true")) {
			mimetypesList.add(MimeTypes.mime_audio);
		}
		if (docs != null && docs.equals("true")) {
			mimetypesList.add(MimeTypes.mime_doc);
		}
		if (video != null && video.equals("true")) {
			mimetypesList.add(MimeTypes.mime_video);
		}
		String[] mimetypes = MimeTypes.get(mimetypesList);
		
		if (mimetypes != null && mimetypes.length > 0) {
			String mimeprefix = "@cm\\:content.mimetype:";
			String mimepart = "";
			for (int i = 0; i < mimetypes.length; i++) {
				if (i == 0) {
					mimepart += "(" + mimeprefix + "\"" + mimetypes[i] + "\"";
				}
				if (i > 0) {
					mimepart += " OR " + mimeprefix + "\"" + mimetypes[i] + "\"";
				}
				if (i == (mimetypes.length - 1)) {
					if(chameleon != null && chameleon.equals("true")){
						mimepart += " OR @ccm\\:linktype:CMchameleon)";
					}else{
						mimepart += ")";
					}
				}

			}
			return mimepart;
		}else if(chameleon != null && chameleon.equals("true")){
			return "@ccm\\:linktype:CMchameleon";
		}
		
		
		return null;
	}
}
