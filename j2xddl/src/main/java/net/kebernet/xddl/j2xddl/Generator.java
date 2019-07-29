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

import static java.lang.Boolean.TRUE;
import static java.util.Optional.ofNullable;
import static net.kebernet.xddl.j2xddl.Reflection.hasAnyOf;
import static net.kebernet.xddl.j2xddl.Reflection.listFields;
import static net.kebernet.xddl.j2xddl.Reflection.nestedType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.CoreType;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.model.Value;

class Generator {

  private static final HashSet<Object> INCLUDE_TYPE = new HashSet<>();

  static {
    INCLUDE_TYPE.add(CoreType.DATE);
    INCLUDE_TYPE.add(CoreType.DATETIME);
    INCLUDE_TYPE.add(CoreType.TIME);
    INCLUDE_TYPE.add(short.class);
    INCLUDE_TYPE.add(Short.class);
  }

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

  Generator(ObjectMapper mapper, HashSet<Class> classes) {
    this.mapper = mapper;
    this.classes = classes;
  }

  Specification generate() {
    Specification specification = new Specification();
    classes.forEach(c -> doClass(specification, c));
    return specification;
  }

  private void doClass(Specification specification, Class clazz) {
    if (clazz.isEnum()) {
      specification.types().add(buildEnum(clazz));
    } else {
      specification
          .structures()
          .add(
              addSwaggerFields(
                  clazz,
                  Structure.builder()
                      .name(clazz.getSimpleName())
                      .properties(
                          listFields(clazz).stream()
                              .map(this::resolveDataType)
                              .collect(Collectors.toList()))
                      .build()));
    }
  }

  private <T extends BaseType> T addSwaggerFields(AnnotatedElement element, T type) {
    Schema schema = element.getAnnotation(Schema.class);
    if (schema != null) {
      type.setRequired(TRUE.equals(type.getRequired()) || schema.required());
      notEmpty(schema.description()).ifPresent(type::setDescription);
      Map<String, JsonNode> ext = new HashMap<>();
      if (schema.maxLength() != Integer.MAX_VALUE)
        ext.put("maxLength", mapper.valueToTree(schema.maxLength()));
      if (schema.minLength() > 0) ext.put("minLength", mapper.valueToTree(schema.maxLength()));
      notEmpty(schema.pattern()).ifPresent(s -> ext.put("pattern", mapper.valueToTree(s)));
      notEmpty(schema.format()).ifPresent(s -> ext.put("format", mapper.valueToTree(s)));
      if (type instanceof Type) {
        notEmpty(schema.maximum())
            .map(parserFor((Type) type))
            .ifPresent(
                n ->
                    ext.put(
                        schema.exclusiveMaximum() ? "exclusiveMaximum" : "maximum",
                        mapper.valueToTree(n)));
        notEmpty(schema.minimum())
            .map(parserFor((Type) type))
            .ifPresent(
                n ->
                    ext.put(
                        schema.exclusiveMinimum() ? "exclusiveMinimum" : "minimum",
                        mapper.valueToTree(n)));
      }
      if (!ext.isEmpty()) {
        //noinspection unchecked
        type.ext().put("json", mapper.valueToTree(ext));
      }
    }
    return type;
  }

  private Function<String, Number> parserFor(Type type) {
    switch (type.getCore()) {
      case FLOAT:
      case DOUBLE:
      case BIG_DECIMAL:
        return Double::parseDouble;
      default:
        return Long::parseLong;
    }
  }

  private Optional<String> notEmpty(String s) {
    return ofNullable(s).filter(f -> !f.trim().isEmpty());
  }

  @SuppressWarnings("unchecked")
  private BaseType resolveDataType(Field field) {
    Class<?> fieldType = field.getType();
    HashMap<String, JsonNode> ext = new HashMap<>();
    if (DATA_TYPES.containsKey(field.getType())) {
      return buildType(field, ext);
    } else if (Collection.class.isAssignableFrom(field.getType())) {
      return buildList(field, fieldType, ext);
    }
    return buildReference(field);
  }

  /**
   * TODO: We need to make sure we can support enums of non-string types at some point.
   *
   * @param
   * @return
   */
  private Type buildEnum(Class type) {
    return addSwaggerFields(
        type,
        Type.builder()
            .core(CoreType.STRING)
            .name(type.getSimpleName())
            .allowable(
                Arrays.stream(type.getEnumConstants())
                    .map(String::valueOf)
                    .map(s -> new Value(mapper.valueToTree(s), null, null))
                    .collect(Collectors.toList()))
            .build());
  }

  private BaseType buildReference(Field field) {
    Reference reference = new Reference();
    reference.setName(field.getName());
    reference.setRef(field.getType().getSimpleName());
    return reference;
  }

  private BaseType buildList(Field field, Class<?> fieldType, HashMap<String, JsonNode> ext) {
    Class nested = (Class) nestedType(field);
    if (!List.class.equals(fieldType)) {
      ext.put("type", mapper.valueToTree(field.getType().getCanonicalName()));
    }
    HashMap<String, JsonNode> childExt = new HashMap<>();
    if (requiresJavaType(nested)) {
      childExt.put("type", mapper.valueToTree(nested.getCanonicalName()));
    }
    return addSwaggerFields(
        field,
        net.kebernet.xddl.model.List.builder()
            .name(field.getName())
            .required(hasAnyOf(field, NotNull.class, NonNull.class) ? TRUE : null)
            .contains(
                DATA_TYPES.containsKey(nested)
                    ? Type.builder()
                        .core(DATA_TYPES.get(nested))
                        .build()
                        .putExt("java", mapper.valueToTree(childExt))
                    : Reference.builder().ref(nested.getSimpleName()).build())
            .build());
  }

  @SuppressWarnings("SuspiciousMethodCalls")
  private boolean requiresJavaType(java.lang.reflect.Type type) {
    return INCLUDE_TYPE.contains(type) || INCLUDE_TYPE.contains(DATA_TYPES.get(type));
  }

  private BaseType buildType(Field field, HashMap<String, JsonNode> ext) {
    if (requiresJavaType(field.getType())) {
      ext.put("type", mapper.valueToTree(field.getType().getCanonicalName()));
    }
    //noinspection unchecked
    return addSwaggerFields(
        field,
        Type.builder()
            .name(field.getName())
            .required(
                (field.getType().isPrimitive() || hasAnyOf(field, NotNull.class, NonNull.class))
                    ? TRUE
                    : null)
            .core(DATA_TYPES.get(field.getType()))
            .build()
            .putExt("java", mapper.valueToTree(ext)));
  }
}
