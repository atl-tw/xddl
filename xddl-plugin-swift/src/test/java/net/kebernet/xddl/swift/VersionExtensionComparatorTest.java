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
package net.kebernet.xddl.swift;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class VersionExtensionComparatorTest {

  @Test
  public void testParseFilename() {
    String parsed = VersionExtensionComparator.toSemVerString("FOOV1_0_1");
    assertThat(parsed).isEqualTo("1.0.1");
  }

  @Test
  public void testSorting() {
    List<File> files =
        Arrays.asList(new File("FOOV1_1"), new File("FOOV1_10"), new File("FOOV1_0"));
    files.sort(new VersionExtensionComparator());
    assertThat(files)
        .containsExactly(new File("FOOV1_0"), new File("FOOV1_1"), new File("FOOV1_10"))
        .inOrder();
  }
}
