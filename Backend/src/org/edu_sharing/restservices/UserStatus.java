package org.edu_sharing.restservices;

import org.codehaus.jackson.annotate.JsonProperty;
import org.edu_sharing.service.lifecycle.PersonLifecycleService;

import java.util.Date;

public class UserStatus {
    @JsonProperty private PersonLifecycleService.PersonStatus status;
    @JsonProperty private Date date;

    public PersonLifecycleService.PersonStatus getStatus() {
        return status;
    }

    public void setStatus(PersonLifecycleService.PersonStatus status) {
        this.status = status;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
