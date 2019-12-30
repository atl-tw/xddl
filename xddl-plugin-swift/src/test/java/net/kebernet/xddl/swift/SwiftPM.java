/*
 * Copyright 2019, 2020 Robert Cooper, ThoughtWorks
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
package net.kebernet.xddl.swift;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SwiftPM {

  public static void buildDir(File directory) throws IOException, InterruptedException {
    try {
      int returnCode =
          new ProcessBuilder()
              .command("swift", "build")
              .directory(directory)
              .inheritIO()
              .start()
              .waitFor();
      if (returnCode != 0) {
        throw new RuntimeException("Swift build failed for " + directory.getAbsolutePath());
      }
    } catch (IOException e) {
      Logger.getAnonymousLogger().log(Level.WARNING, "swift not on the path", e);
    }
  }
}
