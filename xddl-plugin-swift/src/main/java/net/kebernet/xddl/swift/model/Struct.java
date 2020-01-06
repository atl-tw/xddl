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

import com.fasterxml.jackson.databind.node.NullNode;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import net.kebernet.xddl.swift.PropertyExtension;
import net.kebernet.xddl.util.LinesBuilder;

@Data
public class Struct implements SwiftType {
  private String name;
  private List<Field> fieldList = new ArrayList<>();
  private CodingKeys codingKeys;
  private List<SwiftType> nestedTypes = new ArrayList<>();

  public CodingKeys codingKeys() {
    return codingKeys == null ? (this.codingKeys = new CodingKeys()) : this.codingKeys;
  }

  @Override
  public String toString() {
    LinesBuilder lb = new LinesBuilder();

    toSwift(true, lb);
    return lb.toString();
  }

  public void toSwift(boolean addImports, LinesBuilder lb) {
    if (addImports) {
      lb.append("import Foundation").blank();
    }

    lb.append("public struct " + name + ": Codable {").indent();
    fieldList.forEach(lb::append);
    if (!codingKeys().isEmpty()) {
      lb.blank().append("private enum CodingKeys: String, CodingKey {").indent();
      codingKeys()
          .forEach(
              (key, value) ->
                  lb.append(
                      "case " + key + (value instanceof NullNode ? "" : " = " + value.toString())));
      lb.outdent();
      lb.blank().append("}");
    }

    nestedTypes.forEach(
        t -> {
          lb.blank().indent();
          t.toSwift(false, lb);
          lb.outdent().blank();
        });
    lb.outdent().append("}");
  }

  public void addNestedType(SwiftType nestedType) {
    this.nestedTypes.add(nestedType);
  }

  @Data
  public static class Field {
    public final String name;
    public final String type;
    public final boolean optional;
    public final boolean list;
    private final PropertyExtension extension;

    public Field(
        String name, String type, Boolean optional, boolean list, PropertyExtension extension) {
      this.name = name;
      this.type = type;
      this.optional = Boolean.TRUE.equals(optional);
      this.list = list;
      this.extension = extension == null ? new PropertyExtension() : extension;
    }

    @Override
    public String toString() {
      return "var "
          + extension.fieldNameOrElse(name)
          + ": "
          + maybeList(extension.typeOrElse(type))
          + (optional ? "" : "?");
    }

    private String maybeList(String type) {
      if (list) {
        return "[" + type + "]";
      } else {
        return type;
      }
    }
  }
}
