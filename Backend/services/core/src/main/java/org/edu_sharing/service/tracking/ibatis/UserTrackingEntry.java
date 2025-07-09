package org.edu_sharing.service.tracking.ibatis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.edu_sharing.service.tracking.ActivityOnNodeEventType;
import org.edu_sharing.service.tracking.UserActivityEventType;
import org.json.JSONObject;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserTrackingEntry {
    private String authority;
    private String[] authority_organization;
    private String[] authority_mediacenter;
    private Date time;
    private UserActivityEventType type;
    private JSONObject data;
}
