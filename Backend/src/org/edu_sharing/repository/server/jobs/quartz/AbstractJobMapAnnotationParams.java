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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.analysis.function.Abs;
import org.edu_sharing.metadataset.v2.MetadataKey;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobDescription;
import org.edu_sharing.repository.server.jobs.quartz.annotation.JobFieldDescription;
import org.quartz.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class AbstractJobMapAnnotationParams extends AbstractJob {
	@Override
	public final void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
		super.execute(jobExecutionContext);
		Class<? extends AbstractJobMapAnnotationParams> clazz = this.getClass();
		for(Field field : clazz.getDeclaredFields()) {
			if(field.isAnnotationPresent(JobFieldDescription.class)) {
				try {
					field.setAccessible(true);
					if(field.getType().isEnum()) {
						try {
							Object mapped = field.getType().getMethod("valueOf", String.class).invoke(null,
									(String) jobExecutionContext.getJobDetail().getJobDataMap().get(field.getName())
							);
							field.set(this, mapped);
						} catch (Exception e) {
							logger.warn(e.getMessage(), e);
						}
					} else if(field.getType().isPrimitive()) {
						try {
							if(field.getType().getName().equals("boolean")) {
								field.set(this, jobExecutionContext.getJobDetail().getJobDataMap().getBooleanValue(field.getName()));
							} else {
								Constructor<?> constructor = field.getType().getConstructor(field.getType());
								field.set(this, constructor.newInstance(jobExecutionContext.getJobDetail().getJobDataMap().get(field.getName())));
							}
						} catch (Exception e) {
							logger.warn(e.getMessage(), e);
						}
					} else {
						field.set(this, jobExecutionContext.getJobDetail().getJobDataMap().get(field.getName()));
					}
				} catch (IllegalAccessException e) {
					logger.warn("Could not map annotated field " + field.getName());
				}
			}
		}
		this.executeInternal(jobExecutionContext);
	}

	protected abstract void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException;
}
