package org.edu_sharing.repository.server.importer;

import java.util.HashMap;
import java.util.List;

/**
 * dummy class to wrap a property list into an record handler
 */
public class RecordHandlerStatic implements RecordHandlerInterfaceBase {

    private final HashMap<String, Object> props;

    public RecordHandlerStatic(HashMap<String, Object> props) {
        this.props=props;
    }

    @Override
    public HashMap<String, Object> getProperties() {
        return props;
    }
}
