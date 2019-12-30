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
package net.kebernet.xddl.graphwalker.defaults;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public class RecurseIterablesWrapper implements Function<Object, Map<String, ?>> {

  private final Function<Object, Map<String, ?>> wrapped;

  private RecurseIterablesWrapper(Function<Object, Map<String, ?>> wrapped) {
    this.wrapped = wrapped;
  }

  public static Function<Object, Map<String, ?>> wrap(Function<Object, Map<String, ?>> wrapped) {
    return new RecurseIterablesWrapper(wrapped);
  }

  @Override
  public Map<String, ?> apply(Object o) {
    Map<String, ?> fromWrapped = wrapped.apply(o);
    if (fromWrapped.values().isEmpty()
        || fromWrapped.values().stream().noneMatch(v -> v instanceof Iterable)) {
      return fromWrapped;
    }
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    fromWrapped.forEach(
        (key, value) -> {
          if (value instanceof Iterable) {
            int index = 0;
            for (Object val : (Iterable) value) {
              result.put(key + "[" + index + "]", val);
              index++;
            }
          } else {
            result.put(key, value);
          }
        });
    return result;
  }
}
