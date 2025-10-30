package org.edu_sharing.repository.server.tools;

import org.edu_sharing.service.nodeservice.NodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UserEnvironmentToolFactory {

    @Autowired
    private NodeService nodeService;

    public UserEnvironmentTool createUserEnvironmentTool(){
        return new UserEnvironmentTool(nodeService);
    }

}
