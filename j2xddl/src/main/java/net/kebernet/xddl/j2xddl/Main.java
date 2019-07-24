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
package net.kebernet.xddl.j2xddl;

import static net.kebernet.xddl.j2xddl.Reflection.findClasses;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;

public class Main {

  private final ObjectMapper mapper = new ObjectMapper();

  {
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
  }

  public static void main(String... args) {
    Command command = new Command();
    JCommander jCommander;
    try {
      jCommander = JCommander.newBuilder().addObject(command).args(args).build();
      if (command.isHelp()) {
        jCommander.usage();
        return;
      }
      new Main().run(command.getPackageNames(), command.getOutputFile());
    } catch (ParameterException e) {
      System.err.println(e.getMessage());
      JCommander.newBuilder().addObject(command).build().usage();
    } catch (Exception e) {
      e.printStackTrace();
      JCommander.newBuilder().addObject(command).build().usage();
    }
  }

  void run(List<String> packageNames, File outputFile) throws IOException, ClassNotFoundException {
    HashSet<Class> classes = new HashSet<>();
    for (String s : packageNames) {
      classes.addAll(findClasses(s));
    }
    Generator generator = new Generator(mapper, classes);
    mapper.writeValue(outputFile, generator.generate());
  }
}
