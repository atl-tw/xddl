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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Spliterators;
import java.util.stream.Collectors;

import com.google.common.collect.Streams;
import lombok.AccessLevel;
import lombok.Builder;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import net.kebernet.xddl.plugins.Plugin;

@Builder(access = AccessLevel.PUBLIC)
public class Runner {
  private File specificationFile;
  private File outputDirectory;
  private List<String> plugins;

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
          .build()
          .run();
    } catch (IllegalStateException|ParameterException e) {
      System.err.println(e.getMessage());
      JCommander.newBuilder().addObject(command).build().usage();
    } catch (Exception e) {
      e.printStackTrace();
      JCommander.newBuilder().addObject(command).build().usage();
    }
  }

  public void run() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    Specification spec = mapper.readValue(specificationFile, Specification.class);
    Context context = new Context(mapper, spec);
    ServiceLoader<Plugin> implementations = ServiceLoader.load(Plugin.class);
    Set<String> known = Streams.stream(implementations).map(Plugin::getName).collect(Collectors.toSet());
    for(String name : plugins){
      if(!known.contains(name)){
        throw context.stateException("Unknown plugin: "+name+" known plugins: "+known, null);
      }
    }
    for (Plugin plugin : implementations) {
      if (plugins.contains(plugin.getName())) {
        plugin.generateArtifacts(context, outputDirectory);
      }
    }
  }
}
