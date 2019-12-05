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
package net.kebernet.xddl.java;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JavaExtension {
  List<String> imports;
  String annotations;
  String getterAnnotations;

  @JsonProperty("package")
  String packageName;

  String type;

  @JsonProperty("implements")
  List<String> implementsList;

  @JsonProperty("extends")
  String extendsClass;

  String initializer;
  List<String> compareToIncludeProperties;
  String equalsHashCodeWrapper;

  public static JavaExtension parse(ObjectMapper mapper, JsonNode node) {
    if (node == null) {
      return null;
    } else {
      try {
        return mapper.treeToValue(node, JavaExtension.class);
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException("Couldn't parse " + node.toPrettyString());
      }
    }
  }
}
