package org.edu_sharing.spring.typesafe;

import com.typesafe.config.Config;
import org.springframework.core.env.PropertySource;


public class TypesafeConfigPropertySource extends PropertySource<Config> {
    public TypesafeConfigPropertySource(String name, Config source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String path) {
        if (path.contains("["))
            return null;
        if (path.contains(":"))
            return null;
        if (source.hasPath(path)) {
            return source.getAnyRef(path);
        }
        return null;
    }

}
