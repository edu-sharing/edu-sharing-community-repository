package org.edu_sharing.spring.typesafe;

import com.typesafe.config.Config;
import org.edu_sharing.alfresco.lightbend.LightbendConfigLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertySourceFactory;

import java.io.IOException;

public class TypesafePropertySourceFactory implements PropertySourceFactory {

    @Override
    public PropertySource<?> createPropertySource(String name, EncodedResource resource) throws IOException {
        Config config = LightbendConfigLoader.get();
        String safeName = name == null ? "typeSafe" : name;
        return new TypesafeConfigPropertySource(safeName, config);
    }
}
