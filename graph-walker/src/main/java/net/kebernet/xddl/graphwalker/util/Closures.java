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
package net.kebernet.xddl.graphwalker.util;

import java.util.function.Consumer;
import java.util.function.Function;

public abstract class Closures {
  private Closures() {}

  public static <I, O> Function<I, O> functionToRuntimeException(ThrowingFunction<I, O> function) {
    return (i) -> {
      try {
        return function.apply(i);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }

  public static <I> Consumer<I> consumerToRuntimeException(ThrowingConsumer<I> function) {
    return (i) -> {
      try {
        function.accept(i);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  };

  public interface ThrowingConsumer<I> {
    void accept(I input) throws Exception;
  }

  public interface ThrowingFunction<I, O> {
    O apply(I input) throws Exception;
  }
}
