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
package net.kebernet.xddl.markdown;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import org.junit.Test;

public class MarkdownPluginTest {

  @Test
  public void simpleTest() throws IOException {
    MarkdownPlugin instance = new MarkdownPlugin();
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    Specification spec =
        mapper.readValue(
            MarkdownPlugin.class.getResourceAsStream("/sample.json"), Specification.class);
    Context ctx = new Context(mapper, spec);
    File basicDir = new File("build/test/basic");
    basicDir.mkdirs();
    instance.generateArtifacts(ctx, basicDir);
  }
}
