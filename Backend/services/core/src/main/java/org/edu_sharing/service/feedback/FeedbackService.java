package org.edu_sharing.service.feedback;

import org.edu_sharing.repository.client.tools.CCConstants;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.feedback.model.FeedbackData;
import org.edu_sharing.service.feedback.model.FeedbackResult;
import org.edu_sharing.service.permission.annotation.NodePermission;
import org.edu_sharing.service.permission.annotation.Permission;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface FeedbackService {
    @Permission({CCConstants.CCM_VALUE_TOOLPERMISSION_MATERIAL_FEEDBACK})
    List<FeedbackData> getFeedback(
            @NotNull
            @NodePermission({CCConstants.PERMISSION_FEEDBACK, CCConstants.PERMISSION_COLLABORATOR})
            String nodeId
    ) throws InsufficientPermissionException;


    @Permission({CCConstants.CCM_VALUE_TOOLPERMISSION_MATERIAL_FEEDBACK})
    FeedbackResult addFeedback(
            @NotNull
            @NodePermission({CCConstants.PERMISSION_FEEDBACK})
            String nodeId,
            Map<String, List<String>> feedbackData
    );

    void deleteUserData(String userName);

    void changeUserData(String userName, String deletedName);

    void refresh();
}
