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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
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

  public void run(List<String> packageNames, File outputFile)
      throws IOException, ClassNotFoundException {
    HashSet<Class> classes = new HashSet<>();
    for (String s : packageNames) {
      classes.addAll(Arrays.asList(getClasses(s)));
    }
    Generator generator = new Generator(mapper, classes);
    mapper.writeValue(outputFile, generator.generate());
  }

  private static Class[] getClasses(String packageName) throws ClassNotFoundException, IOException {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    assert classLoader != null;
    String path = packageName.replace('.', '/');
    Enumeration<URL> resources = classLoader.getResources(path);
    List<File> dirs = new ArrayList<>();
    while (resources.hasMoreElements()) {
      URL resource = resources.nextElement();
      dirs.add(new File(resource.getFile()));
    }
    ArrayList classes = new ArrayList();
    for (File directory : dirs) {
      classes.addAll(findClasses(directory, packageName));
    }
    return (Class[]) classes.toArray(new Class[classes.size()]);
  }

  private static List<Class> findClasses(File directory, String packageName)
      throws ClassNotFoundException {
    List<Class> classes = new ArrayList<>();
    if (!directory.exists()) {
      return classes;
    }
    File[] files = directory.listFiles();
    for (File file : files == null ? new File[0] : files) {
      if (file.isDirectory()) {
        assert !file.getName().contains(".");
        classes.addAll(findClasses(file, packageName + "." + file.getName()));
      } else if (file.getName().endsWith(".class")) {
        classes.add(
            Class.forName(
                packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
      }
    }
    return classes;
  }
}
