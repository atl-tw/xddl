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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CompositeClassLoader extends ClassLoader {

  private final List<ClassLoader> classLoaders = Collections.synchronizedList(new ArrayList<>());

  CompositeClassLoader(ClassLoader... loaders) {
    add(Object.class.getClassLoader());
    add(getClass().getClassLoader());
    for (ClassLoader loader : loaders) {
      this.add(loader);
    }
  }

  /**
   * Add a loader to the composite
   *
   * @param classLoader The nested classloader
   */
  private void add(ClassLoader classLoader) {
    if (classLoader != null) {
      classLoaders.add(0, classLoader);
    }
  }

  public Class loadClass(String name) throws ClassNotFoundException {
    for (ClassLoader classLoader : classLoaders) {
      try {
        return classLoader.loadClass(name);
      } catch (ClassNotFoundException notFound) {
        // noop
      }
    }
    ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
    if (contextClassLoader != null) {
      return contextClassLoader.loadClass(name);
    } else {
      throw new ClassNotFoundException(name);
    }
  }
}
