package org.edu_sharing.graphql.tools;

import java.io.IOException;
import java.util.List;

public interface SchemaStringProvider {
    List<String> schemaStrings() throws IOException;
}
