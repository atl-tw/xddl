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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public abstract class Utils {
  private Utils() {}

  public static Integer neverNull(Integer i) {
    return i == null ? Integer.valueOf(0) : i;
  }

  public static File[] neverNull(File[] files) {
    return files == null ? new File[0] : files;
  }

  public static String neverNull(String value) {
    return value == null ? "" : value;
  }

  public static <T> Collection<T> neverNull(Collection<T> value) {
    if (value == null) {
      return Collections.emptyList();
    } else {
      return value;
    }
  }

  public static <T> Iterable<T> neverNull(Iterable<T> value) {
    if (value == null) {
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

  public static void ifNotNullOrEmpty(String s, Consumer<String> consumer) {
    if (!Strings.isNullOrEmpty(s)) {
      consumer.accept(s);
    }
  }

  public static void ifNotNullOrEmpty(
      Map<String, JsonNode> node, Consumer<Map<String, JsonNode>> consumer) {
    if (node != null && !node.isEmpty()) {
      consumer.accept(node);
    }
  }

  public static boolean isNullOrEmpty(Collection c) {
    return c == null || c.isEmpty();
  }

  public static <T> Iterable<T> asIterable(Iterator<T> iterator) {
    return new Iterable<T>() {
      @Override
      public Iterator<T> iterator() {
        return iterator;
      }
    };
  }

  public static boolean unboxOrFalse(Boolean b) {
    return b == null ? false : b;
  }
}
