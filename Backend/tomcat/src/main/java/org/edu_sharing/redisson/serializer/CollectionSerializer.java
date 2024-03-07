package org.edu_sharing.redisson.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class CollectionSerializer extends com.esotericsoftware.kryo.serializers.CollectionSerializer{
    @Override
    protected Collection create(Kryo kryo, Input input, Class type, int size) {
        //edu-sharing fix
        if(type.getName().contains("UnmodifiableRandomAccessList")){
            return new ArrayList<>(size);
        }
        if(type.getName().contains("UnmodifiableSet")){
            return new HashSet<>(size);
        }
        return super.create(kryo, input, type, size);
    }
}
