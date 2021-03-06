package org.jongo.marshall.jackson.configuration;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.undercouch.bson4jackson.BsonFactory;
import org.jongo.Provider;
import org.jongo.marshall.jackson.BsonProvider;
import org.jongo.marshall.jackson.JsonProvider;
import org.jongo.marshall.jackson.bson4jackson.BsonModule;
import org.jongo.marshall.jackson.bson4jackson.MongoBsonFactory;

import java.util.ArrayList;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.MapperFeature.AUTO_DETECT_GETTERS;
import static com.fasterxml.jackson.databind.MapperFeature.AUTO_DETECT_SETTERS;
import static org.jongo.marshall.jackson.configuration.JacksonProviders.Type.BSON;
import static org.jongo.marshall.jackson.configuration.JacksonProviders.Type.JSON;

public class JacksonProviders {

    public static Builder usingBson() {
        return Builder.usingBson();
    }

    public static Builder usingJson() {
        return Builder.usingJson();
    }

    public enum Type {
        BSON, JSON;
    }

    public static class Builder {
        private final Type type;
        private final SimpleModule module;
        private final ArrayList<MapperModifier> modifiers;
        private final ObjectMapper mapper;
        private ReaderCallback readerCallback;
        private WriterCallback writerCallback;

        public Builder(ObjectMapper mapper, Type type) {
            this.type = type;
            this.mapper = mapper;
            this.module = new SimpleModule("jongo-custom-module");
            this.modifiers = new ArrayList<MapperModifier>();
            this.readerCallback = new DefaultReaderCallback();
            this.writerCallback = new DefaultWriterCallback();
            addModule(module);
        }

        public static Builder usingJson() {
            return new Builder(new ObjectMapper(), JSON)
                    .addModule(new JsonModule())
                    .addModifier(new SerializationModifier())
                    .addModifier(new DeserializationModifier());
        }

        public static Builder usingBson() {
            BsonFactory bsonFactory = MongoBsonFactory.createFactory();
            return new Builder(new ObjectMapper(bsonFactory), BSON)
                    .addModule(new BsonModule())
                    .addModifier(new SerializationModifier())
                    .addModifier(new DeserializationModifier());
        }

        public MappingConfig innerConfig() {
            for (MapperModifier modifier : modifiers) {
                modifier.modify(mapper);
            }
            return new MappingConfig(mapper, readerCallback, writerCallback);
        }

        public Provider build() {
            MappingConfig config = innerConfig();
            if (type == BSON)
                return new BsonProvider(config);
            else
                return new JsonProvider(config);
        }

        public <T> Builder addDeserializer(Class<T> type, JsonDeserializer<T> deserializer) {
            module.addDeserializer(type, deserializer);
            return this;
        }

        public <T> Builder addSerializer(Class<T> type, JsonSerializer<T> serializer) {
            module.addSerializer(type, serializer);
            return this;
        }

        public Builder addModule(final Module module) {
            modifiers.add(new MapperModifier() {
                public void modify(ObjectMapper mapper) {
                    mapper.registerModule(module);
                }
            });
            return this;
        }

        public Builder addModifier(MapperModifier modifier) {
            modifiers.add(modifier);
            return this;
        }

        public Builder setReaderCallback(ReaderCallback readerCallback) {
            this.readerCallback = readerCallback;
            return this;
        }

        public Builder setWriterCallback(WriterCallback writerCallback) {
            this.writerCallback = writerCallback;
            return this;
        }

        private static class DefaultReaderCallback implements ReaderCallback {
            public ObjectReader getReader(ObjectMapper mapper, Class<?> clazz) {
                return mapper.reader(clazz);
            }
        }

        private static class DefaultWriterCallback implements WriterCallback {
            public ObjectWriter getWriter(ObjectMapper mapper, Object pojo) {
                return mapper.writer();
            }
        }

        public static final class DeserializationModifier implements MapperModifier {

            public void modify(ObjectMapper mapper) {
                mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
                mapper.configure(AUTO_DETECT_SETTERS, false);
            }
        }

        public static final class SerializationModifier implements MapperModifier {

            public void modify(ObjectMapper mapper) {

                mapper.configure(AUTO_DETECT_GETTERS, false);
                mapper.setSerializationInclusion(NON_NULL);
                VisibilityChecker<?> checker = mapper.getSerializationConfig().getDefaultVisibilityChecker();
                mapper.setVisibilityChecker(checker.withFieldVisibility(JsonAutoDetect.Visibility.ANY));
            }
        }
    }
}