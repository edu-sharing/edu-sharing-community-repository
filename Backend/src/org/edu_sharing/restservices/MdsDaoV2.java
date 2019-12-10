package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.List;

import org.edu_sharing.metadataset.v2.*;
import org.edu_sharing.metadataset.v2.tools.MetadataHelper;
import org.edu_sharing.metadataset.v2.tools.MetadataSearchHelper;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.restservices.mds.v1.model.*;
import org.edu_sharing.restservices.shared.MdsQueryCriteria;
import org.edu_sharing.restservices.shared.MdsV2;
import org.edu_sharing.service.search.Suggestion;

public class MdsDaoV2 {

	public static final String DEFAULT = "-default-";

	public static List<MetadataSetInfo> getAllMdsDesc(RepositoryDao repoDao) throws Exception {
		return RepoFactory.getMetadataSetsV2ForRepository(repoDao.getId());
	}
	
	public Suggestions getSuggestions(String queryId,String parameter,String value, List<MdsQueryCriteria> criterias) throws DAOException{
		Suggestions result = new Suggestions();
		ArrayList<Suggestions.Suggestion> suggestionsResult = new ArrayList<>();
		result.setValues(suggestionsResult);
		try{
			List<? extends Suggestion>  suggestions =
					MetadataSearchHelper.getSuggestions(this.repoDao.getId(), mds, queryId, parameter, value, criterias);

			for(Suggestion suggest : suggestions){
				Suggestions.Suggestion suggestion = new Suggestions.Suggestion();
				suggestion.setDisplayString(suggest.getDisplayString());
				//suggestion.setReplacementString(suggest.getReplacementString());
				suggestion.setKey(suggest.getKey());
				suggestionsResult.add(suggestion);
			}
			
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
		return result;
	}
	
	public static MdsDaoV2 getMds(RepositoryDao repoDao, String mdsId) throws DAOException {

		try {
			
			MetadataSetV2 mds=MetadataHelper.getMetadataset(repoDao.getApplicationInfo(),mdsId);
			
			if (mds == null) {
				throw new DAOMissingException(new IllegalArgumentException(mdsId));
			}
			
			return new MdsDaoV2(repoDao, mds);
			
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
		
	}

	private final RepositoryDao repoDao;	
	private final MetadataSetV2 mds;
	
	private MdsDaoV2(RepositoryDao repoDao, MetadataSetV2 mds) {
		this.repoDao = repoDao;		
		this.mds = mds;		
	}

	public MdsV2 asMds() {
		
    	MdsV2 data = new MdsV2();
    	
    	data.setName(mds.getName());
    	data.setCreate(mds.getCreate()!=null ? new MdsV2.Create(mds.getCreate()) : null);
    	data.setWidgets(getWidgets());
    	data.setViews(getViews());
    	data.setGroups(getGroups());
    	data.setLists(getLists());
    	data.setSorts(getSorts());

    	return data; 
	}

	private List<WidgetV2> getWidgets() {
		List<WidgetV2> result = new ArrayList<WidgetV2>();
		for (MetadataWidget type : this.mds.getWidgets()) {
			result.add(new WidgetV2(type));
		}
		return result;		
	}
	
	private List<ViewV2> getViews() {
		List<ViewV2> result = new ArrayList<ViewV2>();
		for (MetadataTemplate type : this.mds.getTemplates()) {
			result.add(new ViewV2(type));
		}
		return result;		
	}
	
	private List<GroupV2> getGroups() {
		List<GroupV2> result = new ArrayList<GroupV2>();
		for (MetadataGroup type : this.mds.getGroups()) {
			result.add(new GroupV2(type));
		}
		return result;		
	}
	
	private List<ListV2> getLists() {
		List<ListV2> result = new ArrayList<>();
		for (MetadataList type : this.mds.getLists()) {
			result.add(new ListV2(type));
		}
		return result;		
	}

	private List<SortV2> getSorts() {
		List<SortV2> result = new ArrayList<>();
		for (MetadataSort type : this.mds.getSorts()) {
			result.add(new SortV2(type));
		}
		return result;
	}
	
	public MetadataSetV2 getMds() {
		return mds;
	}
	
}
