package org.edu_sharing.util;

import com.drew.lang.annotations.NotNull;
import lombok.NonNull;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * A thread-safe utility class that provides lazy initialization for a value.
 * The value is initialized only once when it is accessed for the first time,
 * using the provided {@link Supplier}.
 *
 * @param <T> the type of the value to be provided
 */
public class LazyProvider<T> {
    private final AtomicReference<T> value;
    private final Supplier<T> supplier;


    /**
     * Constructs a new {@code LazyProvider} with the provided supplier.
     * The supplier is used to lazily initialize the value when it is accessed for the first time.
     * This constructor ensures thread-safe lazy initialization.
     *
     * @param supplier the supplier to provide the value when first accessed
     *                 (must not be {@code null})
     */
    public LazyProvider(@NotNull @NonNull Supplier<T> supplier) {
        this.supplier = supplier;
        this.value = new AtomicReference<>();
    }

    /**
     * Returns the lazily initialized value, creating it if it has not already been initialized.
     * The value is created using the provided {@link Supplier} and is thread-safe.
     *
     * @return the lazily initialized value of type {@code T}
     */
    public T get() {
        if(value.get() == null) {
            synchronized(value) {
                if(value.get() == null) {
                    value.set(supplier.get());
                }
            }
        }
        return value.get();
    }

}
