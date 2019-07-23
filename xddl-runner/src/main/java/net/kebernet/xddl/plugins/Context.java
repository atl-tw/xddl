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
    specification.getTypes().forEach(this::checkAndInsert);
    specification.getStructures().forEach(this::checkAndInsert);
    specification.getStructures().forEach(this::validateReferences);
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

  public IllegalStateException stateException(String message, Object offending) {
    try {
      return new IllegalStateException(
          message + "\nOffending expression:\n" + mapper.writeValueAsString(offending));
    } catch (JsonProcessingException e) {
      return new IllegalStateException(message, e);
    }
  }

  public boolean isStructure(Reference reference) {
    return resolveReference(reference) != null && resolveReference(reference) instanceof Structure;
  }

  public BaseType resolveReference(Reference reference) {
    BaseType type = references.get(reference.getRef());
    return type.merge(reference);
  }

  public boolean isType(Reference reference) {
    return resolveReference(reference) != null && resolveReference(reference) instanceof Type;
  }

  public <T extends BaseType> void hasPlugin(
      String extType, T type, Consumer<JsonNode> ifTrue, Consumer<T> ifFalse) {
    Optional<JsonNode> value = ofNullable((JsonNode) type.ext().get(extType));
    if (value.isPresent()) {
      ifTrue.accept(value.get());
    } else {
      ifFalse.accept(type);
    }
  }
}
