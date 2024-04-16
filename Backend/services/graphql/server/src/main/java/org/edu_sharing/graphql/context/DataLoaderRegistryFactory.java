package org.edu_sharing.graphql.context;

import org.dataloader.DataLoaderFactory;
import org.dataloader.DataLoaderRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

import java.util.List;
import java.util.Optional;

@Component
public class DataLoaderRegistryFactory {


    @Autowired(required = false) List<org.dataloader.BatchLoader<?,?>> batchLoaders;
    @Autowired(required = false) List<org.dataloader.BatchLoaderWithContext<?,?>> batchLoaderWithContexts;
    @Autowired(required = false) List<org.dataloader.MappedBatchLoader<?,?>> mappedBatchLoaders;
    @Autowired(required = false) List<org.dataloader.MappedBatchLoaderWithContext<?,?>> mappedBatchLoaderWithContexts;

    public DataLoaderRegistry create() {
        DataLoaderRegistry registry = new DataLoaderRegistry();

        Optional.ofNullable(batchLoaders).ifPresent(batchLoaders->batchLoaders.forEach(batchLoader -> registry.register(batchLoader.getClass().getSimpleName(), DataLoaderFactory.newDataLoader(batchLoader))));
        Optional.ofNullable(batchLoaderWithContexts).ifPresent(batchLoaders->batchLoaders.forEach(batchLoader -> registry.register(batchLoader.getClass().getSimpleName(), DataLoaderFactory.newDataLoader(batchLoader))));
        Optional.ofNullable(mappedBatchLoaders).ifPresent(batchLoaders->batchLoaders.forEach(batchLoader -> registry.register(batchLoader.getClass().getSimpleName(), DataLoaderFactory.newMappedDataLoader(batchLoader))));
        Optional.ofNullable(mappedBatchLoaderWithContexts).ifPresent(batchLoaders->batchLoaders.forEach(batchLoader -> registry.register(batchLoader.getClass().getSimpleName(), DataLoaderFactory.newMappedDataLoader(batchLoader))));

        return registry;
    }
}
