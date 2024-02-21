package org.edu_sharing.redisson;

import com.esotericsoftware.kryo.Kryo;
import org.edu_sharing.redisson.serializer.CollectionSerializer;
import org.edu_sharing.redisson.serializer.MapSerializer;


import java.util.Collection;
import java.util.Map;

/**
 *
 * spring security uses unmodifiable lists and maps for objects that are saved in session:
 *   org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest
 *   org.springframework.security.oauth2.core.user.OAuth2UserAuthority
 *
 * when redis is enabled for session management these objects are created on read by reflection and tried to be filled again.
 * unmodifiable objects can not be filled so an Exception is thrown:
 * Caused by: java.lang.UnsupportedOperationException
 * 	at java.base/java.util.Collections$UnmodifiableMap.put(Collections.java:1505)
 * 	at com.esotericsoftware.kryo.serializers.MapSerializer.read(MapSerializer.java:236)
 * 	at com.esotericsoftware.kryo.serializers.MapSerializer.read(MapSerializer.java:42)
 *
 * our fix is to override the default serializers for Collection.class and Map.class
 */
public class Kryo5Codec extends org.redisson.codec.Kryo5Codec{

    public Kryo5Codec() {
        super(null);
    }

    public Kryo5Codec(ClassLoader classLoader, Kryo5Codec codec) {
        super(classLoader,codec);
    }

    public Kryo5Codec(ClassLoader classLoader) {
        super(classLoader);
    }

    @Override
    protected Kryo createKryo(ClassLoader classLoader) {
        Kryo kryo =  super.createKryo(classLoader);
        kryo.addDefaultSerializer(Collection.class, CollectionSerializer.class);
        kryo.addDefaultSerializer(Map.class, MapSerializer.class);
        return kryo;
    }
}
