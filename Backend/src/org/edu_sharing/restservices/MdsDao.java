package org.edu_sharing.restservices;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.edu_sharing.repository.client.rpc.metadataset.MetadataSet;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetBaseProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetFormsForm;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetFormsPanel;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetFormsProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetList;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetListProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetModelProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetModelType;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueries;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQuery;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetQueryProperty;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetValueKatalog;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetView;
import org.edu_sharing.repository.client.rpc.metadataset.MetadataSetViewProperty;
import org.edu_sharing.repository.client.rpc.metadataset.Validator;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.RepoFactory;
import org.edu_sharing.repository.server.authentication.Context;
import org.edu_sharing.restservices.mds.v1.model.Suggestions;
import org.edu_sharing.restservices.shared.Mds;
import org.edu_sharing.restservices.shared.MdsDesc;
import org.edu_sharing.restservices.shared.MdsForm;
import org.edu_sharing.restservices.shared.MdsForm.MdsFormPanel;
import org.edu_sharing.restservices.shared.MdsForm.MdsFormProperty;
import org.edu_sharing.restservices.shared.MdsForm.MdsFormPropertyParameter;
import org.edu_sharing.restservices.shared.MdsForm.MdsFormPropertyValue;
import org.edu_sharing.restservices.shared.MdsList;
import org.edu_sharing.restservices.shared.MdsList.MdsListProperty;
import org.edu_sharing.restservices.shared.MdsList.MdsListPropertyParameter;
import org.edu_sharing.restservices.shared.MdsList.MdsListPropertyValue;
import org.edu_sharing.restservices.shared.MdsQueries;
import org.edu_sharing.restservices.shared.MdsQueries.MdsQuery;
import org.edu_sharing.restservices.shared.MdsQueries.MdsQueryProperty;
import org.edu_sharing.restservices.shared.MdsQueries.MdsQueryPropertyParameter;
import org.edu_sharing.restservices.shared.MdsQueries.MdsQueryPropertyValue;
import org.edu_sharing.restservices.shared.MdsRef;
import org.edu_sharing.restservices.shared.MdsType;
import org.edu_sharing.restservices.shared.MdsType.MdsProperty;
import org.edu_sharing.restservices.shared.MdsView;
import org.edu_sharing.restservices.shared.MdsView.MdsViewProperty;
import org.edu_sharing.restservices.shared.MdsView.MdsViewPropertyParameter;
import org.edu_sharing.restservices.shared.MdsView.MdsViewPropertyValue;
import org.edu_sharing.service.suggest.SuggestDAO;
import org.edu_sharing.service.suggest.SuggestDAOFactory;

import com.google.gwt.user.client.ui.SuggestOracle;

public class MdsDao {

	public static final String DEFAULT = "-default-";

	public static List<MdsDesc> getAllMdsDesc(RepositoryDao repoDao) throws DAOException {

		try {
			
			List<MdsDesc> mdss = new ArrayList<MdsDesc>();
			for (MetadataSet mds : RepoFactory.getMetadataSetsForRepository(repoDao.getId()).getMetadataSets()) {
	
				MdsDesc item = new MdsDesc();
				
				MdsRef ref = new MdsRef();
				ref.setRepo(repoDao.getId());
				ref.setId(mds.getId());
				
				item.setRef(ref);
				item.setDefaultMds(CCConstants.metadatasetdefault_id.equals(mds.getId()));
				
				mdss.add(item);
			}
	
			return mdss;
			
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
			
	}

	public static MdsDao getMds(RepositoryDao repoDao, String mdsId) throws DAOException {

		try {
			
			if (DEFAULT.equals(mdsId)) {
				mdsId = CCConstants.metadatasetdefault_id;
			}
	
			MetadataSet mds = RepoFactory.getMetadataSetsForRepository(repoDao.getId()).getMetadataSetById(mdsId);
			
			if (mds == null) {
				throw new DAOMissingException(new IllegalArgumentException(mdsId));
			}
			
			String locale = (String) Context.getCurrentInstance().getRequest().getSession().getAttribute(CCConstants.AUTH_LOCALE);
			
			return new MdsDao(repoDao, mds, locale);
			
		} catch (Throwable t) {
			throw DAOException.mapping(t);
		}
		
	}

	private final RepositoryDao repoDao;	
	private final MetadataSet mds;
	private final String locale;
	
	private MdsDao(RepositoryDao repoDao, MetadataSet mds, String locale) {
		this.repoDao = repoDao;		
		this.mds = mds;		
		this.locale = locale;
	}

	public Mds asMds() {
		
    	Mds data = new Mds();
    	
    	data.setRef(getRef());
    	data.setTypes(getTypes());
    	data.setForms(getForms());
    	data.setLists(getLists());
    	data.setViews(getViews());
    	data.setQueries(getQueries());
    	
    	return data; 
	}

	private List<MdsType> getTypes() {
		List<MdsType> result = new ArrayList<MdsType>();
		for (MetadataSetModelType type : this.mds.getMetadataSetModelTypes()) {
			result.add(getType(type));
		}
		return result;		
	}
	
	private MdsType getType(MetadataSetModelType mdsType) {
		
		MdsType result = new MdsType();
		
		result.setType(mdsType.getType());
		
		List<MdsProperty> properties = new ArrayList<MdsProperty>();		
		for (MetadataSetModelProperty property : mdsType.getProperties()) {
			
			MdsProperty prop = new MdsProperty();
			prop.setName(property.getName());
			prop.setType(property.getDatatype());
			prop.setDefaultValue(property.getDefaultValue());
			prop.setProcesstype(property.getProcesstype());
			prop.setKeyContenturl(property.getKeyContenturl());
			prop.setConcatewithtype(property.getConcatewithtype());
			prop.setMultiple(property.getMultiple());
			prop.setCopyFrom(property.getCopyprop());
			
			properties.add(prop);
		}
		
		result.setProperties(properties);
		
		return result;
	}
	
	private List<MdsForm> getForms() {
		List<MdsForm> result = new ArrayList<MdsForm>();
		for (MetadataSetFormsForm metadataSetForm : this.mds.getMetadataSetForms()) {
			result.add(getForm(metadataSetForm));
		}
		return result;
	}
	
	private MdsForm getForm(MetadataSetFormsForm metadataSetForm) {

		MdsForm result = new MdsForm();
		
		result.setId(metadataSetForm.getId());

		List<MdsFormPanel> formPanels = new ArrayList<MdsFormPanel>();
		for (MetadataSetFormsPanel panel : metadataSetForm.getPanels()) {
			
			MdsFormPanel formPanel = new MdsFormPanel();
			
			formPanel.setName(panel.getName());
			formPanel.setLabel(panel.getLabel(this.locale));
			formPanel.setMultiUpload(panel.getMultiupload());
			formPanel.setOnCreate(panel.getOncreate());
			formPanel.setOnUpdate(panel.getOnupdate());
			formPanel.setOrder(panel.getOrder());
			formPanel.setStyleName(panel.getStyleName());
			formPanel.setLayout(panel.getLayout());
			
			List<MdsFormProperty> panelProps = new ArrayList<MdsFormProperty>();
			
			for (MetadataSetFormsProperty mdsProperty : panel.getProperties()) {
				
				MdsFormProperty property = new MdsFormProperty();

				if (mdsProperty.getDefaultValues() != null) {
					property.setDefaultValues(Arrays.asList(mdsProperty.getDefaultValues()));
				}
				
				property.setFormHeight(mdsProperty.getFormheight());
				property.setFormLength(mdsProperty.getFormlength());
				
				if (mdsProperty.getLabel() != null) {
					property.setLabel(mdsProperty.getLabel().getValue(this.locale));
				}
				
				if (mdsProperty.getLabelHint() != null) {
					property.setLabelHint(mdsProperty.getLabelHint().getValue(this.locale));
				}
				
				property.setMultiple(mdsProperty.getMultiple());
				property.setName(mdsProperty.getName());
				
				if (mdsProperty.getPlaceHolder() != null) {
					property.setPlaceHolder(mdsProperty.getPlaceHolder().getValue(this.locale));
				}
				
				property.setStyleName(mdsProperty.getStyleName());
				property.setStyleNameLabel(mdsProperty.getStyleNameLabel());
				property.setType(mdsProperty.getType());
				property.setWidget(mdsProperty.getWidget());
				
				if (mdsProperty.getWidgetTitle() != null) {
					property.setWidgetTitle(mdsProperty.getWidgetTitle().getValue(this.locale));
				}
				
				if (mdsProperty.getCopyfrom() != null) {		
					
					property.setCopyFrom(Arrays.asList(mdsProperty.getCopyfrom().split(",")));
				}
				
				if (mdsProperty.getValidators() != null) {

					List<String> validators = new ArrayList<String>();
					
					for (Validator validator : mdsProperty.getValidators()) {
						validators.add(validator.getMessageId());
					}
					
					property.setValidators(validators);
				}
				
				List<MdsFormPropertyParameter> propParams = new ArrayList<MdsFormPropertyParameter>();
				for (String paramKey : mdsProperty.getParamKeys()) {
					
					MdsFormPropertyParameter param = new MdsFormPropertyParameter();
					param.setName(paramKey);
					param.setValue(mdsProperty.getParam(paramKey));
					
					propParams.add(param);
				}				
				property.setParameters(propParams);
					
				if (mdsProperty.getValuespace() != null) {
					
					List<MdsFormPropertyValue> values = new ArrayList<MdsFormPropertyValue>();
					
					for (MetadataSetValueKatalog valuespace : mdsProperty.getValuespace()) {
						
						MdsFormPropertyValue value = new MdsFormPropertyValue();
						value.setKey(valuespace.getKey());
						value.setValue(valuespace.getValue(this.locale));
						
						values.add(value);
					}
					
					property.setValues(values);
				}
				
				panelProps.add(property);
			}
			
			formPanel.setProperties(panelProps);
			
			formPanels.add(formPanel);
			
		}
		
		result.setPanels(formPanels);			
		
		return result;
	}
	
	private List<MdsList> getLists() {
		List<MdsList> result = new ArrayList<MdsList>();
		for (MetadataSetList metadataSetList : this.mds.getMetadataSetLists()) {
			result.add(getList(metadataSetList));
		}
		return result;
	}
	
	private MdsList getList(MetadataSetList metadataSetList) {

		MdsList result = new MdsList();
		
		result.setId(metadataSetList.getId());
		
		if (metadataSetList.getLabel() != null) {
			result.setLabel(metadataSetList.getLabel().getValue(this.locale));
		}
		
		List<MdsListProperty> properties = new ArrayList<MdsListProperty>();
		for (MetadataSetListProperty mdsProperty : metadataSetList.getProperties()) {
			
			MdsListProperty property = new MdsListProperty(); 
			
			if (mdsProperty.getDefaultValues() != null) {
				property.setDefaultValues(Arrays.asList(mdsProperty.getDefaultValues()));
			}
			
			property.setFormHeight(mdsProperty.getFormheight());
			property.setFormLength(mdsProperty.getFormlength());
			
			if (mdsProperty.getLabel() != null) {
				property.setLabel(mdsProperty.getLabel().getValue(this.locale));
			}
			
			if (mdsProperty.getLabelHint() != null) {
				property.setLabelHint(mdsProperty.getLabelHint().getValue(this.locale));
			}
			
			property.setMultiple(mdsProperty.getMultiple());
			property.setName(mdsProperty.getName());
			
			if (mdsProperty.getPlaceHolder() != null) {
				property.setPlaceHolder(mdsProperty.getPlaceHolder().getValue(this.locale));
			}
			
			property.setStyleName(mdsProperty.getStyleName());
			property.setStyleNameLabel(mdsProperty.getStyleNameLabel());
			property.setType(mdsProperty.getType());
			property.setWidget(mdsProperty.getWidget());
			
			if (mdsProperty.getWidgetTitle() != null) {
				property.setWidgetTitle(mdsProperty.getWidgetTitle().getValue(this.locale));
			}
			
			if (mdsProperty.getCopyfrom() != null) {		
				
				property.setCopyFrom(Arrays.asList(mdsProperty.getCopyfrom().split(",")));
			}
			
			List<MdsListPropertyParameter> propParams = new ArrayList<MdsListPropertyParameter>();
			for (String paramKey : mdsProperty.getParamKeys()) {
				
				MdsListPropertyParameter param = new MdsListPropertyParameter();
				param.setName(paramKey);
				param.setValue(mdsProperty.getParam(paramKey));
				
				propParams.add(param);
			}				
			property.setParameters(propParams);
				
			if (mdsProperty.getValuespace() != null) {
				
				List<MdsListPropertyValue> values = new ArrayList<MdsListPropertyValue>();
				
				for (MetadataSetValueKatalog valuespace : mdsProperty.getValuespace()) {
					
					MdsListPropertyValue value = new MdsListPropertyValue();
					value.setKey(valuespace.getKey());
					value.setValue(valuespace.getValue(this.locale));
					
					values.add(value);
				}
				
				property.setValues(values);
			}
			
			properties.add(property);
		}
		result.setProperties(properties);
		
		return result;
	}
	
	
	private List<MdsView> getViews() {
		List<MdsView> result = new ArrayList<MdsView>();
		for (MetadataSetView metadataSetView : this.mds.getMetadataSetViews()) {
			result.add(getView(metadataSetView));
		}
		return result;
	}
	
	private MdsView getView(MetadataSetView metadataSetView) {

		MdsView result = new MdsView();
		
		result.setId(metadataSetView.getId());
		
		List<MdsViewProperty> properties = new ArrayList<MdsViewProperty>();
		for (MetadataSetViewProperty mdsProperty : metadataSetView.getProperties()) {
			
			MdsViewProperty property = new MdsViewProperty(); 
			
			if (mdsProperty.getDefaultValues() != null) {
				property.setDefaultValues(Arrays.asList(mdsProperty.getDefaultValues()));
			}
			
			property.setFormHeight(mdsProperty.getFormheight());
			property.setFormLength(mdsProperty.getFormlength());
			
			if (mdsProperty.getLabel() != null) {
				property.setLabel(mdsProperty.getLabel().getValue(this.locale));
			}
			
			if (mdsProperty.getLabelHint() != null) {
				property.setLabelHint(mdsProperty.getLabelHint().getValue(this.locale));
			}
			
			property.setMultiple(mdsProperty.getMultiple());
			property.setName(mdsProperty.getName());
			
			if (mdsProperty.getPlaceHolder() != null) {
				property.setPlaceHolder(mdsProperty.getPlaceHolder().getValue(this.locale));
			}
			
			property.setStyleName(mdsProperty.getStyleName());
			property.setStyleNameLabel(mdsProperty.getStyleNameLabel());
			property.setType(mdsProperty.getType());
			property.setWidget(mdsProperty.getWidget());
			
			if (mdsProperty.getWidgetTitle() != null) {
				property.setWidgetTitle(mdsProperty.getWidgetTitle().getValue(this.locale));
			}
			
			if (mdsProperty.getCopyfrom() != null) {		
				
				property.setCopyFrom(Arrays.asList(mdsProperty.getCopyfrom().split(",")));
			}
			
			List<MdsViewPropertyParameter> propParams = new ArrayList<MdsViewPropertyParameter>();
			for (String paramKey : mdsProperty.getParamKeys()) {
				
				MdsViewPropertyParameter param = new MdsViewPropertyParameter();
				param.setName(paramKey);
				param.setValue(mdsProperty.getParam(paramKey));
				
				propParams.add(param);
			}				
			property.setParameters(propParams);
				
			if (mdsProperty.getValuespace() != null) {
				
				List<MdsViewPropertyValue> values = new ArrayList<MdsViewPropertyValue>();
				
				for (MetadataSetValueKatalog valuespace : mdsProperty.getValuespace()) {
					
					MdsViewPropertyValue value = new MdsViewPropertyValue();
					value.setKey(valuespace.getKey());
					value.setValue(valuespace.getValue(this.locale));
					
					values.add(value);
				}
				
				property.setValues(values);
			}			
			properties.add(property);
		}
		result.setProperties(properties);
		
		return result;
	}

	private MdsQueries getQueries() {

		MdsQueries result = new MdsQueries();
		
		MetadataSetQueries metadataSetQueries = this.mds.getMetadataSetQueries();
		
		result.setBaseQuery(metadataSetQueries.getBasequery());
		
		List<MdsQuery> queries = new ArrayList<MdsQuery>();
		for (MetadataSetQuery metadataSetQuery : metadataSetQueries.getMetadataSetQueries()) {
			
			MdsQuery query = new MdsQuery();
			query.setCriteriaboxid(metadataSetQuery.getCriteriaboxid());
			query.setHandlerclass(metadataSetQuery.getHandlerclass());
			query.setJoin(metadataSetQuery.getJoin());
			
			if (metadataSetQuery.getLabel() != null) {
				query.setLabel(metadataSetQuery.getLabel().getValue(this.locale));
			}
			
			query.setLayout(metadataSetQuery.getLayout());
			query.setStatement(metadataSetQuery.getStatement());
			query.setStylename(metadataSetQuery.getStylename());
			query.setWidget(metadataSetQuery.getWidget());
			
			List<MdsQueryProperty> properties = new ArrayList<MdsQueryProperty>();
			for (MetadataSetQueryProperty mdsProperty : metadataSetQuery.getProperties()) {
				
				MdsQueryProperty property = new MdsQueryProperty();
			
				if (mdsProperty.getDefaultValues() != null) {
					property.setDefaultValues(Arrays.asList(mdsProperty.getDefaultValues()));
				}
				
				property.setFormHeight(mdsProperty.getFormheight());
				property.setFormLength(mdsProperty.getFormlength());
				property.setInitByGetParam(mdsProperty.getInit_by_get_param());
				
				if (mdsProperty.getLabel() != null) {
					property.setLabel(mdsProperty.getLabel().getValue(this.locale));
				}
				
				if (mdsProperty.getLabelHint() != null) {
					property.setLabelHint(mdsProperty.getLabelHint().getValue(this.locale));
				}
				
				property.setMultiple(mdsProperty.getMultiple());
				property.setName(mdsProperty.getName());
				
				if (mdsProperty.getPlaceHolder() != null) {
					property.setPlaceHolder(mdsProperty.getPlaceHolder().getValue(this.locale));
				}
				
				property.setStatement(mdsProperty.getStatement());				
				property.setStyleName(mdsProperty.getStyleName());
				property.setStyleNameLabel(mdsProperty.getStyleNameLabel());
				property.setToggle(mdsProperty.getToggle());
				property.setType(mdsProperty.getType());
				property.setWidget(mdsProperty.getWidget());
				
				if (mdsProperty.getWidgetTitle() != null) {
					property.setWidgetTitle(mdsProperty.getWidgetTitle().getValue(this.locale));
				}
				
				if (mdsProperty.getCopyfrom() != null) {		
					
					property.setCopyFrom(Arrays.asList(mdsProperty.getCopyfrom().split(",")));
				}
				
				if (mdsProperty.getValidators() != null) {

					List<String> validators = new ArrayList<String>();
					
					for (Validator validator : mdsProperty.getValidators()) {
						validators.add(validator.getMessageId());
					}
					
					property.setValidators(validators);
				}
				
				List<MdsQueryPropertyParameter> propParams = new ArrayList<MdsQueryPropertyParameter>();
				for (String paramKey : mdsProperty.getParamKeys()) {
					
					MdsQueryPropertyParameter param = new MdsQueryPropertyParameter();
					param.setName(paramKey);
					param.setValue(mdsProperty.getParam(paramKey));
					
					propParams.add(param);
				}				
				property.setParameters(propParams);
					
				if (mdsProperty.getValuespace() != null) {
					
					List<MdsQueryPropertyValue> values = new ArrayList<MdsQueryPropertyValue>();
					
					for (MetadataSetValueKatalog valuespace : mdsProperty.getValuespace()) {
						
						MdsQueryPropertyValue value = new MdsQueryPropertyValue();
						value.setKey(valuespace.getKey());
						value.setValue(valuespace.getValue(this.locale));
						
						values.add(value);
					}
					
					property.setValues(values);
				}
				
				properties.add(property);
			}
			query.setProperties(properties);
			
			queries.add(query);
		}
		result.setQueries(queries);
		
		return result;
	}
	
	public MdsRef getRef() {
		MdsRef ref = new MdsRef();
		ref.setRepo(repoDao.getId());
		ref.setId(mds.getId());
		return ref;
	}

	public MetadataSet getMetadataSet() {
		return this.mds;
	}
	
	public Suggestions suggest(String query, String property, String pattern) throws DAOException {
		MetadataSet mds = getMetadataSet();
		MetadataSetQueries mdsQs = mds.getMetadataSetQueries();
		
		MetadataSetBaseProperty baseProp = null;
		
		String propertyGlobal=CCConstants.getValidGlobalName(property);
		for(MetadataSetQuery mdsQ : mdsQs.getMetadataSetQueries()){
			if(mdsQ.getCriteriaboxid().equals(query)){
				for(MetadataSetBaseProperty prop : mdsQ.getProperties()){
					if(prop.getName().equals(property) || prop.getName().equals(propertyGlobal)){
						baseProp = prop;
						break;
					}
				}
			}
		}
		// TODO: New Mds should fetch this more efficient
		if(baseProp == null){
			for(MetadataSetFormsForm form : mds.getMetadataSetForms()){
				for(MetadataSetFormsPanel panel : form.getPanels()){
					for(MetadataSetFormsProperty prop : panel.getProperties()){
						if(prop.getName().equals(property) || prop.getName().equals(propertyGlobal)){
							baseProp = prop;
							break;
						}
					}
				}
			}
		}

		if(baseProp == null){
			throw new DAOValidationException(
					new IllegalArgumentException("no property found with name " + property));
		}
		
		Suggestions result = new Suggestions();
		ArrayList<Suggestions.Suggestion> suggestionsResult = new ArrayList<Suggestions.Suggestion>();
		result.setValues(suggestionsResult);
		try{
			SuggestDAO dao = SuggestDAOFactory.getSuggestDAO(baseProp);
			ArrayList<? extends SuggestOracle.Suggestion>  suggestions = (ArrayList<? extends SuggestOracle.Suggestion>)dao.query(pattern);
	
			for(SuggestOracle.Suggestion suggest : suggestions){
				Suggestions.Suggestion suggestion = new Suggestions.Suggestion();
				suggestion.setDisplayString(suggest.getDisplayString());
				suggestion.setReplacementString(suggestion.getReplacementString());
				
				if(suggest instanceof org.edu_sharing.repository.client.rpc.Suggestion){
					org.edu_sharing.repository.client.rpc.Suggestion esSuggest = (org.edu_sharing.repository.client.rpc.Suggestion)suggest;
					suggestion.setKey(esSuggest.getKey());
				}
				suggestionsResult.add(suggestion);
			}
			
		}catch(Exception e){
			throw DAOException.mapping(e);
		}
		
		return result;
	}

}
