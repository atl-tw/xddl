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
package net.kebernet.xddl.javatestutils;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class JavaTestCompiler {

  private final File directory;

  public JavaTestCompiler(File directory) {
    this.directory = directory;
  }

  public List<File> javaFiles(File directory) {
    ArrayList<File> results = new ArrayList<>();
    File[] java = directory.listFiles(f -> f.getName().endsWith(".java"));
    if (java != null) results.addAll(Arrays.asList(java));
    File[] dirs = directory.listFiles(File::isDirectory);
    if (dirs != null) Arrays.stream(dirs).forEach(f -> results.addAll(javaFiles(f)));
    return results;
  }

  public ClassLoader compile() throws MalformedURLException {

    File[] files = javaFiles(directory).toArray(new File[0]);
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
    Iterable<? extends JavaFileObject> compilationUnits1 =
        fileManager.getJavaFileObjectsFromFiles(Arrays.asList(files));
    compiler.getTask(null, fileManager, null, null, null, compilationUnits1).call();

    URL[] urls = new URL[] {directory.toURI().toURL()};
    return AccessController.doPrivileged(
        (PrivilegedAction<CompositeClassLoader>)
            () ->
                new CompositeClassLoader(
                    Thread.currentThread().getContextClassLoader(), new URLClassLoader(urls)));
  }
}
