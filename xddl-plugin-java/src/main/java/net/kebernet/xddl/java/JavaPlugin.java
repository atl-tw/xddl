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
package net.kebernet.xddl.java;

import static net.kebernet.xddl.model.Utils.isNullOrEmpty;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;
import net.kebernet.xddl.plugins.Context;
import net.kebernet.xddl.plugins.Plugin;

public class JavaPlugin implements Plugin {
  @Override
  public String getName() {
    return "java";
  }

  @Override
  public String generateArtifacts(Context context, File outputDirectory) throws IOException {
    Stream<EnumClass> enums =
        context.getSpecification().types().stream()
            .filter(t -> !isNullOrEmpty(t.getAllowable()))
            .map(t -> new EnumClass(context, t, t, null));
    Stream<StructureClass> structures =
        context.getSpecification().structures().stream()
            .map(s -> new StructureClass(context, s, null));

    Stream<Writable> writables = Stream.concat(enums, structures);
    writables.forEach(
        w -> {
          try {
            w.write(outputDirectory);
          } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
          }
        });
    return outputDirectory.getAbsolutePath();
  }
}
