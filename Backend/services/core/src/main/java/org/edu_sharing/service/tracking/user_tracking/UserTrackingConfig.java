package org.edu_sharing.service.tracking.user_tracking;

import org.edu_sharing.spring.conditions.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Fallback;

@Configuration
public class UserTrackingConfig {

    /**
     * Fallback, falls kein Plugin (z.B. plugin-mongo) eine eigene {@link UserNodeActivityDataService}
     * Implementierung liefert. Muss als {@code @Bean}-Methode registriert werden, da
     * {@code @ConditionalOnMissingBean} auf gescannten Klassen sich selbst als bereits
     * vorhandenen Bean erkennt und die Definition dadurch verwirft (siehe CacheConfig).
     * {@code @Fallback} sorgt zusätzlich dafür, dass ein von einem Plugin per
     * {@code @Bean}-Methode registrierter Bean beim Autowiring Vorrang hat, falls beide
     * Definitionen aus Registrierungsreihenfolge-Gründen gleichzeitig existieren sollten.
     */
    @Bean
    @Fallback
    @ConditionalOnMissingBean(UserNodeActivityDataService.class)
    public UserNodeActivityDataService defaultUserNodeActivityDataService() {
        return new DefaultUserNodeActivityDataService();
    }
}
