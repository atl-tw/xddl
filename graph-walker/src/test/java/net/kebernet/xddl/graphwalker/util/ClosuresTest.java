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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class ClosuresTest {

  @Test(expected = RuntimeException.class)
  public void functionToRuntimeException() {
    assertThat(Closures.functionToRuntimeException((o) -> "foo" + o).apply("bar"))
        .isEqualTo("foobar");
    Closures.functionToRuntimeException(
            o -> {
              throw new IllegalStateException("Whatever");
            })
        .apply(null);
  }

  @Test(expected = RuntimeException.class)
  public void consumerToRuntimeException() {
    Closures.consumerToRuntimeException(
            (o) -> {
              throw new IllegalStateException("whatever");
            })
        .accept(null);
  }
}
