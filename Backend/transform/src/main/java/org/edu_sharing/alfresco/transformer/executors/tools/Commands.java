package org.edu_sharing.alfresco.transformer.executors.tools;

import org.alfresco.transform.base.executors.RuntimeExec;

import java.util.HashMap;
import java.util.Map;

public class Commands {
    
    public static RuntimeExec getFFMPegRuntimeExec(){
        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*", new String[] { "ffmpeg", "-version" });
        runtimeExec.setCommandsAndArguments(commandsAndArguments);
        return runtimeExec;
    }
}
