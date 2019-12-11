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
package net.kebernet.xddl.glide;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.model.Specification;
import org.junit.Test;

public class GlideRunnerTest {

  @Test
  public void testNamePatches() throws IOException {
    File output = new File("build/test/glide");
    output.mkdirs();
    GlideRunner.builder()
        .command(
            GlideCommand.builder()
                .inputFile(new File("src/test/resources/glide/Specification.xddl.json"))
                .includes(Collections.singletonList(new File("src/test/resources/glide/includes")))
                .patches(new File("src/test/resources/glide/glide"))
                .outputDirectory(output)
                .build())
        .build()
        .run();
    ;
    Specification specification =
        Loader.builder().main(new File("build/test/glide/1_0_2.xddl.json")).build().read();
    assertThat(specification.types()).isEmpty();

    specification =
        Loader.builder().main(new File("build/test/glide/1_0_1.xddl.json")).build().read();
    assertThat(specification.types()).isNotEmpty();
    specification =
        Loader.builder().main(new File("build/test/glide/baseline.xddl.json")).build().read();
    assertThat(specification.types()).isNotEmpty();
  }
}
