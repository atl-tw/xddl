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
package net.kebernet.xddl.powerglide.metadata;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import net.kebernet.xddl.SemanticVersion;
import org.junit.Test;

public class GlideMetadataReaderTest {

  @Test
  public void testSampleGlide() throws IOException {
    Map<SemanticVersion, PackageMetadata> results =
        new GlideMetadataReader().readGlideFolder(new File("src/test/resources/glide-metadata"));

    assertThat(results.keySet())
        .containsExactly(
            new SemanticVersion("1.0"), new SemanticVersion("1.0.1"), new SemanticVersion("1.0.2"));
    assertThat(results.get(new SemanticVersion("1.0.2")).migrationVisitor())
        .isEqualTo("com.my.project.model.v1_0_2.migration.Team");
  }
}
