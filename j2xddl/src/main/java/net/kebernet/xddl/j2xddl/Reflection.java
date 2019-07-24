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
package net.kebernet.xddl.j2xddl;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class Reflection {

  private Reflection() {}

  public static boolean hasAnyOf(AnnotatedElement element, Class<? extends Annotation>... type) {
    return Arrays.stream(type).anyMatch(element::isAnnotationPresent);
  }

  public static List<Field> listFields(Class clazz) {
    List<Field> result = new ArrayList<>(clazz.getDeclaredFields().length);
    Arrays.stream(clazz.getDeclaredFields())
        .filter(f -> !Modifier.isStatic(f.getModifiers()) && !Modifier.isFinal(f.getModifiers()))
        .filter(f -> f.getAnnotation(JsonIgnore.class) == null)
        .forEach(result::add);
    return result;
  }

  public static Type nestedType(Field f) {
    if (Collection.class.isAssignableFrom(f.getType())) {
      ParameterizedType collectionType = (ParameterizedType) f.getGenericType();
      return collectionType.getActualTypeArguments()[0];
    } else {
      return f.getType();
    }
  }
}
