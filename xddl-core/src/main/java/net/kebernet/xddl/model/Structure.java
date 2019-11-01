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
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import net.kebernet.xddl.Loader;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Structure extends BaseType<Structure> {
  private List<BaseType> properties;

  @Override
  public Structure merge(Reference reference) {
    Structure newValue = ModelUtil.merge(new Structure(), this, reference);
    newValue.setProperties(this.properties);
    return newValue;
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
