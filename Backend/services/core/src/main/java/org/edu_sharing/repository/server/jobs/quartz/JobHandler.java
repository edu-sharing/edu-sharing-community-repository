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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;

import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
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

/**
 * @author rudi start jobs, start scheduling of an job, stop scheduling of a job
 */
public class JobHandler {

	private static final int MAX_JOB_LOG_COUNT = 20; // maximal number of jobs to store for history and gui
	private static List<JobInfo> jobs = new ArrayList<>();

	public boolean cancelJob(String jobName) throws SchedulerException {
		boolean result=quartzScheduler.interrupt(jobName, null);
		if(!result){
			try {
				finishJob(quartzScheduler.getJobDetail(jobName, null), JobInfo.Status.Aborted);
			}catch(Throwable t){
				t.printStackTrace();
			}
		}
		return result;
	}
	public void finishJob(JobDetail jobDetail, JobInfo.Status status) {
		for(JobInfo job : jobs){
			if(job.getJobDetail().equals(jobDetail) && job.getStatus().equals(JobInfo.Status.Running)){
				job.setStatus(status);
				job.setFinishTime(System.currentTimeMillis());
				return;
			}
		}
		if(JobLogger.IGNORABLE_JOBS.contains(jobDetail.getJobClass().getName()))
			return;
		throw new IllegalArgumentException("Job "+jobDetail.getFullName()+" was not found");
	}

	public void updateJobName(JobDetail jobDetail, String name) {
		if(jobDetail==null)
			return;
		for(JobInfo info : jobs){
			if(info.getJobDetail().equals(jobDetail)){
				jobDetail.setName(name);
				info.setJobDetail(jobDetail);
				return;
			}
		}
	}

	public class JobConfig {
		
		Class jobClass = null;
		Trigger trigger = null;
		HashMap<String, Object> params = null;
		String jobname = null;

		public JobConfig() {
		}

		public JobConfig(Class jobClass, Trigger trigger, HashMap<String, Object> params, String name) {
			this.jobClass = jobClass;
			this.trigger = trigger;
			this.params = params;
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

		public String getJobname() {
			return jobname;
		}

		public void setJobname(String jobname) {
			this.jobname = jobname;
		}
		
	}

	Logger logger = Logger.getLogger(JobHandler.class);

	static JobHandler instance = null;

	List<JobConfig> jobConfigList = new ArrayList<>();

	public static final String TRIGGER_TYPE_DAILY = "Daily";
	public static final String TRIGGER_TYPE_CRON = "Cron";
	public static final String TRIGGER_TYPE_IMMEDIATE = "Immediate";

	Scheduler quartzScheduler = null;

	// trigger info Const job was vetoed
	public static final String JOB_VETOED = "JOB_VETOED";
	
	public static final String IMMEDIATE_JOBNAME_SUFFIX = "_IMMEDIATE";
	
	public static final String VETO_BY_KEY = "VETO_BY";
	
	public static final String AUTH_INFO_KEY = "AUTH_INFO";

	public static final String FILE_DATA = "FILE_DATA";

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
					
					
					
					/**
					 * don't veto
					 * Allow jobs to run independend of other jobs
					 */
					Class[] allJobs = ((AbstractJob)jobExecutionContext.getJobInstance()).getJobClasses();
					if(allJobs == null || allJobs.length == 0) {
						return false;
					}
					
					/**
					 * veto if there is another job running
					 */
					List currentlyExecutingJobsList = jobExecutionContext.getScheduler().getCurrentlyExecutingJobs();
					boolean veto = false;
					for (Object o : currentlyExecutingJobsList) {
						JobExecutionContext jec = (JobExecutionContext) o;

						if (jec.getJobInstance() instanceof AbstractJob && ((AbstractJob) jec.getJobInstance()).isStarted()
								//&& Arrays.asList(((AbstractJob) jec.getJobInstance()).getJobClasses()).contains(jec.getJobInstance().getClass())
								&& jobExecutionContext.getJobInstance().getClass().equals(jec.getJobInstance().getClass())
						) {
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
					Logger.getLogger(context.getJobInstance().getClass()).error(exception);
				}

				Job job = context.getJobInstance();
				logger.info("JobListener.jobWasExecuted " + job.getClass());
				JobInfo.Status status = JobInfo.Status.Finished;
				if (job instanceof AbstractJob) {
					((AbstractJob) job).setStarted(false);
					status=((AbstractJob) job).isInterrupted() ? JobInfo.Status.Aborted : JobInfo.Status.Finished;
				}
				finishJob(context.getJobDetail(),status);

			}

			@Override
			public void jobToBeExecuted(JobExecutionContext context) {
				Job job = context.getJobInstance();
				logger.info("JobListener.jobToBeExecuted " + job.getClass());
				if (job instanceof AbstractJob) {
					((AbstractJob) job).setStarted(true);
				}
				registerJob(context.getJobDetail());
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
				return "edu-sharing joblistener";
			}
		});

		// use startDelayed() to not block server startup by IMMEDIATE jobs
		quartzScheduler.startDelayed(10);

		refresh();

	}

	public synchronized void refresh() {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			List<? extends Config> list = LightbendConfigLoader.get().getConfigList("jobs.entries");
			jobConfigList.clear();
			for (String groupName : quartzScheduler.getJobGroupNames()) {
				for (String jobName : quartzScheduler.getJobNames(groupName)) {
					if (!quartzScheduler.deleteJob(jobName, groupName)) {
						logger.warn("Unable to delete previously scheduled job " + jobName);
					}
				}
			}
			for (Config job : list) {
				String jobClass = job.getString("class");
				Class clazz = classLoader.loadClass(jobClass);
				String jobName = job.getString("name");
				if (jobName == null || jobName.trim().equals("")) {
					jobName = clazz.getSimpleName() + "_JobName_" + System.currentTimeMillis();
				}
				HashMap<String, Object> params = new HashMap<>();
				if(job.hasPath("params")) {
					for (Map.Entry<String, ConfigValue> configParams : job.getConfig("params").entrySet()) {
						params.put(configParams.getKey(), configParams.getValue().unwrapped());
					}
				}
				String triggerConfig = job.getString("trigger");
				Trigger trigger = getTriggerFromString(jobName, triggerConfig);
				if (trigger != null) {
					jobConfigList.add(new JobConfig(clazz, trigger, params, jobName));
				} else {
					logger.warn("Job "+jobName+" has no trigger and will not be scheduled");
				}
			}

			for (JobConfig jc : jobConfigList) {
				this.scheduleJob(jc);
			}
		}catch (Exception e){
			logger.warn("Could not init scheduled jobs",e);
		}
	}

	private Trigger getTriggerFromString(String jobName, String triggerConfig) throws ParseException {
		Trigger trigger = null;
		if (triggerConfig.contains(TRIGGER_TYPE_DAILY)) {
			// default fire at midnight
			String dailyConfig = triggerConfig.replace("Daily", "").trim();
			int hour = 0;
			int minute = 0;
			if (!dailyConfig.equals("")) {
				dailyConfig = dailyConfig.replaceAll("\\[", "").replaceAll("\\]", "");
				String[] splittedConfig = dailyConfig.split(",");
				if (splittedConfig.length == 2) {
					hour = new Integer(splittedConfig[0]);
					minute = new Integer(splittedConfig[1]);
				}
			}
			trigger = TriggerUtils.makeDailyTrigger(hour, minute);
			trigger.setName(jobName + "_DailyTrigger"+ System.currentTimeMillis());
		}else if (triggerConfig.contains(TRIGGER_TYPE_CRON)) {
			String triggerName = jobName + "_CronTrigger_"+System.currentTimeMillis();
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
		} else if (triggerConfig.contains(TRIGGER_TYPE_IMMEDIATE)) {
			trigger = TriggerUtils.makeImmediateTrigger(0, 1);

			String triggerName = jobName+"_ImmediateTrigger"+System.currentTimeMillis();
			trigger.setName(triggerName);
		}
		return trigger;

	}

	private void registerJob(JobDetail jobDetail) {
		if(JobLogger.IGNORABLE_JOBS.contains(jobDetail.getJobClass().getName()))
			return;
		JobInfo info=new JobInfo(jobDetail);
		jobs.add(info);
		while(jobs.size()>MAX_JOB_LOG_COUNT)
			jobs.remove(0);
	}

	public List<JobInfo> getAllJobs() throws SchedulerException {
		List<JobInfo> result=jobs;
		/*
		List running=getRunningJobs();
		for(JobInfo info : result) {
			boolean isRunning=false;
			for (Object run : running) {
				JobExecutionContext context = (JobExecutionContext) run;
				if(context.getJobDetail().equals(info.getJobDetail())){
					isRunning=true;
				}
			}
			info.setStatus(isRunning ? JobInfo.Status.Running : JobInfo.Status.Finished);
		}
		*/
		return result;
	}
	public  List getRunningJobs() throws SchedulerException {
		return quartzScheduler.getCurrentlyExecutingJobs();
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

		JobDataMap jdm = createJobDataMap(params);

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

	public static JobDataMap createJobDataMap(HashMap<String, Object> params) {
		JobDataMap jdm = new JobDataMap();

		if (params != null && params.size() > 0) {
			for (Map.Entry<String, Object> entry : params.entrySet()) {
				jdm.put(entry.getKey(), entry.getValue());
			}
		}
		return jdm;
	}

	/**
	 * schedules the job. used for cron triggers not for immediate triggers the
	 * job detail name for the scheduler is jobClass.getSimpleName() +
	 * "JobDetail";
	 * 
	 * @param jobConfig
	 */
	private void scheduleJob(JobConfig jobConfig) throws SchedulerException {
		JobDataMap jdm = createJobDataMap(jobConfig.getParams());
		if (jobConfig.getTrigger() != null) {
			logger.info("Schedule job "+jobConfig.getJobname()+" "+jobConfig.getJobClass().getSimpleName()+" "+jobConfig.getTrigger().toString());
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
