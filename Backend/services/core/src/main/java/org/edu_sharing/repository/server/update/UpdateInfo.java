package org.edu_sharing.repository.server.update;

public interface UpdateInfo {
    String getId();

    String getDescription();

    boolean isNonTransactional();

    int getOrder();
    boolean isAuto();
    boolean isTestable();
    void execute(boolean test);
}
