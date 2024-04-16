package org.edu_sharing.repository.server.update;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@UpdateService
public class TestUpdate {

    @UpdateRoutine(
            id = "TestUpdate",
            description = "This is an test update",
            order = 100,
            auto = false,
            isNonTransactional = false)
    public void execute() {
        log.info("TestUpdate.test");
    }

}
