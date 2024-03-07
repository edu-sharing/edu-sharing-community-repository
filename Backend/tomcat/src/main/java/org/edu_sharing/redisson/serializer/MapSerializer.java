package org.edu_sharing.redisson.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import java.util.HashMap;
import java.util.Map;

public class MapSerializer extends com.esotericsoftware.kryo.serializers.MapSerializer{
    @Override
    protected Map create(Kryo kryo, Input input, Class type, int size) {
        if(type.getName().contains("UnmodifiableMap")){
            return new HashMap<>(size);
        }

        return super.create(kryo, input, type, size);
    }
}
