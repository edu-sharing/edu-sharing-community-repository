package org.edu_sharing.spring.converter;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.unit.DataSize;

import java.util.Collections;
import java.util.Set;

public class NumberToDataSizeConverter implements GenericConverter {
    private final StringToDataSizeConverter delegate = new StringToDataSizeConverter();

    @Override
    public Set<GenericConverter.ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new GenericConverter.ConvertiblePair(Number.class, DataSize.class));
    }

    @Override
    public Object convert(Object source, @NotNull TypeDescriptor sourceType, @NotNull TypeDescriptor targetType) {
        return this.delegate.convert((source != null) ? source.toString() : null, TypeDescriptor.valueOf(String.class),
                targetType);
    }
}
