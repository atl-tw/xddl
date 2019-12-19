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
package net.kebernet.xddl;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class SemanticVersionTest {

  @Test
  public void simple() {
    assertThat(new SemanticVersion("1.0")).isEqualTo(new SemanticVersion("1.0"));
  }

  @Test
  public void simpleExtended() {
    assertThat(new SemanticVersion("1.0")).isEqualTo(new SemanticVersion("1.0.0"));
  }

  @Test
  public void simpleExtendedInverse() {
    assertThat(new SemanticVersion("1.0.0.0")).isEqualTo(new SemanticVersion("1.0"));
  }

  @Test
  public void simpleGreater() {
    assertThat(new SemanticVersion("1.1")).isGreaterThan(new SemanticVersion("1.0"));
  }

  @Test
  public void subGreater() {
    assertThat(new SemanticVersion("1.1.1")).isGreaterThan(new SemanticVersion("1.1"));
  }

  @Test
  public void majorGreater() {
    assertThat(new SemanticVersion("2.1")).isGreaterThan(new SemanticVersion("1"));
  }

  @Test
  public void minorLess() {
    assertThat(new SemanticVersion("2.0")).isLessThan(new SemanticVersion("2.0.1"));
  }

  @Test
  public void subWhatever() {
    assertThat(new SemanticVersion("2.0.2")).isGreaterThan(new SemanticVersion("2.0.1"));
  }

  @Test
  public void testIsGreaterThan() {
    assertThat(new SemanticVersion("2.0").isGreaterThan(new SemanticVersion("1.0"))).isTrue();
  }
}
