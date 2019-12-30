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
package net.kebernet.xddl.jsonschema.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Definition {
  private String title;
  private String description;
  private String format;
  private String type;

  @JsonProperty("enum")
  private List<JsonNode> enums;

  private Map<String, Definition> properties;
  private List<String> required;
  private Boolean additionalProperties;
  private Integer minLength;
  private Integer maxLength;
  private String pattern;
  private Number minimum;
  private Number exclusiveMinimum;
  private Number maximum;
  private Number exclusiveMaximum;
  private PropertyNames propertyNames;
  private Integer minProperties;
  private Integer maxProperties;
  private Map<String, List<String>> dependencies;
  private Map<String, Definition> patternProperties;

  @JsonProperty("$ref")
  private String ref;

  private Definition items;

  public Map<String, Definition> properties() {
    if (this.properties == null) {
      this.properties = new HashMap<>();
    }
    return this.properties;
  }

  public List<String> required() {
    if (this.required == null) {
      this.required = new ArrayList<>();
    }
    return required;
  }

  @Data
  public static class PropertyNames {
    private String pattern;
  }
}
