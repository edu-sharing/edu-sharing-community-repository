package org.edu_sharing.spring.context;

import org.edu_sharing.spring.converter.NumberToDataSizeConverter;
import org.edu_sharing.spring.converter.StringToDataSizeConverter;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

public class EduSharingContextInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
        ConfigurableEnvironment environment = configurableApplicationContext.getEnvironment();
        environment.getConversionService().addConverter(new StringToDataSizeConverter());
        environment.getConversionService().addConverter(new NumberToDataSizeConverter());
        environment.getPropertySources().addFirst(new TypesafeConfigPropertySource("typeSafe"));
    }
}
