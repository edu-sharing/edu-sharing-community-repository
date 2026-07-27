package org.edu_sharing.service.usage;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;
import org.edu_sharing.repository.client.tools.CCConstants;

@Data
public class Usage implements Serializable {
    // type of the usage. Default is Direct (directly associated to the node)
	// Indirect = The usage is from an other node which is related to this
	// (e.g. a collection reference which has an usage)
    public enum Type{
    	DIRECT, INDIRECT
	}

	private Type type = Type.DIRECT;

	private String appUser;
    private String appUserMail;

	private String courseId;
	private String courseTitle;

    private Integer distinctPersons;

    private Calendar fromUsed;
    private Calendar toUsed;

    private String lmsId;

    private String nodeId;
    private String parentNodeId;

    private Integer usageCounter;

    private String usageVersion;
    private String usageXmlParams;

    private String resourceId;

    private String guid;

    private Date created;
    private Date modified;

	public Map<String,String> toMap(){
		Map<String,String> result = new HashMap<>();
		result.put(CCConstants.CCM_PROP_USAGE_APPID, this.getLmsId());
		result.put(CCConstants.CCM_PROP_USAGE_APPUSER, this.getAppUser());
		result.put(CCConstants.CCM_PROP_USAGE_APPUSERMAIL, this.getAppUserMail());
		if(this.getUsageCounter() != null) result.put(CCConstants.CCM_PROP_USAGE_COUNTER, this.getUsageCounter().toString());
		result.put(CCConstants.CCM_PROP_USAGE_COURSEID, this.getCourseId());
		result.put(CCConstants.CCM_PROP_USAGE_COURSETITLE, this.getCourseTitle());
		if( this.getFromUsed() != null)	result.put(CCConstants.CCM_PROP_USAGE_FROM, this.getFromUsed().toString());
		result.put(CCConstants.CCM_PROP_USAGE_GUID, this.getGuid());
		if(this.getDistinctPersons() != null) result.put(CCConstants.CCM_PROP_USAGE_MAXPERSONS, this.getDistinctPersons().toString());
		result.put(CCConstants.CCM_PROP_USAGE_PARENTNODEID, this.getParentNodeId());
		result.put(CCConstants.CCM_PROP_USAGE_RESSOURCEID, this.getResourceId());
		if(this.getToUsed() != null) result.put(CCConstants.CCM_PROP_USAGE_TO, this.getToUsed().toString());
		result.put(CCConstants.CCM_PROP_USAGE_VERSION, this.getUsageVersion());
		result.put(CCConstants.CCM_PROP_USAGE_XMLPARAMS, this.getUsageXmlParams());

		return result;
	}

}
