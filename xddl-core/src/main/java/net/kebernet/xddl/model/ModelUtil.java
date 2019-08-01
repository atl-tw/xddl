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
package net.kebernet.xddl.model;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

@SuppressWarnings("WeakerAccess")
public abstract class ModelUtil {

  public static <T> void maybeSet(Consumer<T> consumer, T value, T defaultValue) {
    consumer.accept(ofNullable(value).orElse(defaultValue));
  }

  public static <T> void maybeSet(
      Consumer<T> consumer,
      JsonNode node,
      String name,
      Function<JsonNode, T> reader,
      T defaultValue) {
    if (node.has(name)) {
      consumer.accept(reader.apply(node.get(name)));
    } else {
      consumer.accept(defaultValue);
    }
  }

  public static <T> void maybeSet(
      Consumer<T> consumer, JsonNode node, String name, Function<JsonNode, T> reader) {
    if (node.has(name)) {
      consumer.accept(reader.apply(node.get(name)));
    }
  }

  @SuppressWarnings("unchecked")
  public static <T extends BaseType> T merge(T newValue, T originalValue, Reference reference) {
    maybeSet(newValue::setName, reference.getName(), originalValue.getName());
    maybeSet(newValue::setDescription, reference.getDescription(), originalValue.getDescription());
    newValue.setRequired(originalValue.getRequired());
    Map<String, JsonNode> ext = reference.ext();
    ofNullable(originalValue.getExt()).ifPresent(ext::putAll);
    newValue.setExt(ext);
    return newValue;
  }

  public static <T extends BaseType> void isaType(T type, Consumer<Type> consumer) {
    if (type instanceof Type) {
      consumer.accept(((Type) type));
    }
  }

  public static <T extends BaseType> void isaList(T type, Consumer<List> consumer) {
    if (type instanceof List) {
      consumer.accept(((List) type));
    }
  }

  public static <T extends BaseType> void isaReference(T type, Consumer<Reference> consumer) {
    if (type instanceof Reference) {
      consumer.accept(((Reference) type));
    }
  }

  public static <T extends BaseType> void isaStructure(T type, Consumer<Structure> consumer) {
    if (type instanceof Structure) {
      consumer.accept(((Structure) type));
    }
  }
}
