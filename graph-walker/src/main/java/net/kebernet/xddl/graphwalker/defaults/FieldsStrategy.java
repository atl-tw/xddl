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

import static net.kebernet.xddl.graphwalker.util.Closures.consumerToRuntimeException;
import static net.kebernet.xddl.graphwalker.util.Reflection.findFieldsUpTo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * This is a children strategy function that looks at fields on an object It takes a filter than can
 * be used to filter the fields returned.
 */
public class FieldsStrategy implements Function<Object, Map<String, ?>> {

  private final Predicate<Field> filter;

  private FieldsStrategy(Predicate<Field> filter) {
    this.filter = filter;
  }

  /**
   * Creates a default strategy with common filter, excluding JaCoCo data, static fields, and fields
   * on java* classes.
   *
   * @return A created strategy
   */
  public static Function<Object, Map<String, ?>> create() {
    return create(
        f ->
            !f.getName().equals("$jacocoData")
                && (f.getModifiers() & Modifier.STATIC) == 0
                && !f.getDeclaringClass().getCanonicalName().startsWith("java"));
  }

  @SuppressWarnings("WeakerAccess")
  public static Function<Object, Map<String, ?>> create(Predicate<Field> filter) {
    return new FieldsStrategy(filter);
  }

  @Override
  public Map<String, Object> apply(Object o) {
    if (o == null) return Collections.emptyMap();
    LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<>();
    findFieldsUpTo(o.getClass(), Object.class).stream()
        .filter(filter)
        .forEach(
            consumerToRuntimeException(
                f ->
                    linkedHashMap.put(
                        f.getDeclaringClass().getCanonicalName() + "#" + f.getName(), f.get(o))));
    return linkedHashMap;
  }
}
