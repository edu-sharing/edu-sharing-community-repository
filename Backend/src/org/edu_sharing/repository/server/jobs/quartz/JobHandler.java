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

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;

import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.repository.server.tools.CheckAuthentication;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;
import org.quartz.TriggerListener;
import org.quartz.TriggerUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author rudi start jobs, start scheduling of an job, stop scheduling of a job
 */
public class JobHandler {
	
	public class JobConfig {
		
		Class jobClass = null;
		Trigger trigger = null;
		String triggerType = null;
		HashMap<String, Object> params = null;
		String jobname = null;

		public JobConfig() {
		}

		public JobConfig(Class jobClass, Trigger trigger, HashMap<String, Object> params, String triggerType, String name) {
			this.jobClass = jobClass;
			this.trigger = trigger;
			this.params = params;
			this.triggerType = triggerType;
			this.jobname = name;
		}

		public Class getJobClass() {
			return jobClass;
		}

		public void setJobClass(Class jobClass) {
			this.jobClass = jobClass;
		}

		public Trigger getTrigger() {
			return trigger;
		}

		public void setTrigger(Trigger trigger) {
			this.trigger = trigger;
		}

		public HashMap<String, Object> getParams() {
			return params;
		}

		public void setParams(HashMap<String, Object> params) {
			this.params = params;
		}

		public String getTriggerType() {
			return triggerType;
		}

		public void setTriggerType(String triggerType) {
			this.triggerType = triggerType;
		}

		public String getJobname() {
			return jobname;
		}

		public void setJobname(String jobname) {
			this.jobname = jobname;
		}
		
	}

	Logger logger = Logger.getLogger(JobHandler.class);

	static JobHandler instance = null;

	List<JobConfig> jobConfigList = new ArrayList<JobConfig>();

	public static final String TRIGGER_TYPE_DAILY = "Daily";
	public static final String TRIGGER_TYPE_CRON = "Cron";
	public static final String TRIGGER_TYPE_IMMEDIATE = "Immediate";

	Scheduler quartzScheduler = null;

	// trigger info Const job was vetoed
	public static final String JOB_VETOED = "JOB_VETOED";
	
	public static final String IMMEDIATE_JOBNAME_SUFFIX = "_IMMEDIATE";
	
	public static final String VETO_BY_KEY = "VETO_BY";
	
	public static final String AUTH_INFO_KEY = "AUTH_INFO";

	/**
	 * Singelton
	 */
	protected JobHandler() throws Exception {
		SchedulerFactory schedFact = new org.quartz.impl.StdSchedulerFactory();
		quartzScheduler = schedFact.getScheduler();
		quartzScheduler.addGlobalTriggerListener(new TriggerListener() {
			@Override
			public String getName() {
				return "Edu-SharingGlobalTriggerListener";
			}

			@Override
			public void triggerComplete(Trigger arg0, JobExecutionContext arg1, int arg2) {
			}

			@Override
			public void triggerFired(Trigger arg0, JobExecutionContext arg1) {
			}

			@Override
			public void triggerMisfired(Trigger arg0) {
			}

			@Override
			public boolean vetoJobExecution(Trigger trigger, JobExecutionContext jobExecutionContext) {
				logger.info("TriggerListener.vetoJobExecution called");
				try {
					
					//check Authinfo when it's an IMMEDIATE excecuted job
					if(jobExecutionContext.getJobDetail().getName().contains(IMMEDIATE_JOBNAME_SUFFIX)){
						HashMap authInfo = (HashMap)jobExecutionContext.getJobDetail().getJobDataMap().get(AUTH_INFO_KEY);
						
						//we have to validate ticket here so that is it set in the Job Thread Context
						ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
						serviceRegistry.getAuthenticationService().validate((String)authInfo.get(CCConstants.AUTH_TICKET));
						
						if(authInfo == null || !new CheckAuthentication().isAdmin(null, authInfo)){
							jobExecutionContext.getJobDetail().getJobDataMap().put(VETO_BY_KEY, "not an admin");
							return true;
						}
					}
					
					List currentlyExecutingJobsList = jobExecutionContext.getScheduler().getCurrentlyExecutingJobs();
					boolean veto = false;
					for (Object o : currentlyExecutingJobsList) {
						JobExecutionContext jec = (JobExecutionContext) o;

						if (jec.getJobInstance() instanceof AbstractJob && ((AbstractJob) jec.getJobInstance()).isStarted()
								&& Arrays.asList(((AbstractJob) jec.getJobInstance()).getJobClasses()).contains(jec.getJobInstance().getClass())) {
							veto = true;
							jobExecutionContext.getJobDetail().getJobDataMap().put(VETO_BY_KEY, "another job is running");
							logger.info("a job of class " + jec.getJobInstance().getClass().getName() + " is running. veto = true:");
						}
					}
					
					logger.info("TriggerListener.vetoJobExecution returning:" + veto);
					return veto;
					
				} catch (Exception e) {
					e.printStackTrace();
					return false;
				} finally{
					//we have to clearCurrentSecurityContext() to remove authInfo from the current job thread
					ServiceRegistry serviceRegistry = (ServiceRegistry)AlfAppContextGate.getApplicationContext().getBean(ServiceRegistry.SERVICE_REGISTRY);
					try{
						serviceRegistry.getAuthenticationService().clearCurrentSecurityContext();
					}catch(AuthenticationCredentialsNotFoundException e){
						logger.info("clearCurrentSecurityContext of quartz job thread:"+Thread.currentThread().getId() + " there were no AuthenticationCredentials to remove");
					}
				}
			}
		});
		quartzScheduler.addGlobalJobListener(new JobListener() {

			@Override
			public void jobWasExecuted(JobExecutionContext context, JobExecutionException exception) {

				if (exception != null) {
					logger.error("Job execution failed", exception);
				}

				Job job = context.getJobInstance();
				logger.info("JobListener.jobWasExecuted " + job.getClass());
				if (job instanceof AbstractJob) {
					((AbstractJob) job).setStarted(false);
				}
			}

			@Override
			public void jobToBeExecuted(JobExecutionContext context) {
				Job job = context.getJobInstance();
				logger.info("JobListener.jobToBeExecuted " + job.getClass());
				if (job instanceof AbstractJob) {
					((AbstractJob) job).setStarted(true);
				}

			}

			@Override
			public void jobExecutionVetoed(JobExecutionContext context) {
				Job job = context.getJobInstance();

				logger.info("JobListener.jobExecutionVetoed " + job.getClass());
				if (job instanceof AbstractJob) {
					((AbstractJob) job).setStarted(false);
				}
			}

			@Override
			public String getName() {
				logger.info("JobListener.getName");
				return "edu-sharing joblistener";
			}
		});

		quartzScheduler.start();

		// load config from xml
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		URL url = classLoader.getResource("/org/edu_sharing/repository/server/jobs/quartz/jobs.xml");
		XPathFactory pfactory = XPathFactory.newInstance();
		XPath xpath = pfactory.newXPath();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();

		Document jobsConfig = builder.parse(url.openStream());

		NodeList nodeList = (NodeList) xpath.evaluate("/jobs/job", jobsConfig, XPathConstants.NODESET);
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node jobNode = nodeList.item(i);
			String jobClass = (String) xpath.evaluate("class", jobNode, XPathConstants.STRING);
			Class clazz = classLoader.loadClass(jobClass);
			Node nodeTrigger = (Node) xpath.evaluate("trigger", jobNode, XPathConstants.NODE);
			
			
			String jobName = (String) xpath.evaluate("name", jobNode, XPathConstants.STRING);
			
			if(jobName == null || jobName.trim().equals("")){
				jobName = clazz.getSimpleName() + "_JobName_"+ i +"_" + System.currentTimeMillis();
			}
			
			String triggerName = (String) xpath.evaluate("trigger-name", jobNode, XPathConstants.STRING);

			String triggerConfig = (String) xpath.evaluate(".", nodeTrigger, XPathConstants.STRING);

			Trigger trigger = null;
			String triggerType = null;
			if (triggerConfig.contains(TRIGGER_TYPE_DAILY)) {
				triggerType = TRIGGER_TYPE_DAILY;
				// default fire at midnight
				String dailyConfig = triggerConfig.replace("Daily", "").trim();
				int hour = 0;
				int minute = 0;
				if (!dailyConfig.equals("")) {
					dailyConfig = dailyConfig.replaceAll("[", "").replaceAll("]", "");
					String[] splittedConfig = dailyConfig.split(",");
					if (splittedConfig.length == 2) {
						hour = new Integer(splittedConfig[0]);
						minute = new Integer(splittedConfig[1]);
					}
				}
				logger.info("Daily Trigger fires at " + hour + ":" + minute + " for job:" + clazz.getName());
				trigger = TriggerUtils.makeDailyTrigger(hour, minute);
				trigger.setName(jobName + "DailyTrigger"+ System.currentTimeMillis());
			}
			if (triggerConfig.contains(TRIGGER_TYPE_CRON)) {
				triggerType = TRIGGER_TYPE_CRON;
				
				if(triggerName == null || triggerName.trim().equals("")){
					triggerName = clazz.getSimpleName()+ jobName + "CronTrigger_" + i+ "_"+System.currentTimeMillis();
				}
				
				trigger = new CronTrigger(triggerName, null);

				String cronConfig = triggerConfig.replace("Cron", "");
				String cronexpression = "0 0 12 * * ?"; // Fire at 12pm (noon)
														// every day
				if (!cronConfig.equals("")) {
					cronConfig = cronConfig.replaceAll("\\[", "").replaceAll("\\]", "");
					if (!cronConfig.equals("")) {
						cronexpression = cronConfig;
					}
				}

				((CronTrigger) trigger).setCronExpression(cronexpression);
				logger.info("Cron Trigger fires at " + cronexpression + " for job:" + clazz.getName());
			}
			if (triggerConfig.contains(TRIGGER_TYPE_IMMEDIATE)) {
				triggerType = TRIGGER_TYPE_IMMEDIATE;
				trigger = TriggerUtils.makeImmediateTrigger(0, 1);

				logger.info("ImmediateTrigger for job:" + clazz.getName());

				triggerName = (triggerName == null || triggerName.trim().equals("")) ? clazz.getSimpleName() + "ImmediateTrigger"+System.currentTimeMillis() : triggerName;
				trigger.setName(triggerName);
			}

			HashMap<String, Object> params = new HashMap<String, Object>();
			NodeList paramList = (NodeList) xpath.evaluate("params/param", jobNode, XPathConstants.NODESET);
			for (int pCount = 0; pCount < paramList.getLength(); pCount++) {
				Node nodeParam = paramList.item(pCount);
				String key = (String) xpath.evaluate("key", nodeParam, XPathConstants.STRING);
				String value = (String) xpath.evaluate("value", nodeParam, XPathConstants.STRING);
				params.put(key, value);
			}

			if (trigger != null) {
				jobConfigList.add(new JobConfig(clazz, trigger, params, triggerType, jobName));
			}
		}

		// init the jobs
		for (JobConfig jc : jobConfigList) {
			logger.debug("JobListEntry:" + jc.getJobClass().getSimpleName() + " Trigger:" + jc.getTriggerType() + " " + jc.getTrigger().getClass().getName());
			// all except those to start now
			if (!jc.getTriggerType().equals(TRIGGER_TYPE_IMMEDIATE)) {
				this.scheduleJob(jc);
			}
		}

	}
	
	/**
	 * 
	 * This is for immediate job excecution. when it's called a new job with an
	 * immediate trigger will be created and registered with
	 * scheduler.scheduleJob. For every immediate job there will be a
	 * JobListener which is responsible to delete the Job from the scheduler:
	 * - after the excecution finished
	 * - an exception was drown 
	 * - an veto occured.
	 * 
	 * This listener also saves status information i.e if the job was vetoed or
	 * the job finished. It is returned to the caller so that the caller can ask
	 * for status Information
	 * 
	 * 
	 * Another Idea was to put the status information in an JobDataMap of the
	 * trigger but the Method Scheduler.triggerJob(jobName, null,jdm) creates an
	 * own trigger
	 * 
	 * @param jobClass
	 * @param params
	 * @return ImmediateJobListener
	 * @throws SchedulerException
	 */
	public ImmediateJobListener startJob(Class jobClass, HashMap<String, Object> params) throws SchedulerException, Exception {

		String jobName = jobClass.getSimpleName() + IMMEDIATE_JOBNAME_SUFFIX;

		JobDataMap jdm = new JobDataMap();

		if (params != null && params.size() > 0) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				jdm.put(entry.getKey(), entry.getValue());
			}
		}

		Trigger trigger = TriggerUtils.makeImmediateTrigger(0, 1);
		String triggerName = jobClass.getSimpleName() + "ImmediateTrigger";
		trigger.setName(triggerName);

		final String jobListenerName = jobName;

		ImmediateJobListener iJobListener = new ImmediateJobListener(jobListenerName);

		JobDetail jobDetail = new JobDetail(jobName, null, jobClass) {
			@Override
			public String[] getJobListenerNames() {
				return new String[] { jobListenerName };
			}
		};
		jobDetail.setJobDataMap(jdm);

		quartzScheduler.addJobListener(iJobListener);
		quartzScheduler.scheduleJob(jobDetail, trigger);

		/**
		 * the job is executed asynchronous. we want to give the
		 * user information if the job was vetoed(i.e. cause another job runs).
		 * so we simulate synchr execution and wait 1 second and confirm that the
		 * check is done.
		 * 
		 * for the future maybe there will be a new UI where every job that is
		 * registered at the scheduler is listed with status information. The
		 * status information can be refreshed by polling
		 */
		try {
			Thread.sleep(1000); // do nothing for 1000 miliseconds (1 second)
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		return iJobListener;
	}

	/**
	 * schedules the job. used for cron triggers not for immediate triggers the
	 * job detail name for the scheduler is jobClass.getSimpleName() +
	 * "JobDetail";
	 * 
	 * @param jobConfig
	 */
	private void scheduleJob(JobConfig jobConfig) throws SchedulerException {
		//String jobName = 
		JobDataMap jdm = new JobDataMap();

		if (jobConfig.getParams() != null && jobConfig.getParams().size() > 0) {
			for (Map.Entry<String, Object> entry : jobConfig.getParams().entrySet()) {
				jdm.put(entry.getKey(), entry.getValue());
			}
		}

		if (jobConfig.getTrigger() != null) {
			JobDetail jobDetail = new JobDetail(jobConfig.getJobname(), null, jobConfig.getJobClass());
			jobDetail.setJobDataMap(jdm);
			quartzScheduler.scheduleJob(jobDetail, jobConfig.getTrigger());
		}
	}

	public static JobHandler getInstance() throws Exception {
		if (instance == null) {
			instance = new JobHandler();
		}
		return instance;
	}

	public void shutDown() {
		try {
			quartzScheduler.shutdown();
		} catch (SchedulerException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public Object getTriggerAttribut(String triggerName, String attribut) throws SchedulerException {
		Trigger trigger = quartzScheduler.getTrigger(triggerName, null);

		if (trigger != null) {
			JobDataMap jdm = trigger.getJobDataMap();
			if (jdm != null) {
				return jdm.get(attribut);
			}
		}
		return null;
	}

	public List<JobConfig> getJobConfigList() {
		return jobConfigList;
	}
	
}
