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
package net.kebernet.xddl.util;

import static com.google.common.truth.Truth.assertThat;

import java.util.AbstractMap;
import org.junit.Test;

public class HistogramTest {

  @Test
  public void testIncrementing() {
    Histogram<String> histogram = new Histogram<>();
    histogram.add("Foo").add("Bar").add("Foo").add("Baz");
    assertThat(histogram.entrySet())
        .containsExactly(
            new AbstractMap.SimpleEntry<>("Foo", 2),
            new AbstractMap.SimpleEntry<>("Bar", 1),
            new AbstractMap.SimpleEntry<>("Baz", 1));
  }

  @Test
  public void testAddition() {
    Histogram<String> histogram = new Histogram<>();
    histogram.add("Foo").add("Bar").add("Foo").add("Baz");
    Histogram<String> histogram2 = new Histogram<>();
    histogram.add("Bar").add("Baz");
    histogram.addAll(histogram2);

    assertThat(histogram.entrySet())
        .containsExactly(
            new AbstractMap.SimpleEntry<>("Foo", 2),
            new AbstractMap.SimpleEntry<>("Bar", 2),
            new AbstractMap.SimpleEntry<>("Baz", 2));

    System.out.println(histogram.toString());
  }
}
