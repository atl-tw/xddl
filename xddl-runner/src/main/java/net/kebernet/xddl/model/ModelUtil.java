package net.kebernet.xddl.model;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;

import static java.util.Optional.ofNullable;

public abstract class ModelUtil {

    public static <T> void maybeSet(Consumer<T> consumer, T value, T defaultValue){
        consumer.accept(ofNullable(value).orElse(defaultValue));
    }

    public static <T> void maybeSet(Consumer<T> consumer, JsonNode node, String name,
                                    Function<JsonNode, T> reader, T defaultValue){
        if(node.has(name)){
            consumer.accept(reader.apply(node.get(name)));
        } else {
            consumer.accept(defaultValue);
        }

    }

    public static <T> void maybeSet(Consumer<T> consumer, JsonNode node, String name,
                                    Function<JsonNode, T> reader){
        if(node.has(name)){
            consumer.accept(reader.apply(node.get(name)));
        }

    }

    public static <T extends BaseType> T merge(T newValue, T originalValue, Reference reference){
        maybeSet(newValue::setName, reference.getName(), originalValue.getName());
        maybeSet(newValue::setDescription, reference.getDescription(), originalValue.getDescription());
        newValue.setRequired(originalValue.isRequired());
        Map<String, JsonNode> ext = reference.ext();
        ofNullable(originalValue.getExt()).ifPresent(ext::putAll);
        newValue.setExt(ext);
        return newValue;
    }

}
