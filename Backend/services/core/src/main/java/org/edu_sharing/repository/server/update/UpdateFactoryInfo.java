package org.edu_sharing.repository.server.update;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.function.Consumer;

@Getter
@AllArgsConstructor
public class UpdateFactoryInfo implements UpdateInfo {
    String id;
    String description;
    boolean isNonTransactional;
    int order;
    boolean auto;
    boolean testable;
    Consumer<Boolean> action;


    @Override
    public void execute(boolean test) {
        if(!testable && test) {
            return;
        }

        action.accept(test);
    }
}
