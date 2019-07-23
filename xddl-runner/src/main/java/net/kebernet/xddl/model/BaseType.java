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

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;

@Data
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = Reference.class, name = "Reference"),
  @JsonSubTypes.Type(value = Structure.class, name = "Structure"),
  @JsonSubTypes.Type(value = Type.class, name = "Type"),
  @JsonSubTypes.Type(value = List.class, name = "List")
})
public abstract class BaseType<T extends BaseType> {
  private String name;
  private String description;
  private Map<String, JsonNode> ext;
  private boolean required;

  public Map<String, JsonNode> ext() {
    if (this.ext == null) {
      this.ext = new HashMap<>();
    }
    return this.ext;
  }

  public abstract T merge(Reference reference);
}
