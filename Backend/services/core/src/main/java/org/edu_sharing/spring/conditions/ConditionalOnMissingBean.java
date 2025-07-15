package org.edu_sharing.spring.conditions;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;

import java.lang.annotation.*;


/**
  * @Conditional that only matches when no beans meeting the specified requirements are already contained in the BeanFactory. None of the requirements must be met for the condition to match and the requirements do not have to be met by the same bean.
  * When placed on a @Bean method and none of value, type, name, or annotation has been specified, the bean type to match defaults to the return type of the @Bean method:
  *   @Configuration
  *   public class MyAutoConfiguration {
  *
  *       @ConditionalOnMissingBean
  *       @Bean
  *       public MyService myService() {
  *           ...
  *       }
  *
  *   }
  * In the sample above the condition will match if no bean of type MyService is already contained in the BeanFactory.
  * The condition can only match the bean definitions that have been processed by the application context so far and, as such, it is strongly recommended to use this condition on auto-configuration classes only. If a candidate bean may be created by another auto-configuration, make sure that the one using this condition runs after.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional({OnBeanCondition.class})
public @interface ConditionalOnMissingBean {
    /**
     * The class types of beans that should be checked. The condition matches when no bean
     * of each class specified is contained in the {@link BeanFactory}. Beans that are not
     * autowire candidates or that are not default candidates are ignored.
     *
     * @return the class types of beans to check
     * @see Bean#autowireCandidate()
     * @see BeanDefinition#isAutowireCandidate
     * @see Bean#defaultCandidate()
     * @see AbstractBeanDefinition#isDefaultCandidate
     */
    Class<?>[] value() default {};

    /**
     * The class type names of beans that should be checked. The condition matches when no
     * bean of each class specified is contained in the {@link BeanFactory}. Beans that
     * are not autowire candidates or that are not default candidates are ignored.
     *
     * @return the class type names of beans to check
     * @see Bean#autowireCandidate()
     * @see BeanDefinition#isAutowireCandidate
     * @see Bean#defaultCandidate()
     * @see AbstractBeanDefinition#isDefaultCandidate
     */
    String[] type() default {};

    /**
     * The class types of beans that should be ignored when identifying matching beans.
     *
     * @return the class types of beans to ignore
     * @since 1.2.5
     */
    Class<?>[] ignored() default {};

    /**
     * The class type names of beans that should be ignored when identifying matching
     * beans.
     *
     * @return the class type names of beans to ignore
     * @since 1.2.5
     */
    String[] ignoredType() default {};

    /**
     * The annotation type decorating a bean that should be checked. The condition matches
     * when each annotation specified is missing from all beans in the
     * {@link BeanFactory}. Beans that are not autowire candidates or that are not default
     * candidates are ignored.
     *
     * @return the class-level annotation types to check
     * @see Bean#autowireCandidate()
     * @see BeanDefinition#isAutowireCandidate
     * @see Bean#defaultCandidate()
     * @see AbstractBeanDefinition#isDefaultCandidate
     */
    Class<? extends Annotation>[] annotation() default {};

    /**
     * The names of beans to check. The condition matches when each bean name specified is
     * missing in the {@link BeanFactory}.
     *
     * @return the names of beans to check
     */
    String[] name() default {};

    /**
     * Strategy to decide if the application context hierarchy (parent contexts) should be
     * considered.
     *
     * @return the search strategy
     */
    SearchStrategy search() default SearchStrategy.ALL;

    /**
     * Additional classes that may contain the specified bean types within their generic
     * parameters. For example, an annotation declaring {@code value=Name.class} and
     * {@code parameterizedContainer=NameRegistration.class} would detect both
     * {@code Name} and {@code NameRegistration<Name>}.
     *
     * @return the container types
     * @since 2.1.0
     */
    Class<?>[] parameterizedContainer() default {};
}
