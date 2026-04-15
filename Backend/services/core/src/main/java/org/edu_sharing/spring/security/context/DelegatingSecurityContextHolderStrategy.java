package org.edu_sharing.spring.security.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.core.context.SecurityContextImpl;

@Slf4j
public class DelegatingSecurityContextHolderStrategy implements SecurityContextHolderStrategy {
    private static final SecurityContextHolderStrategy DEFAULT_STRATEGY =
            new AcegiBackedSecurityContextHolderStrategy();

    private static final SecurityContextHolderStrategy THREAD_LOCAL_STRATEGY =
            new ThreadLocalSecurityContextHolderStrategy();


    private static final InheritableThreadLocal<SecurityContextHolderStrategy> CURRENT_STRATEGY =
            new InheritableThreadLocal<>();

    private static SecurityContextHolderStrategy getActiveStrategy() {
        SecurityContextHolderStrategy strategy = CURRENT_STRATEGY.get();
        return (strategy != null) ? strategy : DEFAULT_STRATEGY;
    }

    public static void useAcegiStrategyForCurrentThread() {
        CURRENT_STRATEGY.set(DEFAULT_STRATEGY);
    }

    public static void useThreadLocalStrategyForCurrentThread() {
        CURRENT_STRATEGY.set(THREAD_LOCAL_STRATEGY);
    }

    public static void clearStrategyForCurrentThread() {
        CURRENT_STRATEGY.remove();
    }

    public static void runWithStrategy(SecurityContextHolderStrategy strategy, Runnable runnable) {
        SecurityContextHolderStrategy previous = CURRENT_STRATEGY.get();
        try {
            CURRENT_STRATEGY.set(strategy);
            runnable.run();
        } finally {
            if (previous != null) {
                CURRENT_STRATEGY.set(previous);
            } else {
                CURRENT_STRATEGY.remove();
            }
        }
    }

    @Override
    public void clearContext() {
        getActiveStrategy().clearContext();
    }

    @Override
    public SecurityContext getContext() {
        return getActiveStrategy().getContext();
    }

    @Override
    public void setContext(SecurityContext context) {
        getActiveStrategy().setContext(context);
    }

    @Override
    public SecurityContext createEmptyContext() {
        // Nur zur Sicherheit – i.d.R. delegiert getActiveStrategy().createEmptyContext()
        return new SecurityContextImpl();
    }
}
