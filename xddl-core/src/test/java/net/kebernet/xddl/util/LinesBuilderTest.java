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

public class LinesBuilderTest {

  @Test
  public void testTemplates() {
    LinesBuilder lb = new LinesBuilder();
    lb.append("A$$BC$S123$LDEF", "F\"OO", "BAR");
    assertThat(lb.toString()).isEqualTo("A$BC\"F\\\"OO\"123BARDEF\n");
  }

  @Test
  public void testLineEnding() {
    LinesBuilder lb = new LinesBuilder();
    lb.append("3$L", "BAR");
    assertThat(lb.toString()).isEqualTo("3BAR\n");
  }
}
