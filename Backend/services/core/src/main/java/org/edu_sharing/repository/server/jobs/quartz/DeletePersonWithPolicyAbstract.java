package org.edu_sharing.repository.server.jobs.quartz;

import com.google.gson.Gson;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.security.AuthorityType;
import org.apache.log4j.Logger;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.restservices.PersonDao;
import org.edu_sharing.restservices.RepositoryDao;
import org.edu_sharing.service.authority.AuthorityService;
import org.edu_sharing.service.lifecycle.PersonDeleteOptions;
import org.edu_sharing.service.lifecycle.PersonLifecycleService;
import org.edu_sharing.service.lifecycle.PersonReport;
import org.edu_sharing.service.search.SearchService;
import org.edu_sharing.service.search.model.SearchResult;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@JobDescription(description = "Base class to automatically delete persons which are marked for deletion (this job class must be overridden to use it)")
public abstract class DeletePersonWithPolicyAbstract extends AbstractInterruptableJob{

	protected Logger logger = Logger.getLogger(DeletePersonWithPolicyAbstract.class);

    @Autowired
    private AuthorityService authorityService;
    @Autowired
    private SearchService searchService;

	@Override
	public void executeInterruptable(JobExecutionContext context) throws JobExecutionException {
		AuthenticationUtil.runAsSystem(() -> {
			Map<String, String> properties = new HashMap<>();
			properties.put(
					CCConstants.getValidLocalName(CCConstants.CM_PROP_PERSON_ESPERSONSTATUS),
					PersonLifecycleService.PersonStatus.todelete.name());
			try {
				SearchResult<String> authorities = searchService.findAuthorities(
						AuthorityType.USER,
						"*",
						true,
						0,
						Integer.MAX_VALUE,
						null,
						properties
				);
				authorities.getData().stream().filter((a) ->
						// security check
						PersonLifecycleService.PersonStatus.todelete.name().equals(
                                authorityService.getAuthorityProperty(
										a, CCConstants.CM_PROP_PERSON_ESPERSONSTATUS
								)
						)
				).forEach((a) -> {
					try {
						PersonDao personDao = PersonDao.getPerson(RepositoryDao.getHomeRepository(), a);
						PersonDeleteOptions options = shouldDelete(personDao);
						if (options == null) {
							logger.info("No delete configuration for authority " + a + ", won't delete");
							return;
						}
						PersonReport results = new PersonLifecycleService().deletePersons(
								Collections.singletonList(a),
								options
						);
						logger.info("Deleted person " + a + ", " +
								new Gson().toJson(results.results.get(0))
						);
					} catch (Exception e) {
						logger.error(e.getMessage(), e);
					}
				});

			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
			return null;
		});
	}

	/**
	 * override this method
	 * return the delete config for the given user
	 * or return null. In this case, the user won't be deleted
	 */
	protected abstract PersonDeleteOptions shouldDelete(PersonDao personDao);

	public void run() {

	}

}
