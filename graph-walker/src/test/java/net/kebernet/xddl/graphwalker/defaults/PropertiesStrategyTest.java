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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Stopwatch;
import java.time.Duration;
import java.util.LinkedHashMap;
import net.kebernet.xddl.graphwalker.Child;
import org.junit.Test;

public class PropertiesStrategyTest {

  @Test
  public void simpleTest() {
    Child target = new Child();
    LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
    expected.put(Child.class.getCanonicalName() + ".bar", "bar");
    expected.put(Child.class.getCanonicalName() + ".cycleRef", null);
    expected.put(Child.class.getCanonicalName() + ".foo", "childFoo");
    expected.put(Child.class.getCanonicalName() + ".baz", "baz");

    Stopwatch stopwatch = Stopwatch.createStarted();
    assertThat(PropertiesStrategy.create().apply(target)).isEqualTo(expected);
    Duration first = stopwatch.elapsed();
    System.out.println(first.toNanos());
    stopwatch.reset();
    stopwatch.start();
    assertThat(PropertiesStrategy.create().apply(target)).isEqualTo(expected);
    Duration second = stopwatch.elapsed();
    assertThat(first).isGreaterThan(second);
    System.out.println(second.toNanos());
  }
}
