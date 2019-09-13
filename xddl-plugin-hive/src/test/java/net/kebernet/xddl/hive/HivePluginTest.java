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
package net.kebernet.xddl.hive;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import org.junit.Test;

public class HivePluginTest {

  @Test
  public void smokeTest() throws IOException {
    File[] files = new File("src/test/resources").listFiles(f -> f.getName().endsWith(".json"));

    HivePlugin plugin = new HivePlugin();
    for (File f : files) {
      ObjectMapper mapper = new ObjectMapper();
      Specification spec = mapper.readValue(f, Specification.class);
      Context context = new Context(mapper, spec);
      File outputDirectory = new File("build/test/" + f.getName());
      //noinspection ResultOfMethodCallIgnored
      outputDirectory.mkdirs();
      plugin.generateArtifacts(context, outputDirectory);
    }
  }
}
