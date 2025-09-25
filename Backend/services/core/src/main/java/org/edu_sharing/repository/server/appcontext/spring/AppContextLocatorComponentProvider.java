package org.edu_sharing.repository.server.appcontext.spring;

import org.edu_sharing.repository.server.appcontext.AppContextServiceFactory;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Set;

public class AppContextLocatorComponentProvider extends ClassPathScanningCandidateComponentProvider {

    private boolean considerNestedRepositoryInterfaces;
    private BeanDefinitionRegistry registry;

    public AppContextLocatorComponentProvider(BeanDefinitionRegistry registry) {
        super(false);

        Assert.notNull(registry, "BeanDefinitionRegistry must not be null");

        this.registry = registry;

        addIncludeFilter(new InterfaceTypeFilter(AppContextServiceFactory.class));

    }

    @Override
    protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {

        boolean isNonRepositoryInterface = !AppContextServiceFactory.class.getName().equals(beanDefinition.getBeanClassName());
        boolean isTopLevelType = !beanDefinition.getMetadata().hasEnclosingClass();

        return isNonRepositoryInterface && (isTopLevelType);
    }

    @Override
    public Set<BeanDefinition> findCandidateComponents(String basePackage) {

        Set<BeanDefinition> candidates = super.findCandidateComponents(basePackage);

        for (BeanDefinition candidate : candidates) {
            if (candidate instanceof AnnotatedBeanDefinition) {
                AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);
            }
        }

        return candidates;
    }

    @NonNull
    @Override
    protected BeanDefinitionRegistry getRegistry() {
        return registry;
    }


    private static class InterfaceTypeFilter extends AssignableTypeFilter {

        public InterfaceTypeFilter(Class<?> targetType) {
            super(targetType);
        }

        @Override
        public boolean match(MetadataReader metadataReader, @NotNull MetadataReaderFactory metadataReaderFactory)
                throws IOException {

            return metadataReader.getClassMetadata().isInterface() && super.match(metadataReader, metadataReaderFactory);
        }
    }
}
