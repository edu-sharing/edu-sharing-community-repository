package org.edu_sharing.restservices;

import org.edu_sharing.service.handleservicedoi.DOIServiceException;
import org.edu_sharing.service.handleservicedoi.DOIServiceMissingAttributeException;
import org.edu_sharing.service.handleservicedoi.DOIServiceNotConfiguredException;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DAODOIServiceException extends DAOException{

    DOIServiceException cause;

    public DAODOIServiceException(DOIServiceException t, String nodeId) {
        super(t, nodeId);
        cause = (DOIServiceException) t;
    }

    @Override
    public Map<String, Serializable> getDetails() {
        if(cause instanceof DOIServiceMissingAttributeException){
            return new HashMap<>() {{
                put("property", ((DOIServiceMissingAttributeException)cause).getProperty());
                put("schemaField", ((DOIServiceMissingAttributeException)cause).getSchemaField());
                put("cause",cause.getClass().getSimpleName());
            }};
        }
        if(cause instanceof DOIServiceNotConfiguredException){
            return new HashMap<>() {{
                put("message", "doi service not configured");
                put("cause",cause.getClass().getSimpleName());
            }};
        }
        return new HashMap<>() {{
            put("message", "doi service method failed");
            put("cause",cause.getClass().getSimpleName());
        }};
    }
}
