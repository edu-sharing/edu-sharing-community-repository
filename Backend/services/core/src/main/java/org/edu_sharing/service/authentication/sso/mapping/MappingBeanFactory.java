package org.edu_sharing.service.authentication.sso.mapping;

import com.typesafe.config.*;

public class MappingBeanFactory {

    public static Mapping getMapping(Config config) {
        Mapping mapping = new Mapping();
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
