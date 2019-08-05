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
package net.kebernet.xddl.plugins;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import lombok.Getter;
import lombok.Setter;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;

@Getter
@Setter
public class Context {
  private final ObjectMapper mapper;
  private final Specification specification;
  private Map<String, BaseType> references = new HashMap<>();

  public Context(ObjectMapper mapper, Specification specification) {
    this.mapper = mapper;
    this.specification = specification;
    specification.types().forEach(this::checkAndInsert);
    specification.structures().forEach(this::checkAndInsert);
    specification.structures().forEach(this::validateReferences);
  }

  private void validateReferences(Structure structure) {
    structure
        .getProperties()
        .forEach(
            p -> {
              if (p instanceof Reference) {
                Reference ref = (Reference) p;
                if (!references.containsKey(ref.getRef())) {
                  throw stateException("Unknown reference " + ref.getRef(), ref);
                }
              } else if (p instanceof Structure) {
                validateReferences((Structure) p);
              }
            });
  }

  private void checkAndInsert(BaseType type) {
    if (type instanceof Reference) {
      throw stateException(
          "A reference (" + type.getName() + " cannot be a top level element in a Specification",
          type);
    }
    if (references.containsKey(type.getName())) {
      throw stateException(type.getName() + " is duplicated as a top level name.", type);
    }
    references.put(type.getName(), type);
  }

  /**
   * Constructs a new illegal state exception with a mes
   *
   * @param message A message for the excetion
   * @param offending an offending object that will be serialized into the exception.
   * @return A constructed exception.
   */
  public IllegalStateException stateException(String message, Object offending) {
    try {
      return new IllegalStateException(
          message + "\nOffending expression:\n" + mapper.writeValueAsString(offending));
    } catch (JsonProcessingException e) {
      return new IllegalStateException(message, e);
    }
  }

  /**
   * Resolves a reference to the target contains and merges the metadata from the reference into the
   * new fully resolved contains.
   *
   * @param reference The reference to resolve
   * @param <T> The target contains
   * @return The contains or
   */
  @SuppressWarnings("unchecked")
  public <T extends BaseType<T>> Optional<T> resolveReference(Reference reference) {
    Optional<T> result = ofNullable((T) references.get(reference.getRef()));
    return result.map(t -> t.merge(reference));
  }

  /**
   * Returns true if the given reference resolves to a base contains.
   *
   * @param reference The ref to resulve.
   * @return true if it is a structure.
   */
  public boolean pointsToType(Reference reference) {
    return resolveReference(reference).filter(r -> r instanceof Type).isPresent();
  }

  /**
   * Returns true if the given reference points to a structure.
   *
   * @param reference The reference
   * @return true if it is a structure.
   */
  public boolean pointsToStructure(Reference reference) {
    return resolveReference(reference).filter(r -> r instanceof Structure).isPresent();
  }

  /**
   * Evaluates if a contains has a extension of a given name
   *
   * @param extType The name of the extension contains
   * @param type The contains to check
   * @param ifTrue a consumer called if it has the plugin.
   * @param ifFalse a consumer called if it doesn't have the plugin
   * @param <T> The contains.
   */
  public <T extends BaseType> void hasPlugin(
      String extType, T type, Consumer<JsonNode> ifTrue, Consumer<T> ifFalse) {
    Optional<JsonNode> value = ofNullable((JsonNode) type.ext().get(extType));
    if (value.isPresent()) {
      ifTrue.accept(value.get());
    } else {
      ifFalse.accept(type);
    }
  }

  public Optional<Type> findType(String refName) {
    return ofNullable(references.get(refName)).filter(t -> t instanceof Type).map(t -> (Type) t);
  }
}
