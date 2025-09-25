package org.edu_sharing.service.feedback;

import org.alfresco.service.cmr.repository.NodeRef;
import org.edu_sharing.service.InsufficientPermissionException;
import org.edu_sharing.service.feedback.model.FeedbackData;
import org.edu_sharing.service.feedback.model.FeedbackResult;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Lazy
@Service
public class FeedbackServiceAdapter implements FeedbackService{
    @Override
    public List<FeedbackData> getFeedback(@NotNull String nodeId) throws InsufficientPermissionException {
        return List.of();
    }

    @Override
    public FeedbackResult addFeedback(@NotNull String nodeId, Map<String, List<String>> feedbackData) {
        return null;
    }

    @Override
    public void deleteUserData(String userName) {

    }

    @Override
    public void changeUserData(String userName, String deletedName) {

    }

    @Override
    public void refresh() {

    }

    @Override
    public List<NodeRef> getUsersFeedback(String userName) {
        return List.of();
    }
}
