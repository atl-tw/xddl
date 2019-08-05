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

import static net.kebernet.xddl.model.Utils.neverNull;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.Streams;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;
import net.kebernet.xddl.plugins.Plugin;

@Builder(access = AccessLevel.PUBLIC)
@Getter
public class Runner {
  private File specificationFile;
  private File outputDirectory;
  private List<String> plugins;
  private List<File> includes;
  private Context context;

  public static void main(String... args) {
    Command command = new Command();
    JCommander jCommander;
    try {
      jCommander = JCommander.newBuilder().addObject(command).args(args).build();
      if (command.isHelp()) {
        jCommander.usage();
        return;
      }
      Runner.builder()
          .outputDirectory(command.getOutputDirectory())
          .plugins(command.getFormats())
          .specificationFile(command.getInputFile())
          .includes(command.getIncludes())
          .build()
          .run();
    } catch (Exception e) {
      JCommander.newBuilder().addObject(command).build().usage();
      System.err.print("Error:");
      if (command.isStacktrace()) {
        e.printStackTrace();
      } else {
        System.err.println(e.getMessage());
      }
      System.exit(-1);
    }
  }

  public void run() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    Specification spec = mapper.readValue(specificationFile, Specification.class);
    scanDirectories(neverNull(this.includes), mapper, spec);
    this.context = new Context(mapper, spec);
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

  void scanDirectories(Collection<File> files, ObjectMapper mapper, Specification specification) {
    neverNull(files)
        .forEach(
            f -> {
              if (!f.isDirectory()) {
                throw new RuntimeException(f.getAbsolutePath() + " is not a directory");
              }
              File[] xddls =
                  f.listFiles(scan -> scan.getName().toLowerCase().endsWith(".xddl.json"));
              if (xddls != null) {
                Arrays.stream(xddls).forEach(xddl -> readFile(xddl, mapper, specification));
              }
              File[] directories = f.listFiles(File::isDirectory);
              if (directories != null) {
                scanDirectories(Arrays.asList(directories), mapper, specification);
              }
            });
  }

  private void readFile(File xddl, ObjectMapper mapper, Specification specification) {
    try {
      BaseType type = mapper.readValue(xddl, BaseType.class);
      if (type instanceof Structure) {
        specification.structures().add((Structure) type);
      } else if (type instanceof Type) {
        specification.types().add((Type) type);
      } else {
        throw new RuntimeException(
            xddl.getAbsolutePath()
                + " contains an handle-able type "
                + type.getClass().getCanonicalName());
      }

    } catch (IOException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
