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
package net.kebernet.xddl.plugins;

import static java.util.Optional.ofNullable;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class GeneralException extends RuntimeException {
  public GeneralException(String message) {
    super(message);
  }

  public GeneralException(String message, Throwable cause) {
    super(message, cause);
  }

  public static <T> T wrap(Callable<T> callable) {
    try {
      return callable.call();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new GeneralException("Unexpected exception", e);
    }
  }

  public static void wrap(ThrowingRunnable callable) {
    try {
      callable.run();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new GeneralException("Unexpected exception", e);
    }
  }

  public static <T> Consumer<T> wrap(ThrowingConsumer<T> callable) {
    return t -> {
      try {
        callable.run(t);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new GeneralException("Unexpected exception", e);
      }
    };
  }

  public static <T> T maybeOrThrow(T value, String message) {
    return ofNullable(value).orElseThrow(() -> new GeneralException(message));
  }

  @FunctionalInterface
  public interface ThrowingRunnable {
    void run() throws Exception;
  }

  @FunctionalInterface
  public interface ThrowingConsumer<T> {
    void run(T value) throws Exception;
  }
}
