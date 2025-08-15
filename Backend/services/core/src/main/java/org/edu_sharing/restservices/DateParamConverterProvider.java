package org.edu_sharing.restservices;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

@Provider
public class DateParamConverterProvider implements ParamConverterProvider {

    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (rawType.equals(Date.class)) {
            return new ParamConverter<>() {
                @Override
                public T fromString(String value) {
                    if (value == null) {
                        return null;
                    }
                    try {
                        OffsetDateTime odt = OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                        return rawType.cast(Date.from(odt.toInstant()));
                    } catch (DateTimeParseException e) {
                        throw new WebApplicationException("Invalid date format", Response.Status.BAD_REQUEST);
                    }
                }

                @Override
                public String toString(T value) {
                    Date date = (Date) value;
                    return date.toInstant().atOffset(ZoneId.systemDefault().getRules().getOffset(date.toInstant()))
                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                }
            };
        }
        return null;
    }
}

