package org.edu_sharing.restservices.tracking.v1.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.edu_sharing.restservices.shared.Node;
import org.edu_sharing.restservices.shared.User;

import java.util.Date;

@Data
@AllArgsConstructor
public class UserNodeActivityNode {

    private final Node node;
    private final User user;
    private final String activity;
    @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ssX")
    private final Date timestamp;
}
