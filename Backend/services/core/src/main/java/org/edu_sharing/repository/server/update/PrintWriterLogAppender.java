package org.edu_sharing.repository.server.update;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.Layout;

import java.io.PrintWriter;

@Plugin(name="UpdateLogAppender", category = "Core", elementType = "appender", printObject = true)
public class PrintWriterLogAppender extends AbstractAppender {
    private final PrintWriter printWriter;

    protected PrintWriterLogAppender(String name, PrintWriter printWriter) {
        super(name, null, null, false, null);
        this.printWriter = printWriter;
    }

    @PluginFactory
    public static PrintWriterLogAppender createAppender(@PluginAttribute("name") String name,
                                                        PrintWriter printWriter) {
        // Hier können Sie den gewünschten PrintWriter erstellen, z.B. mit System.out oder einer Datei
        return new PrintWriterLogAppender(name, printWriter);
    }

    @Override
    public void append(LogEvent event) {
        String currentThreadId = Long.toString(Thread.currentThread().getId());
        String logThreadId = ThreadContext.get("logThreadId");

        if (currentThreadId.equals(logThreadId)) {
            printWriter.println(event.getMessage());
            printWriter.flush();
        }
    }
}
