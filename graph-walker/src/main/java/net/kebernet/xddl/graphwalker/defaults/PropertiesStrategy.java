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
package net.kebernet.xddl.graphwalker.defaults;

import static net.kebernet.xddl.graphwalker.util.Closures.consumerToRuntimeException;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class PropertiesStrategy implements Function<Object, Map<String, ?>> {
  private static final ConcurrentHashMap<Class, PropertyDescriptor[]> DESCRIPTORS =
      new ConcurrentHashMap<>();
  private final Predicate<PropertyDescriptor> filter;

  private PropertiesStrategy(Predicate<PropertyDescriptor> filter) {
    this.filter = filter;
  }

  public static Function<Object, Map<String, ?>> create() {
    return create(
        d -> {
          if (!"class".equals(d.getName())
              && d.getReadMethod() != null
              && (d.getReadMethod().getModifiers() & Modifier.STATIC) == 0) {
            d.getReadMethod().setAccessible(true);
            return true;
          }
          return false;
        });
  }

  public static Function<Object, Map<String, ?>> create(Predicate<PropertyDescriptor> filter) {
    return new PropertiesStrategy(filter);
  }

  @Override
  public Map<String, ?> apply(Object o) {
    LinkedHashMap<String, Object> result = new LinkedHashMap<>();
    Arrays.stream(findDescriptor(o.getClass()))
        .filter(filter)
        .forEach(
            consumerToRuntimeException(
                d ->
                    result.put(
                        o.getClass().getCanonicalName() + "." + d.getName(),
                        d.getReadMethod().invoke(o))));
    return result;
  }

  private PropertyDescriptor[] findDescriptor(Class clazz) {
    if (DESCRIPTORS.containsKey(clazz)) {
      return DESCRIPTORS.get(clazz);
    }
    try {
      PropertyDescriptor[] descriptors = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
      DESCRIPTORS.put(clazz, descriptors);
      return descriptors;
    } catch (IntrospectionException e) {
      throw new IllegalArgumentException("Unable to introspect " + clazz.getCanonicalName(), e);
    }
  }
}
