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
package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.helper.NodeRunner;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.edu_sharing.service.lifecycle.PersonDeleteOptions;
import org.edu_sharing.service.lifecycle.PersonDeleteResult;
import org.edu_sharing.service.lifecycle.PersonReport;
import org.edu_sharing.service.nodeservice.RecurseMode;
import org.edu_sharing.service.util.CSVTool;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;

import java.io.BufferedReader;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@JobDescription(
		description = "Delete persons based on a specific logic",
		tags = {JobDescription.JobTag.DeletePersonJob}
)
public abstract class DeletePersonJobCustomImpl extends AbstractJobMapAnnotationParams{
	protected Logger logger = Logger.getLogger(DeletePersonJobCustomImpl.class);
	// must be declared in the target class
	// @JobFieldDescription(description = "the authorities to delete")
	// List<String> authorities;

	@Override
	protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		PersonReport report=new PersonReport();
		try {
			report.results = AuthenticationUtil.runAsSystem(() ->
					((List<String>)this.getClass().getDeclaredField("authorities").get(this)).stream().map(this::deleteAuthority).collect(Collectors.toList())
			);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		storeJobResultData(report);
	}

	/**
	 * override this method and return delete information
	 * @param authority
	 */
	protected abstract PersonDeleteResult deleteAuthority(String authority);
}
