package org.edu_sharing.service.bulk;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.edu_sharing.restservices.shared.Node;

@Getter
@AllArgsConstructor
public class BulkRun {
    Node node;
    RunState state;

    public enum RunState {
        New,
        Published
    }
}
