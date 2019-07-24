/*
 * Copyright 2019 Robert Cooper, ThoughtWorks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kebernet.xddl.j2xddl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.NonNull;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.CoreType;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;

import static java.util.Optional.ofNullable;
import static net.kebernet.xddl.j2xddl.Reflection.hasAnyOf;
import static net.kebernet.xddl.j2xddl.Reflection.listFields;
import static net.kebernet.xddl.j2xddl.Reflection.nestedType;

public class Generator {

    private static final HashMap<Class, CoreType> DATA_TYPES = new HashMap<>();

    static {
        DATA_TYPES.put(String.class, CoreType.STRING);
        DATA_TYPES.put(CharSequence.class, CoreType.STRING);
        DATA_TYPES.put(Long.class, CoreType.LONG);
        DATA_TYPES.put(long.class, CoreType.LONG);
        DATA_TYPES.put(Short.class, CoreType.INTEGER);
        DATA_TYPES.put(short.class, CoreType.INTEGER);
        DATA_TYPES.put(Integer.class, CoreType.INTEGER);
        DATA_TYPES.put(int.class, CoreType.INTEGER);
        DATA_TYPES.put(Boolean.class, CoreType.BOOLEAN);
        DATA_TYPES.put(boolean.class, CoreType.BOOLEAN);
        DATA_TYPES.put(Double.class, CoreType.DOUBLE);
        DATA_TYPES.put(double.class, CoreType.DOUBLE);
        DATA_TYPES.put(Float.class, CoreType.FLOAT);
        DATA_TYPES.put(float.class, CoreType.FLOAT);
        DATA_TYPES.put(byte[].class, CoreType.BINARY);
        DATA_TYPES.put(BigDecimal.class, CoreType.BIG_DECIMAL);
        DATA_TYPES.put(BigInteger.class, CoreType.BIG_INTEGER);
        DATA_TYPES.put(LocalDate.class, CoreType.DATE);
        DATA_TYPES.put(LocalTime.class, CoreType.TIME);
        DATA_TYPES.put(OffsetTime.class, CoreType.TIME);
        DATA_TYPES.put(LocalDateTime.class, CoreType.DATETIME);
        DATA_TYPES.put(Date.class, CoreType.DATETIME);
        DATA_TYPES.put(Instant.class, CoreType.DATETIME);
    }

    private final ObjectMapper mapper;
    private final HashSet<Class> classes;

    public Generator(ObjectMapper mapper, HashSet<Class> classes) {
        this.mapper = mapper;
        this.classes = classes;
    }

    public Specification generate(){
        Specification specification = new Specification();
        specification.setStructures(classes.stream().map(this::doClass).collect(Collectors.toList()));
        return specification;
    }

    private Structure doClass(Class clazz) {
        return addSwaggerFields(clazz,
                Structure.builder()
                .name(clazz.getSimpleName())
                .properties(
                        listFields(clazz).stream().map(this::resolveDataType).collect(Collectors.toList())
                ).build()
        );

    }


    private <T extends BaseType> T addSwaggerFields(AnnotatedElement element, T type){
        Schema schema = element.getAnnotation(Schema.class);
        if(schema != null){
            type.setRequired(Boolean.TRUE.equals(type.getRequired())|| schema.required());
            notEmpty(schema.description()).ifPresent(type::setDescription);
            Map<String, JsonNode> ext = new HashMap<>();
            if(schema.maxLength()!= Integer.MAX_VALUE) ext.put("maxLength", mapper.valueToTree(schema.maxLength()));
            if(schema.minLength() > 0) ext.put("minLength", mapper.valueToTree(schema.maxLength()));
            notEmpty(schema.pattern()).ifPresent(s-> ext.put("pattern", mapper.valueToTree(s)));
            notEmpty(schema.format()).ifPresent(s-> ext.put("format", mapper.valueToTree(s)));
            if(type instanceof Type) {
                notEmpty(schema.maximum()).map(parserFor((Type) type)).ifPresent(n ->  ext.put(
                        schema.exclusiveMaximum() ? "exclusiveMaximum" : "maximum",
                        mapper.valueToTree(n)));
                notEmpty(schema.minimum()).map(parserFor((Type) type)).ifPresent(n ->  ext.put(
                        schema.exclusiveMinimum() ? "exclusiveMinimum" : "minimum",
                        mapper.valueToTree(n)));
            }
        }
        return type;
    }

    private Function<String, Number> parserFor(Type type) {
        switch(type.getCore()) {
            case FLOAT:
            case DOUBLE:
            case BIG_DECIMAL:
                return Double::parseDouble;
            default:
                return Long::parseLong;
        }
    }

    private Optional<String> notEmpty(String s){
        return ofNullable(s).filter(f->!f.trim().isEmpty());
    }

    @SuppressWarnings("unchecked")
    private BaseType resolveDataType(Field field) {
        if (DATA_TYPES.containsKey(field.getType())) {
            return addSwaggerFields(field,
                    Type.builder()
                    .name(field.getName())
                    .required( (field.getType().isPrimitive() || hasAnyOf(field, NotNull.class, NonNull.class)) ? Boolean.TRUE : null)
                    .core(DATA_TYPES.get(field.getType()))
                    .build()
            );
        } else if (Collection.class.isAssignableFrom(field.getType())){
            java.lang.reflect.Type nested = nestedType(field);
            return addSwaggerFields(field, net.kebernet.xddl.model.List.builder()
                    .name(field.getName())
                    .required(hasAnyOf(field, NotNull.class, NonNull.class))
                    .type(DATA_TYPES.containsKey(nested) ?
                            Type.builder().core(DATA_TYPES.get(nested)).build() :
                            Reference.builder().ref(nested.getTypeName()).build())
                    .build());
        }

        return null;
    }


}
