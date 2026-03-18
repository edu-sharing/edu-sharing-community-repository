package org.edu_sharing.repository.server.tools;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.spring.scope.refresh.annotations.RefreshScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

@Slf4j
@Component
@RefreshScope
public class RepositoryEnvironment {

    @Value("${jobs.primaryHostname:}")
    private String primaryHostname;

    public boolean isPrimaryRepository() {
        if (StringUtils.isNotBlank(primaryHostname)) {
            try {
                return Arrays.asList(
                        InetAddress.getLocalHost().getHostName(),
                        InetAddress.getLocalHost().getHostName().split("\\.")[0]
                ).contains(primaryHostname);
            } catch (UnknownHostException e) {
                log.warn("Could not resolve hostname", e);
                return false;
            }
        } else {
            log.debug("No primaryHostname key, assuming no cluster, jobs are active on this repository");
            return true;
        }

    }

}
