package org.edu_sharing.repository.server.tools;

import lombok.RequiredArgsConstructor;
import org.edu_sharing.service.nodeservice.NodeService;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserEnvironmentToolFactory {

    private final NodeService nodeService;

    @Bean
    @SessionScope
    public UserEnvironmentTool createUserEnvironmentTool(){
        return new UserEnvironmentTool(nodeService);
    }

    public UserEnvironmentTool createEnvironmentTool(Map<String, String> authInfo){
        return new UserEnvironmentTool(nodeService, authInfo);
    }
}
