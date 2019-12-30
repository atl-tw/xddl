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
package net.kebernet.xddl.graphwalker.defaults;

import static com.google.common.truth.Truth.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import net.kebernet.xddl.graphwalker.Child;
import org.junit.Test;

public class RecurseIterablesWrapperTest {

  @Test
  public void testListOfThings() {
    HasListOfChild target = new HasListOfChild();
    LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
    expected.put(
        HasListOfChild.class.getCanonicalName() + ".listOfChild[0]",
        target.getListOfChild().get(0));
    expected.put(
        HasListOfChild.class.getCanonicalName() + ".listOfChild[1]",
        target.getListOfChild().get(1));
    expected.put(HasListOfChild.class.getCanonicalName() + ".whatever", "whatever");

    Map<String, ?> values = RecurseIterablesWrapper.wrap(PropertiesStrategy.create()).apply(target);
    assertThat(values).isEqualTo(expected);
  }

  @Test
  public void testHasNoList() {
    Child target = new Child();
    LinkedHashMap<String, Object> expected = new LinkedHashMap<>();
    expected.put(Child.class.getCanonicalName() + ".bar", "bar");
    expected.put(Child.class.getCanonicalName() + ".cycleRef", null);
    expected.put(Child.class.getCanonicalName() + ".foo", "childFoo");
    expected.put(Child.class.getCanonicalName() + ".baz", "baz");

    Map<String, ?> values = RecurseIterablesWrapper.wrap(PropertiesStrategy.create()).apply(target);
    assertThat(values).isEqualTo(expected);
  }
}
