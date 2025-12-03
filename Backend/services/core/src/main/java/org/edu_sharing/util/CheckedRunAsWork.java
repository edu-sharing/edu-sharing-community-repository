package org.edu_sharing.util;


import org.alfresco.repo.security.authentication.AuthenticationUtil;

import java.util.function.Function;
import java.util.function.Supplier;


/**
 * A functional interface representing a supplier of results with the ability to throw a checked exception.
 * This can be used to create or fetch a value while managing exceptions in a functional programming style.
 *
 * @param <T> the type of results supplied by this supplier
 * @param <E> the type of exception that may be thrown by this supplier
 */
@FunctionalInterface
public interface CheckedRunAsWork<T, E extends Throwable> {
    /**
     * Gets a result.
     *
     * @return a result
     * @throws E thrown by this function
     */
    T get() throws E;

    /**
     * Wraps a {@link CheckedRunAsWork} into a {@link Supplier}, converting any exception
     * thrown by the {@code CheckedRunAsWork} into a runtime exception.
     *
     * @param <T>             the type of the result supplied by the {@code CheckedSupplier}
     * @param <E>             the type of the exception potentially thrown by the {@code CheckedRunAsWork}
     * @param checkedRunAsWork the {@code CheckedRunAsWork} to wrap
     * @return a {@link Supplier} that either provides the result of the {@code CheckedRunAsWork}
     * or throws a runtime exception
     */
    static <T, E extends Throwable> AuthenticationUtil.RunAsWork<T> wrap(CheckedRunAsWork<T, E> checkedRunAsWork) {
        return () -> {
            try {
                return checkedRunAsWork.get();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Wraps a {@link CheckedRunAsWork} into a {@link Supplier} and translates any exception thrown by the
     * {@code CheckedRunAsWork} into a runtime exception using the provided exception handler.
     *
     * @param <T>              the type of the result supplied by the {@code CheckedRunAsWork}
     * @param <E>              the type of the exception potentially thrown by the {@code CheckedRunAsWork}
     * @param <ER>             the type of the runtime exception to be thrown when an exception occurs
     * @param checkedRunAsWork  the {@code CheckedRunAsWork} to wrap
     * @param exceptionHandler a {@link Function} that converts the thrown exception into a runtime exception
     * @return a {@link Supplier} that either provides the result of the {@code CheckedRunAsWork} or throws a runtime exception
     */
    static <T, E extends Throwable, ER extends RuntimeException> Supplier<T> wrapOrThrow(CheckedRunAsWork<T, E> checkedRunAsWork, Function<Throwable, ER> exceptionHandler) {
        return () -> {
            try {
                return checkedRunAsWork.get();
            } catch (Throwable e) {
                throw exceptionHandler.apply(e);
            }
        };
    }


    /**
     * Wraps a {@link CheckedRunAsWork} into a {@link Supplier} that provides a default value in case an exception is thrown.
     *
     * @param <T>             the type of the result supplied by the {@code CheckedRunAsWork}
     * @param <E>             the type of the exception potentially thrown by the {@code CheckedRunAsWork}
     * @param checkedRunAsWork the {@link CheckedRunAsWork} to wrap
     * @param defaultValue    the default value to return if the {@code CheckedRunAsWork} throws an exception
     * @return a {@link Supplier} that provides either the result of the {@code CheckedRunAsWork} or the default value
     */
    static <T, E extends Throwable> Supplier<T> wrap(CheckedRunAsWork<T, E> checkedRunAsWork, T defaultValue) {
        return () -> {
            try {
                return checkedRunAsWork.get();
            } catch (Throwable e) {
                return defaultValue;
            }
        };
    }
}
