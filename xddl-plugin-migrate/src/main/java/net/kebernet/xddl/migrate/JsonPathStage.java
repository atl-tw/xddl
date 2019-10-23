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
package net.kebernet.xddl.migrate;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class JsonPathStage extends Stage {
  List<String> steps;
  private Context start = Context.CURRENT;

  @SuppressWarnings("unused")
  public enum Context {
    /** Indicates that this step should be evaluated from the document root. */
    ROOT,
    /** Indicates that this step should be evaluated from the local context. */
    LOCAL,
    /** Indicates that this step should start from the current value */
    CURRENT;
  }
}
