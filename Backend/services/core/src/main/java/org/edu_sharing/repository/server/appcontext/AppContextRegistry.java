package org.edu_sharing.repository.server.appcontext;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ListableBeanFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A registry for managing application-specific contexts and their configurations.
 * <p>
 * The {@code AppContextRegistry} is responsible for holding a collection of application
 * contexts, each identified by a unique application identifier. It enables the retrieval
 * of context-specific configurations and facilitates context-aware dependency management
 * for multi-application environments.
 * <p>
 * Features:
 * - Maintains a map of application-specific contexts and their associated configurations.
 * - Differentiates between a "local" context and other application contexts.
 * - Provides builders for constructing application contexts dynamically.
 * <p>
 * Design:
 * - Each context is represented by a {@code ContextDefinition} object.
 * - A local context is mandatory and is validated during construction.
 * - The registry is immutable once created, ensuring thread safety and consistency.
 */
public final class AppContextRegistry {

    @Getter
    private final Map<String, ContextDefinition> contexts;
    @Getter
    private final ContextDefinition fallbackContext;

    public AppContextRegistry(Map<String, ContextDefinition> contexts) {
        HashMap<String, ContextDefinition> copyContext = new HashMap<>(contexts);
        ContextDefinition localContext = copyContext.get("local");
        if (localContext == null) {
            throw new IllegalArgumentException("No local app context found");
        }
        this.fallbackContext = copyContext.remove("fallback");
        this.contexts = Collections.unmodifiableMap(copyContext);
    }


    /**
     * Retrieves the {@link ContextDefinition} associated with the given application ID.
     * <p>
     * This method looks up the context registry to find the context definition for the specified
     * application ID. If no context definition is found, an {@link IllegalArgumentException} is thrown.
     *
     * @param contextName the unique identifier of the application for which the context definition is requested
     * @return the {@link ContextDefinition} associated with the specified application ID
     * @throws IllegalArgumentException if no context definition is found for the given application ID
     */
    public ContextDefinition getContexts(String contextName) {
        ContextDefinition def = contexts.get(contextName);
        if (def == null) {
            throw new IllegalArgumentException("No app context found for contextName: " + contextName);
        }

        return def;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<String, ContextDefinition> contexts = new HashMap<>();

        /**
         * Creates a {@code ContextBuilder} for defining configurations specific to the local application context.
         * <p>
         * This method initializes a builder instance that is associated with the predefined "local" context.
         * The created builder can be used to specify local context-specific overrides and configurations,
         * which will apply to components or dependencies unique to the local application.
         * <p>
         * The configurations defined via this method can later be finalized and incorporated into the broader
         * app context infrastructure through the context management system.
         *
         * @return a {@code ContextBuilder} instance for configuring the local application context.
         */
        public ContextBuilder localAppContext() {
            return new ContextBuilder(this, "local");
        }

        public ContextBuilder fallbackAppContext() {
            return new ContextBuilder(this, "fallback");
        }

        /**
         * Adds an application-specific context by its identifier.
         * <p>
         * This method creates a new {@code ContextBuilder} instance associated with the given
         * application identifier. The returned {@code ContextBuilder} can then be used to define
         * overrides and configurations specific to the provided application context.
         *
         * @param name the unique identifier of the application context to be added
         * @return a {@code ContextBuilder} instance for configuring the specified application context
         */
        public ContextBuilder addAppContext(String name) {
            return new ContextBuilder(this, name);
        }

        public AppContextRegistry build() {
            return new AppContextRegistry(Collections.unmodifiableMap(contexts));
        }
    }

    @RequiredArgsConstructor
    public static final class ContextBuilder {
        private final Builder parent;
        private final String contextName;
        private final Map<Class<?>, BeanOverride<?>> overrides = new HashMap<>();

        /**
         * Overrides the default binding for the specified interface type with a specific bean name.
         * This method allows customization of which implementation should be used for a given type
         * within the context, based on its associated bean name.
         *
         * @param <T>      the type of the interface or class being overridden
         * @param type     the class type for which the override is being applied
         * @param beanName the name of the bean that should be used for the specified type
         * @return the current instance of {@code ContextBuilder} to allow method chaining
         */
        public <T> ContextBuilder defineBean(Class<T> type, String beanName) {
            overrides.put(type, new OverrideByBeanName<>(type, beanName));
            return this;
        }

        /**
         * Overrides the default binding for the specified interface or class type with a specific implementation type.
         * This method allows customization of which implementation should be used for a given type within the context.
         *
         * @param <T>          the type of the interface or class being overridden
         * @param type         the class type for which the override is being applied
         * @param specificType the specific class type that should be used as the override for the specified type
         * @return the current instance of {@code ContextBuilder} to allow method chaining
         * @throws IllegalArgumentException if the specific type is not a valid override for the given type
         */
        public <T,S extends T> ContextBuilder defineBean(Class<T> type, Class<S> specificType) {
            if (!type.isAssignableFrom(specificType)) {
                throw new IllegalArgumentException("Cannot override type " + type + " with subtype " + specificType);
            }
            overrides.put(type, new OverrideByClass<>(specificType));
            return this;
        }

        public Builder done() {
            parent.contexts.put(contextName, new ContextDefinition(contextName, overrides));
            return parent;
        }
    }

    public record ContextDefinition(
            String contextName,
            Map<Class<?>, BeanOverride<?>> overrides) {

        @SuppressWarnings("unchecked")
        public <T> BeanOverride<T> resolveOverrideBean(Class<T> type) {
            return (BeanOverride<T>)overrides.get(type);
        }
    }

    public interface BeanOverride<T> {
        T getBean(ListableBeanFactory beanFactory);
    }

    public record OverrideByBeanName<T>(Class<T> type, String name) implements BeanOverride<T> {
        @Override
        public T getBean(ListableBeanFactory beanFactory) {
            return beanFactory.getBean(name, type);
        }
    }

    public record OverrideByClass<T, I extends T>(Class<I> type) implements BeanOverride<T> {
        @Override
        public T getBean(ListableBeanFactory beanFactory) {
            return beanFactory.getBean(type);
        }
    }


}
