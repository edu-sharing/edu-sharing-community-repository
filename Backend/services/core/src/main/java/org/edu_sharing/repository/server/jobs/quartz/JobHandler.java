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
import java.util.*;
import java.util.stream.Collectors;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import net.sf.acegisecurity.AuthenticationCredentialsNotFoundException;

import org.alfresco.repo.cache.SimpleCache;
import org.alfresco.service.ServiceRegistry;
import org.apache.log4j.Logger;
import org.edu_sharing.alfrescocontext.gate.AlfAppContextGate;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.quartz.*;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import org.springframework.context.ApplicationContext;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.SimpleScheduleBuilder.simpleSchedule;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.CronScheduleBuilder.*;

/**
 * @author rudi start jobs, start scheduling of an job, stop scheduling of a job
 */
@Component
public class JobHandler {

	public static final Object KEY_RESULT_DATA = "JOB_RESULT_DATA";
	private static final int MAX_JOB_LOG_COUNT = 20; // maximal number of jobs to store for history and gui
	public final static SimpleCache<String, List<JobInfo>> jobs = (SimpleCache)  AlfAppContextGate.getApplicationContext().getBean("eduSharingJobsListCache");
	private static final String JOB_LIST_KEY = "jobs";

	//private final ApplicationContext eduApplicationContext = null;

	private final JobClusterLocker jobClusterLocker;

	@Autowired
	public JobHandler(JobClusterLocker jobClusterLocker, SchedulerFactoryBean schedulerFactoryBean) throws Exception {
		this.jobClusterLocker = jobClusterLocker;
		this.schedulerFactoryBean = schedulerFactoryBean;
		init();
	}

	private final SchedulerFactoryBean schedulerFactoryBean;

	public static void refreshJobsCache(JobInfo job) {
		// trigger refresh of the jobs list in cluster
		jobs.put(JOB_LIST_KEY, jobs.get(JOB_LIST_KEY).stream().map(j -> j.equals(job) ? job : j).collect(Collectors.toList()));
	}

	public boolean cancelJob(String jobName) throws SchedulerException {

		boolean result=quartzScheduler.interrupt(JobKey.jobKey(jobName));
		if(!result){
			try {
				finishJob(quartzScheduler.getJobDetail(JobKey.jobKey(jobName)), JobInfo.Status.Aborted);
			}catch(Throwable t){
				t.printStackTrace();
			}
		}
		return result;
	}
	public void finishJob(JobDetail jobDetail, JobInfo.Status status) {
		for(JobInfo job : getJobs()){
			if(job.equalsDetail(jobDetail) && job.getStatus().equals(JobInfo.Status.Running)){
				job.setStatus(status);
				job.setFinishTime(System.currentTimeMillis());
				refreshJobsCache(job);
				return;
			}
		}
		if(JobLogger.IGNORABLE_JOBS.contains(jobDetail.getJobClass().getName()))
			return;
		throw new IllegalArgumentException("Job "+jobDetail.getKey()+" was not found");
	}

	private static synchronized List<JobInfo> getJobs() {
		if(jobs.get(JOB_LIST_KEY) == null) {
			jobs.put(JOB_LIST_KEY, new ArrayList<>());
		}
		return jobs.get(JOB_LIST_KEY);
	}
	private static synchronized void addJob(JobInfo jobInfo) {
		List<JobInfo> jobList = getJobs();
		jobList.add(jobInfo);
		jobs.put(JOB_LIST_KEY, jobList);
	}

	public void updateJobName(JobDetail jobDetail, String name) {
		if(jobDetail==null)
			return;
		for(JobInfo info : getJobs()){
			if(info.equalsDetail(jobDetail)){
				((JobDetailImpl)jobDetail).setName(name);
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

	//static JobHandler instance = null;

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

//	/**
//	 * Singelton
//	 */
//	protected JobHandler() throws Exception {
//		init(org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext());
//	}
//
//	@Autowired
//	protected JobHandler(ApplicationContext applicationContext) throws Exception {
//		init(applicationContext);
//	}

	private void init() throws Exception{
		quartzScheduler = schedulerFactoryBean.getScheduler();
		quartzScheduler.getListenerManager().addTriggerListener(new TriggerListener() {
			@Override
			public String getName() {
				return "Edu-SharingGlobalTriggerListener";
			}

			@Override
			public void triggerComplete(Trigger trigger, JobExecutionContext context, Trigger.CompletedExecutionInstruction triggerInstructionCode) {

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

					//check cluster singeltons
					if(jobExecutionContext.getJobInstance() instanceof JobClusterLocker.ClusterSingelton){
						boolean aquiredLock = jobClusterLocker.tryLock(jobExecutionContext.getJobInstance().getClass().getName());
						if(!aquiredLock){
							veto = true;
							jobExecutionContext.getJobDetail().getJobDataMap().put(VETO_BY_KEY, "same job is running on another cluster node");
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
		quartzScheduler.getListenerManager().addJobListener(new JobListener() {

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

				if(job instanceof JobClusterLocker.ClusterSingelton){
					jobClusterLocker.releaseLock(job.getClass().getName());
				}

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
				if(context.getJobDetail() != null && context.getJobDetail().getJobDataMap() != null){
					String vetoBy = (String)context.getJobDetail().getJobDataMap().get(JobHandler.VETO_BY_KEY);
					Logger.getLogger(context.getJobDetail().getJobClass()).info("Job was vetoed by "+vetoBy);
				}
			}

			@Override
			public String getName() {
				return "edu-sharing joblistener";
			}
		});

		// use startDelayed() to not block server startup by IMMEDIATE jobs
		quartzScheduler.startDelayed(10);

		refresh(true);
	}

	public synchronized void refresh(boolean triggerImmediateJobs) {
		try {
			ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
			List<? extends Config> list = LightbendConfigLoader.get().getConfigList("jobs.entries");
			jobConfigList.clear();
			for (String groupName : quartzScheduler.getJobGroupNames()) {

				for (JobKey jobKey : quartzScheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
					String jobName = jobKey.getName();
					if (!quartzScheduler.deleteJob(jobKey)) {
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
					if(!triggerConfig.contains(TRIGGER_TYPE_IMMEDIATE) || triggerImmediateJobs) {
						jobConfigList.add(new JobConfig(clazz, trigger, params, jobName));
					}
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

			trigger = newTrigger()
					.withSchedule(dailyAtHourAndMinute(hour,minute))
					.withIdentity(jobName + "_DailyTrigger"+ System.currentTimeMillis())
					.build();

		}else if (triggerConfig.contains(TRIGGER_TYPE_CRON)) {
			String triggerName = jobName + "_CronTrigger_"+System.currentTimeMillis();

			String cronConfig = triggerConfig.replace("Cron", "");
			String cronexpression = "0 0 12 * * ?"; // Fire at 12pm (noon)
			// every day
			if (!cronConfig.equals("")) {
				cronConfig = cronConfig.replaceAll("\\[", "").replaceAll("\\]", "");
				if (!cronConfig.equals("")) {
					cronexpression = cronConfig;
				}
			}
			trigger = newTrigger()
					.withIdentity(triggerName)
					.withSchedule(cronSchedule(cronexpression))
					.build();
		} else if (triggerConfig.contains(TRIGGER_TYPE_IMMEDIATE)) {
			String triggerName = jobName+"_ImmediateTrigger"+System.currentTimeMillis();
			trigger = newTrigger()
					.withIdentity(triggerName)
					.withSchedule(simpleSchedule().withIntervalInSeconds(1).withRepeatCount(0))
					.build();
		}
		return trigger;

	}

	private synchronized void registerJob(JobDetail jobDetail) {
		if(JobLogger.IGNORABLE_JOBS.contains(jobDetail.getJobClass().getName()))
			return;
		JobInfo info=new JobInfo(jobDetail);
		JobLogger.init(info);
		addJob(info);
		while(getJobs().size()>MAX_JOB_LOG_COUNT) {
			List<JobInfo> jobsTmp = getJobs();
			jobsTmp.remove(0);
			jobs.put(JOB_LIST_KEY, jobsTmp);
		}
	}

	public List<JobInfo> getAllRunningJobs() throws SchedulerException {
		List<JobInfo> result=getJobs();
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

		String triggerName = jobClass.getSimpleName() + "ImmediateTrigger";
		Trigger trigger = newTrigger()
				.withIdentity(triggerName)
				.withSchedule(simpleSchedule()
								.withIntervalInSeconds(1)
								.withRepeatCount(0))
				.build();

		final String jobListenerName = jobName;

		JobDetail jobDetail = newJob(jobClass).withIdentity(jobName).setJobData(jdm).build();

		/*JobDetail jobDetail = new JobDetail(jobName, null, jobClass) {
			@Override
			public String[] getJobListenerNames() {
				return new String[] { jobListenerName };
			}
		};*/

		ImmediateJobListener iJobListener = new ImmediateJobListener(jobListenerName);
		quartzScheduler.getListenerManager().addJobListener(iJobListener);
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
			JobDetail jobDetail =  newJob(jobConfig.getJobClass()).withIdentity(jobConfig.getJobname()).setJobData(jdm).build();
			quartzScheduler.scheduleJob(jobDetail, jobConfig.getTrigger());
		}
	}

	public static JobHandler getInstance() throws Exception {
//		if (instance == null) {
//			instance = new JobHandler();
//		}
//		return instance;
		return getInstance(org.edu_sharing.spring.ApplicationContextFactory.getApplicationContext());
	}

	public static JobHandler getInstance(ApplicationContext applicationContext) throws Exception {
//		if (instance == null) {
//			instance = new JobHandler(applicationContext);
//		}
//		return instance;
		return applicationContext.getBean(JobHandler.class);
	}

	public void shutDown() {
		try {
			quartzScheduler.shutdown();
		} catch (SchedulerException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public Object getTriggerAttribut(String triggerName, String attribut) throws SchedulerException {

		Trigger trigger = quartzScheduler.getTrigger(TriggerKey.triggerKey(triggerName));

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
