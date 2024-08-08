package ru.bublinoid.thenails.utils;

import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Historical {

    Logger log = LoggerFactory.getLogger(Historical.class);

    void setHash(UUID uuid);
    UUID getHash();

    static UUID md5(List<?> objects) {
        final String hashFieldValues = objects.stream()
                .map(Objects::toString)
                .map(String::toUpperCase)
                .collect(Collectors.joining(","));
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(hashFieldValues.getBytes(Charset.forName("UTF-8")));

            ByteBuffer byteBuffer = ByteBuffer.wrap(md.digest());
            final long high = byteBuffer.getLong();
            final long low = byteBuffer.getLong();
            return new UUID(high, low);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 provider is required on your JVM");
        }
    }

    default Stream<Object> getHashObjects() {
        final Class<? extends Historical> clazz = this.getClass();
        List<Object> fieldValues = new ArrayList<>();
        for (Field f : clazz.getDeclaredFields()) {
            if (f.isAnnotationPresent(HashField.class)) {
                f.setAccessible(true);
                try {
                    fieldValues.add(f.get(this));
                } catch (IllegalAccessException e) {
                    log.error("Error accessing field", e);
                }
            }
        }
        if (fieldValues.isEmpty()) {
            throw new IllegalStateException("No fields annotated with @HashField found in " + clazz.getName());
        }
        return fieldValues.stream();
    }

    default UUID generateHash() {
        val thisHashObjects = this.getHashObjects().collect(Collectors.toList());
        return Historical.md5(thisHashObjects);
    }

}
