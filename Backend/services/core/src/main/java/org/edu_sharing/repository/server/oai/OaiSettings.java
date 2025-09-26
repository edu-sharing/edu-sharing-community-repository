package org.edu_sharing.repository.server.oai;

import lombok.Data;
import org.edu_sharing.lightbend.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "exporter.oai")
public class OaiSettings {
    private boolean enabled;
    private OaiIdentifier identify;
    private String identifierPrefix;
    private int itemsPerPage;
    private List<String> sets;
}

