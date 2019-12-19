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
package net.kebernet.xddl.diff;

import static net.kebernet.xddl.model.Utils.isNullOrEmpty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.kebernet.xddl.model.*;

@SuppressWarnings("WeakerAccess")
public class SchemaElement {
  private static final ObjectMapper mapper = new ObjectMapper();
  private final List<String> path;
  private final Class<? extends BaseType> resolvedType;
  private final CoreType coreType;
  private final Set<JsonNode> allowableValues;

  public SchemaElement(
      List<String> path,
      Class<? extends BaseType> resolvedType,
      CoreType coreType,
      Set<JsonNode> allowableValues) {
    this.path = path;
    this.resolvedType = resolvedType;
    this.coreType = coreType;
    this.allowableValues = allowableValues;
  }

  public SchemaElement(Collection<String> path, BaseType resolved) {
    this(
        new ArrayList<>(path),
        resolved.getClass(),
        resolved instanceof Type ? ((Type) resolved).getCore() : null,
        resolved instanceof Type
            ? Utils.neverNull(((Type) resolved).getAllowable()).stream()
                .map(Value::getValue)
                .collect(Collectors.toSet())
            : null);
  }

  public String pathToDotNotation() {
    return Joiner.on(".").join(path);
  }

  public String typeName() {
    return Type.class.equals(resolvedType)
        ? String.valueOf(coreType)
        : resolvedType.getSimpleName();
  }

  public String allowableValues() {
    return "["
        + Utils.neverNull(allowableValues).stream()
            .map(this::writeValueAsString)
            .collect(Collectors.joining(","))
        + "]";
  }

  private String writeValueAsString(JsonNode node) {
    try {
      return mapper.writeValueAsString(node);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public String toString() {
    return pathToDotNotation()
        + " ("
        + typeName()
        + ")"
        + (!isNullOrEmpty(allowableValues) ? allowableValues() : "");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SchemaElement)) return false;
    SchemaElement that = (SchemaElement) o;
    return Objects.equal(path, that.path)
        && Objects.equal(resolvedType, that.resolvedType)
        && coreType == that.coreType
        && Objects.equal(
            new HashSet<>(Utils.neverNull(allowableValues)),
            new HashSet<>(Utils.neverNull(that.allowableValues)));
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(path, resolvedType, coreType, allowableValues);
  }
}
