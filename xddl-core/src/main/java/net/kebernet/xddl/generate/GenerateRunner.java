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
package net.kebernet.xddl.generate;

import static net.kebernet.xddl.model.Utils.neverNull;

import com.google.common.collect.Streams;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import net.kebernet.xddl.plugins.Plugin;

@Builder(access = AccessLevel.PUBLIC)
@Getter
public class GenerateRunner {

  private File specificationFile;
  private File outputDirectory;
  private List<String> plugins;
  private List<File> includes;
  private Context context;
  private Map<String, Object> vals;
  private File valsFile;

  public void run() throws IOException {

    Specification specification =
        Loader.builder()
            .main(specificationFile)
            .includes(this.includes)
            .valsFile(valsFile)
            .vals(vals)
            .build()
            .read();
    this.context = new Context(Loader.mapper(), specification);
    Iterable<Plugin> implementations = neverNull(ServiceLoader.load(Plugin.class));
    Set<String> known =
        Streams.stream(implementations).map(Plugin::getName).collect(Collectors.toSet());
    for (String name : neverNull(plugins)) {
      if (!known.contains(name)) {
        throw context.stateException("Unknown plugin: " + name + " known plugins: " + known, null);
      }
    }
    for (Plugin plugin : implementations) {
      if (plugins.contains(plugin.getName())) {
        plugin.generateArtifacts(context, outputDirectory);
      }
    }
  }
}
