package net.myitian.blockyfile.config;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;

public final class ConfigCodec {
    private final LinkedHashMap<String, Pair<ConsumerWithIOException<JsonReader>, ConsumerWithIOException<JsonWriter>>> fieldMap = new LinkedHashMap<>();

    public static void readStringList(JsonReader reader, List<String> list, boolean clearBeforeAdd) throws IOException {
        reader.beginArray();
        if (clearBeforeAdd) {
            list.clear();
        }
        while (true) {
            switch (reader.peek()) {
                case END_ARRAY:
                    reader.endArray();
                    return;
                case END_DOCUMENT:
                    return;
                default:
                    String item = reader.nextString();
                    if (item != null) list.add(item);
                    break;
            }
        }
    }

    public static void writeStringList(JsonWriter writer, List<String> list) throws IOException {
        writer.beginArray();
        for (String item : list) {
            if (item != null) writer.value(item);
        }
        writer.endArray();
    }

    public Map<String, Pair<ConsumerWithIOException<JsonReader>, ConsumerWithIOException<JsonWriter>>> getFieldMap() {
        return fieldMap;
    }

    public boolean deserialize(JsonReader reader) throws IOException {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            return false;
        }
        reader.beginObject();
        Set<String> nameSet = new HashSet<>(fieldMap.size());
        while (reader.peek() == JsonToken.NAME) {
            String name = reader.nextName();
            var pair = fieldMap.get(name);
            if (pair != null) {
                nameSet.add(name);
                pair.getLeft().accept(reader);
            } else {
                reader.skipValue();
            }
        }
        return nameSet.size() == fieldMap.size();
    }

    public boolean serialize(JsonWriter writer) throws IOException {
        writer.beginObject();
        for (var fieldInfo : fieldMap.entrySet()) {
            writer.name(fieldInfo.getKey());
            fieldInfo.getValue().getRight().accept(writer);
        }
        writer.endObject();
        return true;
    }

    @FunctionalInterface
    public interface ConsumerWithIOException<T> {
        void accept(T t) throws IOException;
    }
}