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

import org.junit.Test;

public class StringTemplateTest {

  @Test
  public void testSingleToken() {
    StringTemplate template = StringTemplate.of("ABC$1DEF");
    StringTemplate.Match first = template.untilNextOf("$1");
    StringTemplate.Match second = template.untilNextOf("$1");

    assertThat(first.token).isEqualTo("$1");
    assertThat(first.run).isEqualTo("ABC");
    assertThat(second).isNull();
    assertThat(template.tail()).isEqualTo("DEF");
  }

  @Test
  public void testWithEscapeToken() {
    StringTemplate template = StringTemplate.of("ABC$$1DE$1FGH");
    StringTemplate.Match first = template.untilNextOf("$$", "$1");
    StringTemplate.Match second = template.untilNextOf("$$", "$1");
    StringTemplate.Match third = template.untilNextOf("$$", "$1");
    assertThat(first.token).isEqualTo("$$");
    assertThat(first.run).isEqualTo("ABC");
    assertThat(second.token).isEqualTo("$1");
    assertThat(second.run).isEqualTo("1DE");
    assertThat(third).isNull();
    assertThat(template.tail()).isEqualTo("FGH");
  }
}
