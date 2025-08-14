package org.edu_sharing.restservices;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
                        return rawType.cast(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").parse(value));
                    } catch (ParseException e) {
                        throw new WebApplicationException("Invalid date format", Response.Status.BAD_REQUEST);
                    }
                }

                @Override
                public String toString(T value) {
                    return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format((Date) value);
                }
            };
        }
        return null;
    }
}

