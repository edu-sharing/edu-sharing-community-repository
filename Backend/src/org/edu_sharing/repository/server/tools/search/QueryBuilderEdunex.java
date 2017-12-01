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
package org.edu_sharing.repository.server.tools.search;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQuery;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;
import org.edu_sharing.repository.client.tools.MimeTypes;

public class QueryBuilderEdunex extends QueryBuilderBase {
	
	/**
	 * SearchWord
	 */
	String searchWord = "";
	
	/**
	 * Taxon
	 */
	String taxon_id = "";
	String taxon_entry = null;
	String taxon_keywords = null;

	/**
	 * User
	 */
	String user = null;
	
	/**
	 * Date From-To
	 */
	String isoDateFrom = null;
	String isoDateTo = null;
	
	/**
	 * Mimetypes
	 */
	String[] mimetypes = null;
	
	String adressat = null;
	String learningResourceType = null;
	String arixTyp = null;
	
	@Override
	public String getSearchString() {
		
		String queryString = "<search fields='gebtext,adressattext,text,titel,typ,datum,sprache,lernziele,herausgeber,autor,produ,schlag,mmdatei'>" +
		"${conditions}" +
		"</search>";
		
		String conditions = "";
		
		if(searchWord != null && !searchWord.trim().equals("")){
			conditions = conditions + "<condition operator=\"AND\" field='text_fields'>" + searchWord + "</condition>";
		}
		
		if(taxon_id !=null && !taxon_id.trim().equals("")){
			conditions = conditions + "<condition operator=\"OR\" field='geb'>" + taxon_id + "</condition>";
		}
		
		if(adressat != null && !adressat.trim().equals("")){
			conditions = conditions + "<condition operator=\"OR\" field='adressat'>" + adressat + "</condition>";
		}
		if(learningResourceType != null && !learningResourceType.trim().equals("")){
			conditions = conditions + "<condition operator=\"OR\" field='typ'>" + learningResourceType + "</condition>";
		}
		
		if(taxon_entry != null && !taxon_entry.trim().equals("")){
			conditions = conditions + "<condition field='geb'>" + taxon_entry + "</condition>";
		}
		
		if(taxon_keywords != null && !taxon_keywords.trim().equals("")){
			conditions = conditions + "<condition field='schlag'>" + taxon_keywords + "</condition>";
		}
		
		if(user != null && !user.trim().equals("")){
			conditions = conditions + "<condition field='produ'>" + user + "</condition>";
		}
		
		if(mimetypes != null && mimetypes.length > 0){
			String[] edunexTypes = getEdunexTypesByMime(mimetypes);
			String typeString = null;
			for(String type:edunexTypes){
				if(typeString == null){
					typeString = type;
				}else{
					typeString = typeString + " " +type;
				}
			}
			
			if(typeString != null){
				conditions = conditions + "<condition field='typ' operator='OR'>" + typeString + "</condition>";
			}
		}
		
		if(arixTyp != null && arixTyp.trim().length() > 0){
			conditions = conditions + "<condition field='typ' operator='OR'>" + arixTyp + "</condition>";
		}
		
		if(conditions != null && !conditions.trim().equals("")){
			queryString = queryString.replace("${conditions}", conditions);
			return queryString;
		}
		
		return null;
	}
	@Override
	public void setContentKind(String[] contentkind) {
	}
	
	public void setSearchMimeTypes(String[] mimetypes) {
		this.mimetypes = mimetypes;
	}
	
	public void setSearchPeriod(String isoDateFrom, String isoDateTo) {
		this.isoDateFrom = isoDateFrom;
		this.isoDateTo = isoDateTo;
	}
	
	@Override
	public void setSearchWord(String searchWord) {
		this.searchWord = searchWord;
	}
	
	public void setTaxon(String id, String entry, String keywords) {
		this.taxon_id = id;
		this.taxon_entry = entry;
		this.taxon_keywords = keywords;
	}
	
	public void setUser(String user) {
		this.user = user;
	}
	
	private String[] getEdunexTypesByMime(String[] mimeTypes){
		ArrayList<String> edunexList = new ArrayList<String>();
		
		/*
		 * 	19:Bild
			29:Audio
			49:Video
			55:didaktisches Medium (DVD mit Navigation)
			56:Medienmodul (technisch das gleiche wie 49: eben eine Filmdatei)
			69:Programm
			79:Dokument 
		 * */
		
		String audio = "Audio";
		String dokument = "Dokument";
		String bild = "Bild";
		List video = Arrays.asList(new String[]{"Video","Medienmodul"});
		
		for(String mimeType: mimeTypes){
			
			if(Arrays.asList(MimeTypes.mime_audio).contains(mimeType)){
				if(!edunexList.contains(audio)) edunexList.add(audio);
			}
			if(Arrays.asList(MimeTypes.mime_doc).contains(mimeType)){
				if(!edunexList.contains(dokument)) edunexList.add(dokument);
			}
			if(Arrays.asList(MimeTypes.mime_pic).contains(mimeType)){
				if(!edunexList.contains(bild)) edunexList.add(bild);
			}
			if(Arrays.asList(MimeTypes.mime_video).contains(mimeType)){
				if(!edunexList.contains(video)) edunexList.addAll(video);
			}
		}
		
		return edunexList.toArray(new String[edunexList.size()]);
		/*@TODO
		'internet-seite',
		'programm',
		'scorm'*/ 
		
	}
	
	@Override
	public void setMetadataSetQuery(String repositoryId, String metadataSetId, String standaloneMetadataSetName, HashMap<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> map) {
		
		if(map != null){
			for(Map.Entry<MetadataSetQuery, HashMap<MetadataSetQueryProperty, String[]>> entry:map.entrySet()){
				MetadataSetQuery mdsq = entry.getKey();
				
				if(mdsq != null && mdsq.getCriteriaboxid() != null){
					String criteriaboxid = mdsq.getCriteriaboxid();
					
					//mimetypes
					if(criteriaboxid.equals("filetypes") ){
						String audio = getValue("audio",entry.getValue())[0];
						String video = getValue("video",entry.getValue())[0];
						String docs = getValue("docs",entry.getValue())[0];
						String pics = getValue("pics",entry.getValue())[0];
						ArrayList mimetypesList = new ArrayList();
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
							this.setSearchMimeTypes(mimetypes);
						}
					}
					//user
					if(criteriaboxid.equals("user") ){
						String user = getValue("user",entry.getValue())[0];
						if(user != null && !user.equals("")){
							this.setUser(user);
						}
					}
					//taxon
					if(criteriaboxid.equals("keywords") ){
						String taxonEntry = getValue("taxon_entry",entry.getValue())[0];
						String keywords = getValue("classification_keyword",entry.getValue())[0];
						this.setTaxon(null, taxonEntry, keywords);
					}
					
					//quickfix for lvr 110511
					if(criteriaboxid.equals(MetadataSetQuery.DEFAULT_CRITERIABOXID)){
						String[] searchWords = getValue("searchword",entry.getValue());
						if(searchWords != null){
							
							
							if(this.searchWord == null) this.searchWord = "";
							
							for(String sw : searchWords){
								if(sw != null) this.searchWord = this.searchWord + " " + sw;
							}
							if(this.searchWord != null){
								this.searchWord = this.searchWord.trim();
							}
						}
						
						String[] allContext = getValue("{http://www.campuscontent.de/model/1.0}educationalcontext",entry.getValue());
						String educationalContext = (allContext != null && allContext.length > 0) ? allContext[0] : null;
						if(educationalContext != null && !educationalContext.trim().equals("")){
							
							//school
							//highereducation
							//training
							//other
							
							if(educationalContext.equals("school")){
								adressat = "A E";
							}
							if(educationalContext.equals("highereducation")){
								adressat = "BB Q";
							}
							if(educationalContext.equals("training")){
								adressat = "T";
							}
							if(educationalContext.equals("other")){
								adressat = "SO J";
							}
						}
						
						String[] learningResourceTypes = getValue("{http://www.campuscontent.de/model/lom/1.0}learningresourcetype",entry.getValue());
						if(learningResourceTypes != null){
							
							for(String lrt : learningResourceTypes){
								if(lrt != null && lrt.contains("slide")){
									learningResourceType = "10"; //Diareihe
								}
							}
						}
						
						if(this.taxon_id == null) this.taxon_id ="";
						
						String[] taxonIds = getValue("{http://www.campuscontent.de/model/1.0}taxonid",entry.getValue());
						for(String taxonId : taxonIds){
							if(taxonId != null && !taxonId.trim().equals("")){
								
								/**
								 * "Allgemeines, Wissenschaft
								 * 
								 * 'wirtschaft' => '700', 
								 * 'sport' => '600',
								 */
								if(taxonId.contains("0")){
									this.taxon_id = this.taxon_id +" " + "700 600";
								}
								//Philosophie
								if(taxonId.contains("1")){
									this.taxon_id = this.taxon_id+" " + "450";
								}
								//Religion, Religionsphilosophie
								if(taxonId.contains("2")){
									this.taxon_id = this.taxon_id+" " + "520";
								}
								//Sozialwissenschaften, Soziologie
								if(taxonId.contains("3")){
									//mapping auf sachuntericht und 'politik/gemeinschaftskunde''480' 
									this.taxon_id = this.taxon_id+" " + "28010 480";
								}
								//Sprache, Linguistik
								if(taxonId.contains("4")){
									//'spanisch' => '20007' engl:20001 französisch:20002  griechisch 20003 italienisch 20004 latein 20005
									this.taxon_id =  this.taxon_id+" " + "20007 20001 20002 20003 20004 20005";
								}
								/**
								 * Naturwissenschaften
								 * 'biologie' => '080',
									'chemie' => '100',
									'geographie' =>  '220', 
									'informatik' => '320'
									'mathematik' => '380', 
									'physik' => '460', 
		
								 */
								if(taxonId.contains("5")){
									this.taxon_id =  this.taxon_id+" " +"080 100 220 320 380 460";
								}
								//Technik 50005
								if(taxonId.contains("6")){
									this.taxon_id =  this.taxon_id+" " + "50005";
								}
								//Künste, Bildende Kunst allgemein
								/**
								 * Kunst 060  Musik 420
								 */
								if(taxonId.contains("7")){
									this.taxon_id =  this.taxon_id+" " + "060 420";
								}
								/**
								 * Literatur, Rhetorik, Literaturwissenschaft
								 * 'deutsch' => '120', 
								 */
								if(taxonId.contains("8")){
									this.taxon_id = this.taxon_id+" " +"120";
								}
								/**
								 * 'geschichte' => '240',
								 */
								if(taxonId.contains("9")){
									this.taxon_id = this.taxon_id+" " +"240";
								}
							}
						}
						
						/**
						 * without mapping
						 */
						String[] taxonIds2 = getValue("geb",entry.getValue());
						
						if(taxonIds2 != null && taxonIds2.length > 0){
							this.taxon_id = null;
							for(String taxonId : taxonIds2){
								if(this.taxon_id == null){
									this.taxon_id = taxonId;
								}else{
									this.taxon_id +=" "+ taxonId;
								}
							}
						}
						
						this.taxon_id = this.taxon_id.trim();
						
						String[] typ = getValue("typ", entry.getValue());
						if(typ != null && typ.length > 0){
							arixTyp = null;
							for(String t:typ){
								if(arixTyp == null){
									arixTyp = t;
								}else{
									arixTyp += " "+t;
								}
							}
						}
						
						String[] context = getValue("adressat", entry.getValue());
						if(context != null && context.length > 0){
							adressat = null;
							for(String t:context){
								if(adressat == null){
									adressat = t;
								}else{
									adressat += " "+t;
								}
							}
						}
					}
				}
			}
		}
	}
	
	public String[] getValue(String propName, HashMap<MetadataSetQueryProperty, String[]> propValue){
		for(Map.Entry<MetadataSetQueryProperty, String[]> entry: propValue.entrySet()){
			if(propName.equals(entry.getKey().getName())){
				String[] val = entry.getValue();
				if(val != null && val.length > 0 && val[0] != null){
					return val;
				}
			}
		}
		return new String[]{};
	}
	@Override
	public void setAspects(String[] _aspects) {
	}
}
