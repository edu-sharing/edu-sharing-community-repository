package org.edu_sharing.service.authentication.sso.mapping;

import com.typesafe.config.*;
import org.edu_sharing.service.authentication.SSOAuthorityMapper;

public class MappingBeanFactory {

    public static final String SETTINGS = "settings";

    /**
     * creates the mapping found at path, falls back to an empty mapping when the path is not configured
     */
    public static Mapping getMapping(Config rootConfig, String path) {
        Config config = rootConfig.hasPath(path) ? rootConfig.getConfig(path) : ConfigFactory.empty();
        return getMapping(config, rootConfig.getConfig(SSOAuthorityMapper.CONFIG_PATH));
    }

    /**
     * @param defaultSettings the security.sso.settings block. settings configured on the mapping are merged on top of it
     */
    public static Mapping getMapping(Config config, Config defaultSettings) {
        Mapping mapping = new Mapping();
        Config settings = config.hasPath(SETTINGS) ? config.getConfig(SETTINGS).withFallback(defaultSettings) : defaultSettings;
        mapping.setSettings(ConfigBeanFactory.create(settings, SSOAuthorityMapper.Configuration.class));

        if(config.hasPath("preferRemoteUser")) {
            mapping.setPreferRemoteUser(config.getBoolean("preferRemoteUser"));
        }
        if(config.hasPath("person")){
            config.getObject("person").forEach((key, value) -> {
                if(key.equals("additionalKeyValues")){
                    handleAdditionValues(mapping, value);
                    return;
                }
                if(value.valueType().equals(ConfigValueType.OBJECT)){
                    throw new IllegalStateException("person mapping must be a native type");
                }

                if(value.valueType().equals(ConfigValueType.LIST)){
                    throw new IllegalStateException("person mapping must be a native type");
                }

                if(value.valueType().equals(ConfigValueType.NULL)){
                    return;
                }

                mapping.getPerson().put(key, value.unwrapped().toString());
            });
        }

        if(config.hasPath("group")){
            config.getObject("group").forEach((key, value) -> {
                if(value.valueType().equals(ConfigValueType.OBJECT)){
                    throw new IllegalStateException("group mapping must be a native type");
                }
                Mapping.Group group = ConfigBeanFactory.create(((ConfigObject) value).toConfig(), Mapping.Group.class);
                group.setGroup(key);
                mapping.getGroup().put(key, group);
            });
        }



        return mapping;
    }

    private static void handleAdditionValues(Mapping mapping, ConfigValue configValue) {
        if(!configValue.valueType().equals(ConfigValueType.OBJECT)){
            throw new IllegalStateException("additionalValues must be an object");
        }

        ((ConfigObject) configValue).forEach((key, value) -> {
            if(value.valueType().equals(ConfigValueType.OBJECT)){
                throw new IllegalStateException("additionalValues must be a native type");
            }
            if(value.valueType().equals(ConfigValueType.LIST)){
                throw new IllegalStateException("additionalValues must be a native type");
            }
            if(value.valueType().equals(ConfigValueType.NULL)){
                return;
            }

            mapping.getAdditionalKeyValues().put(key, value.unwrapped().toString());
        });
    }
}
