package org.edu_sharing.repository.server.jobs.quartz;

import org.alfresco.query.PagingRequest;
import org.alfresco.query.PagingResults;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.elasticsearch.common.collect.Map;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.context.ApplicationContext;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

@JobDescription(description = "Log usernames with special chars.")
public class LogUsernamesWithSpecialChars extends AbstractJob {

    protected Logger logger = Logger.getLogger(LogUsernamesWithSpecialChars.class);

    ApplicationContext applicationContext = AlfAppContextGate.getApplicationContext();

    ServiceRegistry serviceRegistry = (ServiceRegistry) applicationContext.getBean(ServiceRegistry.SERVICE_REGISTRY);

    @JobFieldDescription(description = "regex the username must match. predefined are: -whitespace-,-whitespace2-. default is -whitespace-")
    private String regex;

    java.util.Map<String, String> predefinedRegEx = Map.of(
            "-whitespace-", "\\s+",
            "-whitespace2-", "[ \\t\\n\\r\\f]+");

    int pageSize = 100;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String paramRegExp = jobExecutionContext.getJobDetail().getJobDataMap().getString("regex");
        String regExp;
        if (paramRegExp == null || paramRegExp.trim().equals("")) regExp = predefinedRegEx.get("-whitespace-");
        else regExp = (predefinedRegEx.get(paramRegExp) == null) ? paramRegExp : predefinedRegEx.get(paramRegExp);

        try {
            Pattern pattern = Pattern.compile(regExp);
            AuthenticationUtil.runAsSystem(() -> {
                PagingRequest pr = new PagingRequest(pageSize);
                PagingResults<PersonService.PersonInfo> people = null;
                int skipCount = 0;
                do {
                    people = serviceRegistry.getPersonService().getPeople(null, null, null, pr);
                    for (PersonService.PersonInfo personInfo : people.getPage()) {

                        if (pattern.matcher(personInfo.getUserName()).find()) {
                            logger.info("username: '" + personInfo.getUserName() + "'"
                                    + " matches: '" + regExp + "'"
                                    + " fn: '" + personInfo.getFirstName() + "'"
                                    + " ln: '" + personInfo.getLastName() + "'"
                                    + " nodeRef: '" + personInfo.getNodeRef() + "'");
                        }
                    }
                    logger.info("finished skipCount:" + skipCount);
                    skipCount += pageSize;
                    pr = new PagingRequest(skipCount,pageSize);
                } while (people.hasMoreItems());
                return null;
            });
        } catch (PatternSyntaxException e) {
            logger.error("invalid regexp:" + regExp);
            return;
        }
    }

    public static void main(String[] args) {
        String username = "test1daf ccc";
        java.util.Map<String, String> predefinedRegEx = Map.of(
                "-whitespace-", "\\s+",
                "-allwhitespace-", "[ \\t\\n\\r\\f]+");
        String regExp = "[o]";
        System.out.println(username + " regexp:" + regExp);
        System.out.println(Pattern.compile(regExp).matcher(username).find());
    }
}
