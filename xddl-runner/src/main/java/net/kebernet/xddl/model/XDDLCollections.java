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

import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

public abstract class XDDLCollections {
  private XDDLCollections() {}

  public static <T> Collection<T> neverNull(Collection<T> value) {
    if (value == null || value.isEmpty()) {
      return Collections.emptyList();
    } else {
      return value;
    }
  }

  public static <T> void ifNotNullOrEmpty(Collection<T> value, Consumer<Collection<T>> consumer) {
    if (!neverNull(value).isEmpty()) {
      consumer.accept(value);
    }
  }
}
