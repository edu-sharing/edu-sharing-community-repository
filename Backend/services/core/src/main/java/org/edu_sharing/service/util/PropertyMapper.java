package org.edu_sharing.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * The {@code PropertyMapper} class provides a utility for accessing and transforming
 * properties stored in a {@link Map}. It offers methods to retrieve properties in
 * various formats (e.g., {@code String}, {@code Integer}, {@code Long}, {@code Boolean},
 * or {@code List}) with support for default values.
 *
 * <p>This class is designed to simplify property access by handling common scenarios
 * such as missing keys, default value fallbacks, and type casting. It ensures that the
 * retrieved values conform to the expected types, while suppressing warnings for
 * unchecked casts where necessary.</p>
 *
 * <h2>Key Features:</h2>
 * <ul>
 *   <li>Retrieve properties as {@code String}, {@code Integer}, {@code Long},
 *       {@code Boolean}, or {@code List}.</li>
 *   <li>Support for default values when properties are missing or invalid.</li>
 *   <li>Handles both single values and lists seamlessly.</li>
 * </ul>
 *
 * <h2>Usage:</h2>
 * <pre>
 * {@code
 * Map<String, Object> properties = Map.of(
 *     "key1", "value1",
 *     "key2", List.of("value2a", "value2b"),
 *     "key3", 42
 * );
 *
 * PropertyMapper mapper = new PropertyMapper(properties);
 *
 * String value1 = mapper.getString("key1");
 * List<String> value2 = mapper.getStringList("key2");
 * Integer value3 = mapper.getInteger("key3");
 * Boolean missingValue = mapper.getBoolean("nonExistentKey", false);
 * }
 * </pre>
 *
 * <h2>Method Details:</h2>
 * <ul>
 *   <li>{@link #getString(String)}: Retrieves a property as a {@code String}.</li>
 *   <li>{@link #getInteger(String)}: Retrieves a property as an {@code Integer}.</li>
 *   <li>{@link #getLong(String)}: Retrieves a property as a {@code Long}.</li>
 *   <li>{@link #getBoolean(String)}: Retrieves a property as a {@code Boolean}.</li>
 *   <li>{@link #getStringList(String)}: Retrieves a property as a {@code List<String>}.</li>
 * </ul>
 *
 * @see Map
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class PropertyMapper {
    private final Map<String, Object> properties;

    /**
     * Retrieves a property as a {@code List<String>}.
     *
     * @param key the key of the property to retrieve.
     * @return the property value as a {@code List<String>}, or an empty list if the key is not found.
     */
    @NotNull
    public List<String> getStringList(String key) {
        return getAsList(key);
    }

    /**
     * Retrieves a property as a {@code List<String>} with a default value.
     *
     * @param key          the key of the property to retrieve.
     * @param defaultValue the default value to return if the key is not found or the value is {@code null}.
     * @return the property value as a {@code List<String>}, or the default value if the key is not found.
     */
    @NotNull
    public List<String> getStringList(String key, List<String> defaultValue) {
        return getAsList(key, defaultValue);
    }

    /**
     * Retrieves a property as a {@code String}.
     *
     * @param name the key of the property to retrieve.
     * @return the property value as a {@code String}, or {@code null} if the key is not found.
     */
    public String getString(String name) {
        return getAsSingleValue(name, null);
    }

    /**
     * Retrieves a property as a {@code String} with a default value.
     *
     * @param name         the key of the property to retrieve.
     * @param defaultValue the default value to return if the key is not found or the value is {@code null}.
     * @return the property value as a {@code String}, or the default value if the key is not found.
     */
    public String getString(String name, String defaultValue) {
        return getAsSingleValue(name, defaultValue);
    }

    /**
     * Retrieves a property as an {@code Integer}.
     *
     * @param name the key of the property to retrieve.
     * @return the property value as an {@code Integer}, or {@code null} if the key is not found.
     */
    public Integer getInteger(String name) {
        return getInteger(name, null);
    }

    /**
     * Retrieves a property as an {@code Integer} with a default value.
     *
     * @param name         the key of the property to retrieve.
     * @param defaultValue the default value to return if the key is not found or the value is {@code null}.
     * @return the property value as an {@code Integer}, or the default value if the key is not found.
     */
    public Integer getInteger(String name, Integer defaultValue) {
        Object value = getAsSingleValue(name, defaultValue);
        if(value == null) {
            return null;
        }
        if(value instanceof Integer intValue) {
            return intValue;
        }
        if(value instanceof Long longValue) {
            return longValue.intValue();
        }
        if(value instanceof String stringValue) {
            return Integer.parseInt(stringValue);
        }
        throw new IllegalStateException("Cannot convert " + value.getClass().getName() + " to Integer");
    }

    /**
     * Retrieves a property as a {@code Long}.
     *
     * @param name the key of the property to retrieve.
     * @return the property value as a {@code Long}, or {@code null} if the key is not found.
     */
    public Long getLong(String name) {
        return getLong(name, null);
    }

    /**
     * Retrieves a property as a {@code Long} with a default value.
     *
     * @param name         the key of the property to retrieve.
     * @param defaultValue the default value to return if the key is not found or the value is {@code null}.
     * @return the property value as a {@code Long}, or the default value if the key is not found.
     */
    public Long getLong(String name, Long defaultValue) {
        Object value = getAsSingleValue(name, defaultValue);
        if(value == null) {
            return null;
        }
        if(value instanceof Long longValue) {
            return longValue;
        }
        if(value instanceof Integer intValue) {
            return intValue.longValue();
        }
        if(value instanceof String stringValue) {
            return Long.parseLong(stringValue);
        }
        throw new IllegalStateException("Cannot convert " + value.getClass().getName() + " to Long");
    }

    /**
     * Retrieves a property as a {@code Boolean}.
     *
     * @param name the key of the property to retrieve.
     * @return the property value as a {@code Boolean}, or {@code null} if the key is not found.
     */
    public Boolean getBoolean(String name) {
        return getBoolean(name, null);
    }

    /**
     * Retrieves a property as a {@code Boolean} with a default value.
     *
     * @param name         the key of the property to retrieve.
     * @param defaultValue the default value to return if the key is not found or the value is {@code null}.
     * @return the property value as a {@code Boolean}, or the default value if the key is not found.
     */
    public Boolean getBoolean(String name, Boolean defaultValue) {
        Object value = getAsSingleValue(name, defaultValue);
        if (value == null) {
            return null;
        }

        if(value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }

        if(value instanceof Boolean booleanValue) {
            return booleanValue;
        }

        throw new IllegalStateException("Cannot convert " + value.getClass().getName() + " to Boolean");
    }

    @NotNull
    private <T> List<T> getAsList(String name) {
        return getAsList(name, null);
    }


    /**
     * Retrieves an enum value of the specified type from the properties using the given key.
     *
     * @param name      the key of the property to retrieve.
     * @param enumClass the class of the enum to which the retrieved value will be converted.
     * @param <T>       the type of the enum.
     * @return the enum value of the specified type if found and valid; {@code null} otherwise.
     */
    public <T extends Enum<T>> T getEnum(String name, Class<T> enumClass) {
        String value = getString(name);
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Retrieves a property as a {@code Date}.
     *
     * @param name the key of the property to retrieve.
     * @return the property value as a {@code Date}, or {@code null} if the key is not found.
     */
    public Date getDate(String name) {
        Object value = getAsSingleValue(name, null);
        if (value == null) {
            return null;
        }

        if (value instanceof Date) {
            return (Date) value;
        }

        if (value instanceof String stringValue) {
            try {
                return new Date(Long.parseLong(stringValue));
            } catch (NumberFormatException ignored) {
                OffsetDateTime odt = OffsetDateTime.parse(stringValue, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                return Date.from(odt.toInstant());
            }
        }

        throw new IllegalStateException("Cannot convert " + value.getClass().getName() + " to Date");
    }

    @NotNull
    private <T> List<T> getAsList(String name, List<T> defaultValue) {
        Object value = properties.getOrDefault(name, defaultValue);
        if (value == null) {
            return Collections.emptyList();
        }

        if (value instanceof List) {
            return (List<T>) value;
        }
        return Collections.singletonList((T) value);
    }


    private <T> T getAsSingleValue(String name, T defaultValue) {
        Object value = properties.getOrDefault(name, defaultValue);
        if (value instanceof List) {
            return ((List<?>) properties.get(name))
                    .stream()
                    .findFirst()
                    .map(x -> (T) x)
                    .orElse(defaultValue);
        }
        return (T) value;
    }

}
