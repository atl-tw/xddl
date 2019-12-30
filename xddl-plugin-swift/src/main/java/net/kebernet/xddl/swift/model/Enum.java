/*
 * Copyright 2019, 2020 Robert Cooper, ThoughtWorks
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
package net.kebernet.xddl.swift.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import net.kebernet.xddl.migrate.format.CaseFormat;
import net.kebernet.xddl.model.Value;

@Data
@Builder
public class Enum implements SwiftType {
  private String name;
  private String valueType;
  @Builder.Default private List<NameValue> nameValues = new ArrayList<>();

  @VisibleForTesting
  static String makeSwiftNameFriendly(String value) {
    value = value.replaceAll("[-\\+ .]", "_");
    value = CaseFormat.LOWER_SNAKE.to(CaseFormat.LOWER_CAMEL).apply(value);
    return Character.isAlphabetic(value.charAt(0)) ? value : "val_" + value;
  }

  public static List<NameValue> toNameValues(List<Value> allowable) {
    return allowable.stream()
        .map(
            value -> {
              String asText = value.getValue().asText();
              String name = makeSwiftNameFriendly(asText);
              if (Objects.equals(name, asText)) {
                return new NameValue(name, null);
              } else {
                return new NameValue(name, value.getValue());
              }
            })
        .collect(Collectors.toList());
  }

  public static String determineEnumType(List<Value> allowable) {
    if (allowable.stream().allMatch(v -> v.getValue().isDouble())) {
      return "Double";
    }
    if (allowable.stream().allMatch(v -> v.getValue().isInt())) {
      return "Integer";
    }
    if (allowable.stream().allMatch(v -> v.getValue().isTextual())) {
      return "String";
    }
    throw new IllegalArgumentException(
        "Can't determine a type from the list of available values " + allowable);
  }

  @Override
  public String toString() {
    LinesBuilder lb = new LinesBuilder();
    toSwift(true, lb);
    return lb.toString();
  }

  @Override
  public void toSwift(boolean addImports, LinesBuilder lb) {

    if (addImports) {
      lb.append("import Foundation").blank();
    }
    lb.append("public enum $L: $L, Codable {", name, valueType).indent();
    nameValues.forEach(
        nv -> {
          if (nv.value == null) {
            lb.append("case $L", nv.name);
          } else {
            lb.append("case $L = $L", nv.name, nv.value.toString());
          }
        });
    lb.outdent();
    lb.append("}");
  }

  @Data
  public static class NameValue {
    private final String name;
    private final JsonNode value;

    public NameValue(String name, JsonNode value) {
      this.name = name;
      this.value = value;
    }
  }
}
