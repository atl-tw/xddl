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
package net.kebernet.xddl.migrate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "@type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = JsonPathStage.class, name = "jsonp"),
  @JsonSubTypes.Type(value = MapStage.class, name = "map"),
  @JsonSubTypes.Type(value = RegexStage.class, name = "regex"),
  @JsonSubTypes.Type(value = LiteralStage.class, name = "literal"),
  @JsonSubTypes.Type(value = RenameStage.class, name = "rename"),
  @JsonSubTypes.Type(value = CaseStage.class, name = "case"),
  @JsonSubTypes.Type(value = TemplateStage.class, name = "template"),
  @JsonSubTypes.Type(value = JavaStage.class, name = "java")
})
public class Stage {
  private int index;
}
