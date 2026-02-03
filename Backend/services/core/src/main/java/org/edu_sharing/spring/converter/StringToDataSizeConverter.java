package org.edu_sharing.spring.converter;

import org.jetbrains.annotations.NotNull;
import org.springframework.boot.convert.DataSizeUnit;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.GenericConverter;
import org.springframework.util.ObjectUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;

import java.util.Collections;
import java.util.Set;

public class StringToDataSizeConverter implements GenericConverter {
    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return Collections.singleton(new ConvertiblePair(String.class, DataSize.class));
    }

    @Override
    public Object convert(Object source, @NotNull TypeDescriptor sourceType, @NotNull TypeDescriptor targetType) {
        if (ObjectUtils.isEmpty(source)) {
            return null;
        }
        return convert(source.toString(), getDataUnit(targetType));
    }

    private DataUnit getDataUnit(TypeDescriptor targetType) {
        DataSizeUnit annotation = targetType.getAnnotation(DataSizeUnit.class);
        return (annotation != null) ? annotation.value() : null;
    }

    private DataSize convert(String source, DataUnit unit) {
        return DataSize.parse(source, unit);
    }
}
