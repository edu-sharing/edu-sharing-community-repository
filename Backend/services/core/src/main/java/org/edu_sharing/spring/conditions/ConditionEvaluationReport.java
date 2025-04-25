package org.edu_sharing.spring.conditions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Records condition evaluation details for reporting and logging.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @since 1.0.0
 */
public final class ConditionEvaluationReport {

    private static final String BEAN_NAME = "autoConfigurationReport";

    private static final ConditionEvaluationReport.AncestorsMatchedCondition ANCESTOR_CONDITION = new ConditionEvaluationReport.AncestorsMatchedCondition();

    private final SortedMap<String, ConditionEvaluationReport.ConditionAndOutcomes> outcomes = new TreeMap<>();

    private boolean addedAncestorOutcomes;

    /**
     * -- GETTER --
     *  The parent report (from a parent BeanFactory if there is one).
     *
     */
    @Getter
    private ConditionEvaluationReport parent;

    private final List<String> exclusions = new ArrayList<>();

    private final Set<String> unconditionalClasses = new HashSet<>();

    /**
     * Private constructor.
     * @see #get(ConfigurableListableBeanFactory)
     */
    private ConditionEvaluationReport() {
    }

    /**
     * Record the occurrence of condition evaluation.
     * @param source the source of the condition (class or method name)
     * @param condition the condition evaluated
     * @param outcome the condition outcome
     */
    public void recordConditionEvaluation(String source, Condition condition, ConditionOutcome outcome) {
        Assert.notNull(source, "Source must not be null");
        Assert.notNull(condition, "Condition must not be null");
        Assert.notNull(outcome, "Outcome must not be null");
        this.unconditionalClasses.remove(source);
        this.outcomes.computeIfAbsent(source, (key) -> new ConditionEvaluationReport.ConditionAndOutcomes()).add(condition, outcome);
        this.addedAncestorOutcomes = false;
    }

    /**
     * Records the names of the classes that have been excluded from condition evaluation.
     * @param exclusions the names of the excluded classes
     */
    public void recordExclusions(Collection<String> exclusions) {
        Assert.notNull(exclusions, "exclusions must not be null");
        this.exclusions.addAll(exclusions);
    }

    /**
     * Records the names of the classes that are candidates for condition evaluation.
     * @param evaluationCandidates the names of the classes whose conditions will be
     * evaluated
     */
    public void recordEvaluationCandidates(List<String> evaluationCandidates) {
        Assert.notNull(evaluationCandidates, "evaluationCandidates must not be null");
        this.unconditionalClasses.addAll(evaluationCandidates);
    }

    /**
     * Returns condition outcomes from this report, grouped by the source.
     * @return the condition outcomes
     */
    public Map<String, ConditionEvaluationReport.ConditionAndOutcomes> getConditionAndOutcomesBySource() {
        if (!this.addedAncestorOutcomes) {
            this.outcomes.forEach((source, sourceOutcomes) -> {
                if (!sourceOutcomes.isFullMatch()) {
                    addNoMatchOutcomeToAncestors(source);
                }
            });
            this.addedAncestorOutcomes = true;
        }
        return Collections.unmodifiableMap(this.outcomes);
    }

    private void addNoMatchOutcomeToAncestors(String source) {
        String prefix = source + "$";
        this.outcomes.forEach((candidateSource, sourceOutcomes) -> {
            if (candidateSource.startsWith(prefix)) {
                ConditionOutcome outcome = ConditionOutcome
                        .noMatch(ConditionMessage.forCondition("Ancestor " + source).because("did not match"));
                sourceOutcomes.add(ANCESTOR_CONDITION, outcome);
            }
        });
    }

    /**
     * Returns the names of the classes that have been excluded from condition evaluation.
     * @return the names of the excluded classes
     */
    public List<String> getExclusions() {
        return Collections.unmodifiableList(this.exclusions);
    }

    /**
     * Returns the names of the classes that were evaluated but were not conditional.
     * @return the names of the unconditional classes
     */
    public Set<String> getUnconditionalClasses() {
        Set<String> filtered = new HashSet<>(this.unconditionalClasses);
        this.exclusions.forEach(filtered::remove);
        return Collections.unmodifiableSet(filtered);
    }

    /**
     * Attempt to find the {@link ConditionEvaluationReport} for the specified bean
     * factory.
     * @param beanFactory the bean factory (may be {@code null})
     * @return the {@link ConditionEvaluationReport} or {@code null}
     */
    public static ConditionEvaluationReport find(BeanFactory beanFactory) {
        if (beanFactory instanceof ConfigurableListableBeanFactory) {
            return ConditionEvaluationReport.get((ConfigurableListableBeanFactory) beanFactory);
        }
        return null;
    }

    /**
     * Obtain a {@link ConditionEvaluationReport} for the specified bean factory.
     * @param beanFactory the bean factory
     * @return an existing or new {@link ConditionEvaluationReport}
     */
    public static ConditionEvaluationReport get(ConfigurableListableBeanFactory beanFactory) {
        synchronized (beanFactory) {
            ConditionEvaluationReport report;
            if (beanFactory.containsSingleton(BEAN_NAME)) {
                report = beanFactory.getBean(BEAN_NAME, ConditionEvaluationReport.class);
            }
            else {
                report = new ConditionEvaluationReport();
                beanFactory.registerSingleton(BEAN_NAME, report);
            }
            locateParent(beanFactory.getParentBeanFactory(), report);
            return report;
        }
    }

    private static void locateParent(BeanFactory beanFactory, ConditionEvaluationReport report) {
        if (beanFactory != null && report.parent == null && beanFactory.containsBean(BEAN_NAME)) {
            report.parent = beanFactory.getBean(BEAN_NAME, ConditionEvaluationReport.class);
        }
    }

    public ConditionEvaluationReport getDelta(ConditionEvaluationReport previousReport) {
        ConditionEvaluationReport delta = new ConditionEvaluationReport();
        this.outcomes.forEach((source, sourceOutcomes) -> {
            ConditionEvaluationReport.ConditionAndOutcomes previous = previousReport.outcomes.get(source);
            if (previous == null || previous.isFullMatch() != sourceOutcomes.isFullMatch()) {
                sourceOutcomes.forEach((conditionAndOutcome) -> delta.recordConditionEvaluation(source,
                        conditionAndOutcome.getCondition(), conditionAndOutcome.getOutcome()));
            }
        });
        List<String> newExclusions = new ArrayList<>(this.exclusions);
        newExclusions.removeAll(previousReport.getExclusions());
        delta.recordExclusions(newExclusions);
        List<String> newUnconditionalClasses = new ArrayList<>(this.unconditionalClasses);
        newUnconditionalClasses.removeAll(previousReport.unconditionalClasses);
        delta.unconditionalClasses.addAll(newUnconditionalClasses);
        return delta;
    }

    /**
     * Provides access to a number of {@link ConditionEvaluationReport.ConditionAndOutcome} items.
     */
    public static class ConditionAndOutcomes implements Iterable<ConditionEvaluationReport.ConditionAndOutcome> {

        private final Set<ConditionEvaluationReport.ConditionAndOutcome> outcomes = new LinkedHashSet<>();

        public void add(Condition condition, ConditionOutcome outcome) {
            this.outcomes.add(new ConditionEvaluationReport.ConditionAndOutcome(condition, outcome));
        }

        /**
         * Return {@code true} if all outcomes match.
         * @return {@code true} if a full match
         */
        public boolean isFullMatch() {
            for (ConditionEvaluationReport.ConditionAndOutcome conditionAndOutcomes : this) {
                if (!conditionAndOutcomes.getOutcome().isMatch()) {
                    return false;
                }
            }
            return true;
        }

        @NotNull
        @Override
        public Iterator<ConditionEvaluationReport.ConditionAndOutcome> iterator() {
            return Collections.unmodifiableSet(this.outcomes).iterator();
        }

    }

    /**
     * Provides access to a single {@link Condition} and {@link ConditionOutcome}.
     */
    @Getter
    public static class ConditionAndOutcome {

        private final Condition condition;

        private final ConditionOutcome outcome;

        public ConditionAndOutcome(Condition condition, ConditionOutcome outcome) {
            this.condition = condition;
            this.outcome = outcome;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            ConditionEvaluationReport.ConditionAndOutcome other = (ConditionEvaluationReport.ConditionAndOutcome) obj;
            return (ObjectUtils.nullSafeEquals(this.condition.getClass(), other.condition.getClass())
                    && ObjectUtils.nullSafeEquals(this.outcome, other.outcome));
        }

        @Override
        public int hashCode() {
            return this.condition.getClass().hashCode() * 31 + this.outcome.hashCode();
        }

        @Override
        public String toString() {
            return this.condition.getClass() + " " + this.outcome;
        }

    }

    private static final class AncestorsMatchedCondition implements Condition {

        @Override
        public boolean matches(@NotNull ConditionContext context, @NotNull AnnotatedTypeMetadata metadata) {
            throw new UnsupportedOperationException();
        }

    }

}
