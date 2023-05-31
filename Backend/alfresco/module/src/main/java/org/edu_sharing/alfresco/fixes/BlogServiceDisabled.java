package org.edu_sharing.alfresco.fixes;

import org.alfresco.model.BlogIntegrationModel;
import org.alfresco.repo.blog.BlogDetails;
import org.alfresco.repo.blog.BlogIntegrationImplementation;
import org.alfresco.repo.blog.BlogIntegrationService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

import java.util.List;

public class BlogServiceDisabled implements BlogIntegrationService, BlogIntegrationModel {
    @Override
    public void register(BlogIntegrationImplementation blogIntegrationImplementation) {

    }

    @Override
    public BlogIntegrationImplementation getBlogIntegrationImplementation(String s) {
        return null;
    }

    @Override
    public List<BlogIntegrationImplementation> getBlogIntegrationImplementations() {
        return null;
    }

    @Override
    public List<BlogDetails> getBlogDetails(NodeRef nodeRef) {
        return null;
    }

    @Override
    public void newPost(BlogDetails blogDetails, NodeRef nodeRef, QName qName, boolean b) {

    }

    @Override
    public void updatePost(NodeRef nodeRef, QName qName, boolean b) {

    }

    @Override
    public void deletePost(NodeRef nodeRef) {

    }
}
