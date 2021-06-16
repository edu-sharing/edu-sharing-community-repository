package org.edu_sharing.repository.client.rpc;

import java.io.Serializable;
import java.util.HashMap;

public class SchoolContextValues implements Serializable{

	Boolean resultBasedOnCombination = false;
	
	HashMap<String,String> federalState = new HashMap<String,String>();
	HashMap<String,String> typeOfSchool = new HashMap<String,String>();
	HashMap<String,String> schoolSubject = new HashMap<String,String>();
	HashMap<String,String> ageGroup = new HashMap<String,String>();
	HashMap<String,String> topic = new HashMap<String,String>();
	
	public SchoolContextValues() {
	}
	
	/**
	 * <NodeRef,Caption>
	 * @return
	 */
	public HashMap<String, String> getFederalState() {
		return federalState;
	}

	/**
	 * <NodeRef,Caption>
	 * @return
	 */
	public HashMap<String, String> getTypeOfSchool() {
		return typeOfSchool;
	}

	/**
	 * <NodeRef,Caption>
	 * @return
	 */
	public HashMap<String, String> getSchoolSubject() {
		return schoolSubject;
	}

	/**
	 * <NodeRef,Caption>
	 * @return
	 */
	public HashMap<String, String> getAgeGroup() {
		return ageGroup;
	}

	/**
	 * <NodeRef,Caption>
	 * @return
	 */
	public HashMap<String, String> getTopic() {
		return topic;
	}
	
	public Boolean getResultBasedOnCombination() {
		return resultBasedOnCombination;
	}
	
	public void setResultBasedOnCombination(Boolean resultBasedOnCombination) {
		this.resultBasedOnCombination = resultBasedOnCombination;
	}
	
}
