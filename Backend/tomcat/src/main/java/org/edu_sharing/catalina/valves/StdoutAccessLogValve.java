package org.edu_sharing.catalina.valves;

import org.apache.catalina.valves.AbstractAccessLogValve;

import java.io.CharArrayWriter;

public class StdoutAccessLogValve extends AbstractAccessLogValve {

    @Override
    protected void log(CharArrayWriter message) {
        System.out.println(message.toCharArray());
    }
}
