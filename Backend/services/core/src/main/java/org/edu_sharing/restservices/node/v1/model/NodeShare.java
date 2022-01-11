package org.edu_sharing.restservices.node.v1.model;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.repository.client.rpc.Share;
import org.edu_sharing.repository.server.tools.URLTool;
import org.edu_sharing.restservices.shared.Node;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import com.fasterxml.jackson.annotation.JsonProperty;


@ApiModel(description = "")
public class NodeShare  {
	private boolean password;
	private String token;
	private String email;
	private Long expiryDate;
	private long invitedAt;
	private int downloadCount;
	private String url;
	private String shareId;


	public NodeShare(){}
	public NodeShare(NodeRef node,Share share){
		shareId=share.getNodeId();
		token=share.getToken();
		email=share.getEmail();
		password=share.getPassword()!=null;
		expiryDate=share.getExpiryDate();
		invitedAt=share.getInvitedAt().getTime();
		downloadCount=share.getDownloadCount();
		url=URLTool.getShareServletUrl(node, share.getToken());
	}

	public boolean isPassword() {
		return password;
	}

	public void setPassword(boolean password) {
		this.password = password;
	}

	@JsonProperty("shareId")
	public String getShareId() {
		return shareId;
	}
	public void setShareId(String shareId) {
		this.shareId = shareId;
	}
	@JsonProperty("url")
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	@JsonProperty("token")
	public String getToken() {
		return token;
	}
	public void setToken(String token) {
		this.token = token;
	}
	@JsonProperty("email")
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	@JsonProperty("expiryDate")
	public Long getExpiryDate() {
		return expiryDate;
	}
	public void setExpiryDate(Long expiryDate) {
		this.expiryDate = expiryDate;
	}
	@JsonProperty("invitedAt")
	public long getInvitedAt() {
		return invitedAt;
	}
	public void setInvitedAt(long invitedAt) {
		this.invitedAt = invitedAt;
	}
	@JsonProperty("downloadCount")
	public int getDownloadCount() {
		return downloadCount;
	}
	public void setDownloadCount(int downloadCount) {
		this.downloadCount = downloadCount;
	}

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class NodeEntry {\n");
        sb.append("}\n");
    return sb.toString();
  }
}
