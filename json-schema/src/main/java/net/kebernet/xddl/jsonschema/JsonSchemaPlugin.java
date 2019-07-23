package net.kebernet.xddl.jsonschema;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.kebernet.xddl.jsonschema.model.Definition;
import net.kebernet.xddl.jsonschema.model.Schema;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.CoreType;
import net.kebernet.xddl.model.List;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;
import net.kebernet.xddl.plugins.Plugin;

import static net.kebernet.xddl.model.ModelUtil.maybeSet;

public class JsonSchemaPlugin implements Plugin {

    private static Map<CoreType, String> CORE_TYPES = new HashMap<CoreType, String>() {
        {
            put(CoreType.BIG_DECIMAL, "string");
            put(CoreType.BIG_INTEGER, "string");
            put(CoreType.BOOLEAN, "boolean");
            put(CoreType.INTEGER, "integer");
            put(CoreType.LONG, "integer");
            put(CoreType.FLOAT, "number");
            put(CoreType.DOUBLE, "number");
            put(CoreType.DATETIME, "string");
            put(CoreType.DATE, "string");
            put(CoreType.TEXT, "string");
            put(CoreType.STRING, "string");

        }
    };

    private static Map<CoreType, Function<JsonNode, ? extends Number>> CORE_NUMBER_READERS = new HashMap<CoreType, Function<JsonNode, ? extends Number>>() {
        {
            put(CoreType.BIG_INTEGER, JsonNode::bigIntegerValue);
            put(CoreType.BIG_DECIMAL, (node) -> new BigDecimal(node.asText()));
            put(CoreType.INTEGER, JsonNode::intValue);
            put(CoreType.LONG, JsonNode::longValue);
            put(CoreType.FLOAT, JsonNode::floatValue);
            put(CoreType.DOUBLE, JsonNode::doubleValue);
        }
    };

    @Override
    public String getName() {
        return "json";
    }

    @Override
    public String generateArtifacts(Context context, File outputDirectory) throws IOException {
        File file = new File(outputDirectory, "schema.json");
        file.getParentFile().mkdirs();

        ObjectMapper mapper = context.getMapper();
        mapper.writeValue(file, createSchema(context));
        return "fuck-all";
    }

    Schema createSchema(Context context) {
        final Schema schema = new Schema();
        context.getSpecification()
                .getStructures()
                .forEach(s -> this.visit(context, s, schema));
        return schema;

    }

    private void visit(Context context, Structure s, Schema schema) {
        Definition def = new Definition();
        if (def.getRef() != null) {
            context.stateException("Can't have a reference as a stop level definition", s);
        }
        def.setTitle(s.getName());
        def.setDescription(s.getDescription());
        def.setType("object");
        s.getProperties().forEach(p -> {
            def.properties().put(p.getName(), this.visitBaseType(context, p));
            if (p.isRequired()) {
                def.required().add(p.getName());
            }
        });
        schema.definitions().put(s.getName(), def);

    }

    private Definition visitBaseType(Context context, BaseType p) {
        if (p == null) {
            System.err.println("BASE TYPE IS NULL? WHY IS THAT?");
            return null;
        }
        if (p instanceof Reference) {
            return doReference(context, (Reference) p);
        }
        if (p instanceof List) {
            return doList(context, (List) p);
        }
        if (p instanceof Type) {
            return doType(context, (Type) p);
        }
        throw new IllegalArgumentException("Unknown base type " + p.getName());
    }

    private Definition doList(Context context, List list) {
        Definition def = new Definition();
        def.setTitle(list.getName());
        def.setDescription(list.getDescription());
        def.setType("array");
        doBaseTypeExtensions(context, list, def);
        def.setItems(visitBaseType(context, list.getType()));
        return def;
    }

    private Definition doReference(Context context, Reference reference) {
        if (context.isStructure(reference)) {
            Definition def = new Definition();
            def.setRef("#/definitions/" + reference.getRef());
        } else if (context.isType(reference)) {
            return doType(context, (Type) context.resolveReference(reference));
        }
        throw context.stateException("Unable to resolve reference " + reference.getRef(), reference);
    }

    private Definition doType(Context context, Type type) {
        Definition definition = new Definition();
        definition.setTitle(type.getName());
        definition.setDescription(type.getDescription());
        doTypeExtensions(context, type, definition);
        return definition;
    }

    private void doTypeExtensions(Context context, Type type, Definition definition) {
        this.doBaseTypeExtensions(context, type, definition);
        context.hasPlugin(
                "json",
                type,
                jsonNode -> {
                    maybeSet(definition::setType,
                            jsonNode,
                            "type",
                            JsonNode::asText,
                            CORE_TYPES.get(type.getCore()));
                    maybeSet(definition::setMinimum,
                            jsonNode,
                            "minimum",
                            CORE_NUMBER_READERS.get(type.getCore()));
                    maybeSet(definition::setExclusiveMinimum,
                            jsonNode,
                            "exclusiveMinimum",
                            CORE_NUMBER_READERS.get(type.getCore()));
                    maybeSet(definition::setMaximum,
                            jsonNode,
                            "maximum",
                            CORE_NUMBER_READERS.get(type.getCore()));
                    maybeSet(definition::setExclusiveMaximum,
                            jsonNode,
                            "exclusiveMaximum",
                            CORE_NUMBER_READERS.get(type.getCore()));
                }
                ,
                schemaType -> {
                    definition.setType(CORE_TYPES.get(type.getCore()));
                });
    }

    private void doBaseTypeExtensions(Context context, BaseType type, Definition definition) {
        context.hasPlugin(
                "json",
                type,
                jsonNode -> {
                    if (jsonNode.has("format"))
                        definition.setFormat(jsonNode.get("format").asText());
                    maybeSet(definition::setAdditionalProperties,
                            jsonNode,
                            "additionalProperties",
                            JsonNode::asBoolean);
                    maybeSet(definition::setMinProperties,
                            jsonNode,
                            "minProperties",
                            JsonNode::intValue);
                    maybeSet(definition::setMaxProperties,
                            jsonNode,
                            "maxProperties",
                            JsonNode::intValue);
                    maybeSet(definition::setPattern,
                            jsonNode,
                            "pattern",
                            JsonNode::asText);
                    maybeSet(definition::setMinLength,
                            jsonNode,
                            "minLength",
                            JsonNode::intValue);
                    maybeSet(definition::setMaxLength,
                            jsonNode,
                            "maxLength",
                            JsonNode::intValue);
                },
                (n)->{});


    }

}
