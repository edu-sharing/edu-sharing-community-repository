package org.edu_sharing.restservices.shared;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;;

import org.edu_sharing.restservices.RepositoryDao;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.Objects;

@Schema(description = "")
public class NodeRef implements Serializable, Comparable<NodeRef> {

	private String repo = null;
	private boolean isHomeRepo = false;
	private String id = null;
	
	boolean archived = false;
	
	public NodeRef(){}
	public NodeRef(String repoId,String nodeId) {
		repo=repoId;
		id=nodeId;
	}
	public NodeRef(RepositoryDao repo,String nodeId) {
		this.repo=repo.getId();
		this.isHomeRepo=repo.isHomeRepo();
		this.id=nodeId;
	}

	/**
   **/
	@Schema(required = true, description = "")
	@JsonProperty("repo")
	public String getRepo() {
		return repo;
	}

	public void setRepo(String repo) {
		this.repo = repo;
	}

	/**
   **/
	@Schema(required = true, description = "")
	@JsonProperty("id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public void setArchived(boolean archived) {
		this.archived = archived;
	}
	
	@Schema(required = true, description = "")
	@JsonProperty("archived")
	public boolean isArchived() {
		return archived;
	}

	@JsonProperty("isHomeRepo")
	public boolean isHomeRepo() {
		return isHomeRepo;
	}
	public void setHomeRepo(boolean isHomeRepo) {
		this.isHomeRepo = isHomeRepo;
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("class RepoRef {\n");

		sb.append("  repo: ").append(repo).append("\n");
		sb.append("  id: ").append(id).append("\n");
		sb.append("}\n");
		return sb.toString();
	}

	@Override
	public int compareTo(NodeRef other) {

		String s1 = this.repo + this.id;
		String s2 = other.repo + other.id;
		
		return s1.compareTo(s2);
	}
	@Override
	public boolean equals(Object other) {
		if(other instanceof NodeRef){
			NodeRef o=(NodeRef)other;
			return this.repo.equals(o.repo) && this.id.equals(o.id);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(repo, id, archived);
	}
}
