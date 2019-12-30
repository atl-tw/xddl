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
package net.kebernet.xddl.ognl;

import com.google.common.collect.Sets;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class BeanWalker {

  private final Set<Object> visited = Sets.newIdentityHashSet();
  private final LinkedList<Object> queue = new LinkedList<>();
  private final Predicate<PropertyDescriptor> propertyFilter;

  public BeanWalker(Object rootBean, Predicate<PropertyDescriptor> propertyFilter) {
    queue.add(rootBean);
    this.propertyFilter = propertyFilter;
  }

  public void apply(final PropertyVisitor visitor) {
    try {
      while (!queue.isEmpty()) {
        final Object current = queue.pollFirst();
        if (current == null) break;
        visited.add(current);
        BeanInfo bd = Introspector.getBeanInfo(current.getClass());
        Arrays.stream(bd.getPropertyDescriptors())
            .filter(propertyFilter)
            .forEach(
                pd ->
                    visitor
                        .visit(pd, current)
                        .ifPresent(
                            result -> {
                              result.removeAll(visited);
                              queue.addAll(result);
                            }));
      }

    } catch (IntrospectionException e) {
      throw new IllegalStateException(e);
    }
  }

  public interface PropertyVisitor {
    Optional<Set<Object>> visit(PropertyDescriptor property, Object target);
  }
}
