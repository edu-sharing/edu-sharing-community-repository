package org.edu_sharing.repository;

import lombok.*;
import org.edu_sharing.repository.server.tools.ApplicationInfo;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TrackingApplicationInfo {
    /**
     * the app info from the app which is currently accessing the system
     */
    @NonNull
    private ApplicationInfo applicationInfo;
    /**
     * the user currently accessing the system (might be not equal to the authenicated user!)
     */
    private String userId;
}
