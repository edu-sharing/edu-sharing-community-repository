package org.edu_sharing.alfresco.fixes;

import org.alfresco.repo.blog.BlogDetails;
import org.alfresco.repo.blog.BlogIntegrationImplementation;

import java.util.Map;

public class BlogIntegrationDisabled implements BlogIntegrationImplementation {
    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String newPost(BlogDetails blogDetails, String s, String s1, boolean b) {
        return null;
    }

    @Override
    public boolean updatePost(BlogDetails blogDetails, String s, String s1, String s2, boolean b) {
        return false;
    }

    @Override
    public Map<String, Object> getPost(BlogDetails blogDetails, String s) {
        return null;
    }

    @Override
    public boolean deletePost(BlogDetails blogDetails, String s) {
        return false;
    }
}
