package org.edu_sharing.restservices.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

@ApiModel(description = "")
public class Mediacenter extends ManagableGroup {
	public Mediacenter(Group group) {
		this.setAuthorityName(group.getAuthorityName());
		this.setAuthorityType(group.getAuthorityType());
		this.setGroupName(group.getGroupName());
		this.setProfile(group.getProfile());
	}

	public static class Profile extends GroupProfile {
		private MediacenterProfileExtension mediacenter;

		public Profile() {
		}

		public Profile(GroupProfile profile){
			super(profile);
		}

		@JsonProperty
		public MediacenterProfileExtension getMediacenter() {
			return mediacenter;
		}

		public void setMediacenter(MediacenterProfileExtension mediacenter) {
			this.mediacenter = mediacenter;
		}
	}
	public static class MediacenterProfileExtension{
		// id, Standort, Kreiskürzel, Kurzbezeichnung (not here, equals the displayName), URL/Link zur Startseite / Kataloge (JSON)
		private String id,location,districtAbbreviation,mainUrl;
		private List<Catalog> catalogs;
		public enum ContentStatus{
			Activated,
			Deactivated,
		};
		private ContentStatus contentStatus;

		@JsonProperty
		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}
		@JsonProperty
		public String getLocation() {
			return location;
		}

		public void setLocation(String location) {
			this.location = location;
		}
		@JsonProperty
		public String getDistrictAbbreviation() {
			return districtAbbreviation;
		}

		public void setDistrictAbbreviation(String districtAbbreviation) {
			this.districtAbbreviation = districtAbbreviation;
		}
		@JsonProperty
		public String getMainUrl() {
			return mainUrl;
		}

		public void setMainUrl(String mainUrl) {
			this.mainUrl = mainUrl;
		}
		@JsonProperty
		public List<Catalog> getCatalogs() {
			return catalogs;
		}

		public void setCatalogs(List<Catalog> catalogs) {
			this.catalogs = catalogs;
		}
		@JsonProperty
		public ContentStatus getContentStatus() {
			return contentStatus;
		}

		public void setContentStatus(ContentStatus contentStatus) {
			this.contentStatus = contentStatus;
		}
	}
	public static class Catalog{
		private String name,url;

		@JsonProperty
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		@JsonProperty
		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}
	}
}
