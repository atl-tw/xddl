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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Data;
import net.kebernet.xddl.Loader;

@Data
public class Specification implements HasExtensions {
  private String title;
  private String description;
  private String comment;
  private String version;
  private String entryRef;
  private List<Type> types;
  private List<Structure> structures;
  private List<PatchDelete> deletions;
  private Map<String, JsonNode> ext;
  private Map<String, Object> constants;

  public synchronized Specification setTypes(List<Type> types) {
    this.types = types;
    return this;
  }

  public synchronized Specification setStructures(List<Structure> structures) {
    this.structures = structures;
    return this;
  }

  public synchronized Specification setExt(Map<String, JsonNode> ext) {
    this.ext = ext;
    return this;
  }

  public synchronized Specification setDeletions(List<PatchDelete> deletions) {
    this.deletions = deletions;
    return this;
  }

  public synchronized List<Type> getTypes() {
    return types;
  }

  public synchronized List<Structure> getStructures() {
    return structures;
  }

  public synchronized Map<String, JsonNode> getExt() {
    return ext;
  }

  @SuppressWarnings("unused")
  public synchronized List<PatchDelete> getDeletions() {
    return deletions;
  }

  /**
   * Returns the ext map, creating it if it is null
   *
   * @return a map of strings to json nodes.
   */
  public synchronized Map<String, JsonNode> ext() {
    if (this.ext == null) {
      this.ext = new HashMap<>();
    }
    return this.ext;
  }

  public synchronized List<Structure> structures() {
    if (this.structures == null) {
      this.structures = new ArrayList<>();
    }
    return this.structures;
  }

  public synchronized List<Type> types() {
    if (this.types == null) {
      this.types = new ArrayList<>();
    }
    return this.types;
  }

  public synchronized List<PatchDelete> deletions() {
    if (this.deletions == null) {
      this.deletions = new ArrayList<>();
    }
    return this.deletions;
  }

  @Override
  public String toString() {
    try {
      return Loader.mapper().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Why can't I write myself to string? " + this.getClass(), e);
    }
  }
}
