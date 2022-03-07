package org.edu_sharing.restservices.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.fasterxml.jackson.annotation.JsonProperty;

@Schema(description = "")
public class MdsQueryCriteria {

	private String property;
	private List<String> values;

	@Schema(required = true, description = "")
	@JsonProperty("property")
	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	@Schema(required = true, description = "")
	@JsonProperty("values")
	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}

	public static List<MdsQueryCriteria> fromMap(Map<String, String[]> criterias) {
		List<MdsQueryCriteria>  criterasConverted =new ArrayList<>();
		for(Entry<String, String[]> set : criterias.entrySet()){
			MdsQueryCriteria criteria = new MdsQueryCriteria();
			criteria.setProperty(set.getKey());
			criteria.setValues(Arrays.asList(set.getValue()));
			criterasConverted.add(criteria);
		}
		return criterasConverted;
	}

}
