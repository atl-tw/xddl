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
package net.kebernet.xddl.model;

import static com.google.common.base.Charsets.UTF_8;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class Utils {
  private Utils() {}

  @Nonnull
  public static Integer orZero(Integer i) {
    return i == null ? Integer.valueOf(0) : i;
  }

  @Nonnull
  public static File[] neverNull(@Nullable File[] files) {
    return files == null ? new File[0] : files;
  }

  @Nonnull
  public static String neverNull(@Nullable String value) {
    return value == null ? "" : value;
  }

  @Nonnull
  public static <T> Collection<T> neverNull(@Nullable Collection<T> value) {
    if (value == null) {
      return Collections.emptyList();
    } else {
      return value;
    }
  }

  @Nonnull
  public static <T> Iterable<T> neverNull(@Nullable Iterable<T> value) {
    if (value == null) {
      return Collections.emptyList();
    } else {
      return value;
    }
  }

  public static <T> void ifNotNullOrEmpty(
      @Nullable Collection<T> value, @Nonnull Consumer<Collection<T>> consumer) {
    if (!neverNull(value).isEmpty()) {
      consumer.accept(value);
    }
  }

  public static void ifNotNullOrEmpty(@Nullable String s, @Nonnull Consumer<String> consumer) {
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

  public static boolean isNullOrEmpty(@Nullable Collection<?> c) {
    return c == null || c.isEmpty();
  }

  public static boolean isNullOrEmpty(@Nullable Map<?, ?> c) {
    return c == null || c.isEmpty();
  }

  public static <T> Iterable<T> asIterable(@Nonnull Iterator<T> iterator) {
    return () -> iterator;
  }

  public static boolean unboxOrFalse(@Nullable Boolean b) {
    return b == null ? false : b;
  }

  @Nonnull
  public static <T> Stream<T> streamOrEmpty(@Nullable T[] array) {
    if (array == null) {
      return Stream.empty();
    }
    return Arrays.stream(array);
  }

  @SuppressFBWarnings("ERRMSG")
  public static String stackTraceAsString(Throwable t) {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(baos, UTF_8))) {
      t.printStackTrace(pw);
      pw.flush();
      return new String(baos.toByteArray(), UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
