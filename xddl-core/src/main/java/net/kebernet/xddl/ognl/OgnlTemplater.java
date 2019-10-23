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
package net.kebernet.xddl.ognl;

import static net.kebernet.xddl.model.ModelUtil.forEach;
import static net.kebernet.xddl.model.ModelUtil.neverNegative;
import static net.kebernet.xddl.model.Utils.neverNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.Specification;
import ognl.Ognl;

public class OgnlTemplater implements BeanWalker.PropertyVisitor {
  private static final Pattern PATTERN = Pattern.compile("[^\\\\]?(\\$\\{(\\\\}|[^}])*})");
  private static final Predicate<PropertyDescriptor> predicate = pd -> // is read/write
      pd.getReadMethod() != null
              // is collection or string.
              && ((Collection.class.isAssignableFrom(pd.getPropertyType())
                      || Map.class.isAssignableFrom(pd.getPropertyType()))
                  || BaseType.class.isAssignableFrom(pd.getPropertyType())
                  || (pd.getPropertyType() == String.class && pd.getWriteMethod() != null));
  private final Specification specification;
  private final Map<String, Object> context = new HashMap<>();

  public OgnlTemplater(Specification specification, Map<String, Object> vals) {
    this.specification = specification;
    context.put("specification", specification);
    context.put("vals", vals);
  }

  public void run() {
    BeanWalker beanWalker = new BeanWalker(specification, predicate);
    beanWalker.apply(this);
  }

  @Override
  public Optional<Set<Object>> visit(PropertyDescriptor property, Object target) {
    if (property.getName().equals("contains")) {
      System.out.println("");
    }
    try {
      property.getReadMethod().setAccessible(true);
      if (Collection.class.isAssignableFrom(property.getPropertyType())) {
        return Optional.ofNullable((Collection) property.getReadMethod().invoke(target))
            .map(this::filterJsonNodes)
            .map(HashSet::new);
      }
      if (Map.class.isAssignableFrom(property.getPropertyType())) {
        return Optional.ofNullable((Map) property.getReadMethod().invoke(target))
            .map(Map::values)
            .map(this::filterJsonNodes)
            .map(HashSet::new);
      }
      property.getWriteMethod().setAccessible(true);
      Object value = property.getReadMethod().invoke(target);
      if (value instanceof String) {
        Optional.of((String) value)
            .map(this::fillTemplate)
            .ifPresent(
                s -> {
                  try {
                    property.getWriteMethod().invoke(target, s);
                  } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException(e);
                  }
                });
        return Optional.empty();
      } else if (value != null) {
        new BeanWalker(value, predicate).apply(this);
        return Optional.of(Collections.singleton(value));
      } else {
        return Optional.empty();
      }
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private Collection filterJsonNodes(Collection collection) {
    return (Collection)
        collection.stream()
            .filter(
                o -> {
                  if (o instanceof JsonNode) {
                    doNode((JsonNode) o);
                    return false;
                  }
                  return true;
                })
            .collect(Collectors.toList());
  }

  private void doNode(JsonNode o) {
    Map<String, JsonNode> updates = new HashMap<>();
    forEach(
        o.fields(),
        entry -> {
          if (entry.getValue().isTextual() && entry.getValue().asText().contains("${")) {
            String newValue = fillTemplate(entry.getValue().asText());
            updates.put(entry.getKey(), Loader.mapper().valueToTree(newValue));
          } else if (entry.getValue().isObject()) {
            doNode(entry.getValue());
          } else if (entry.getValue().isArray()) {
            ArrayNode array = (ArrayNode) entry.getValue();
            for (int i = 0; i < array.size(); i++) {
              if (array.get(i).isObject()) {
                doNode(array.get(i));
              } else if (array.get(i).isTextual()) {
                array.set(i, Loader.mapper().valueToTree(fillTemplate(array.get(i).asText())));
              }
            }
          }
          if (!updates.isEmpty()) {
            ObjectNode objectNode = (ObjectNode) o;
            updates.forEach(objectNode::replace);
          }
        });
  }

  /**
   * This method fills a template containing OGNL expressions wrapped inside ${} blocks. The root
   * context of the OGNL evaluation will contain "specification" with the current specification
   * object fully populated, and you can pass additional objects in the map
   *
   * @param template The template to read from.
   * @return String value of the resulting applied template.
   */
  public String fillTemplate(String template) {
    template = neverNull(template);
    StringBuilder result = new StringBuilder();

    Matcher matcher = PATTERN.matcher(template);
    int nextText = 0;
    while (!template.isEmpty() && matcher.find()) {
      int start = matcher.start();
      char startChar = template.charAt(start);
      if (startChar != '$') {
        result.append(template, neverNegative(start - 1), start + 1);
        start++;
      }
      String expression = template.substring(start, matcher.end() - 1).replaceFirst("\\$\\{", "");
      try {
        Object exp = Ognl.parseExpression(expression);
        Object evaluated = Ognl.getValue(exp, context);
        result.append(evaluated);
      } catch (Exception e) {
        e.printStackTrace();
        throw new IllegalStateException("OGNL Failed [" + expression + "] in:\n" + template);
      }
      nextText = matcher.end();
    }
    if (nextText < template.length()) {
      result.append(template.substring(neverNegative(nextText)));
    }
    return result.toString();
  }
}
