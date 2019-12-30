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
package net.kebernet.xddl.swift.model;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.model.Value;
import org.junit.Test;

public class EnumTest {

  @Test
  public void testSwiftNameFormatting() {
    String result = Enum.makeSwiftNameFriendly("THIS IS A TEST");
    assertThat(result).isEqualTo("thisIsATest");

    result = Enum.makeSwiftNameFriendly("this_is_a_test");
    assertThat(result).isEqualTo("thisIsATest");

    result = Enum.makeSwiftNameFriendly("1");
    assertThat(result).isEqualTo("val_1");
  }

  @Test
  public void testToNameValues() {
    ObjectMapper mapper = Loader.mapper();

    List<Value> values =
        Arrays.asList(
            new Value(mapper.valueToTree(1), null, null),
            new Value(mapper.valueToTree(2), null, null),
            new Value(mapper.valueToTree(3), null, null));

    List<Enum.NameValue> result = Enum.toNameValues(values);
    assertThat(result)
        .containsExactly(
            new Enum.NameValue("val_1", values.get(0).getValue()),
            new Enum.NameValue("val_2", values.get(1).getValue()),
            new Enum.NameValue("val_3", values.get(2).getValue()));

    values =
        Arrays.asList(
            new Value(mapper.valueToTree("foo"), null, null),
            new Value(mapper.valueToTree("bar"), null, null),
            new Value(mapper.valueToTree("baz"), null, null));
    result = Enum.toNameValues(values);

    assertThat(result)
        .containsExactly(
            new Enum.NameValue("foo", null),
            new Enum.NameValue("bar", null),
            new Enum.NameValue("baz", null));

    values =
        Arrays.asList(
            new Value(mapper.valueToTree("FIRST_THING"), null, null),
            new Value(mapper.valueToTree("SECOND_THING"), null, null),
            new Value(mapper.valueToTree("THIRD_THING"), null, null));
    result = Enum.toNameValues(values);

    assertThat(result)
        .containsExactly(
            new Enum.NameValue("firstThing", values.get(0).getValue()),
            new Enum.NameValue("secondThing", values.get(1).getValue()),
            new Enum.NameValue("thirdThing", values.get(2).getValue()));
  }
}
