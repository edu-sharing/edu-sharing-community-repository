package org.edu_sharing.alfresco.transformer;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})
@ComponentScan({"org.edu_sharing.alfresco.transformer","org.alfresco.transformer"})
public class Application
{
    @Value("${container.name}")
    private String containerName;

    @Bean
    MeterRegistryCustomizer<MeterRegistry> metricsCommonTags()
    {
        return registry -> registry.config().commonTags("containerName", containerName);
    }

    public static void main(String[] args)
    {
        SpringApplication.run(Application.class, args);
    }
}
