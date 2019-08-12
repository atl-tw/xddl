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
package net.kebernet.xddl.unify;

import com.beust.jcommander.Parameter;
import java.io.File;
import java.util.List;
import lombok.Data;

@Data
public class UnifyCommand {
  @Parameter(
      names = {"--input-file", "-i"},
      description = "The specification file.",
      required = true)
  private File inputFile;

  @Parameter(
      names = {"--include-dir", "-d"},
      description = "Directory(ies) to scan for *.xddl.json files to include.")
  private List<File> includes;

  @Parameter(
      names = {"--patches-dir", "-p"},
      description = "Directory(ies) to scan for *.patch.json files to include.")
  private List<File> patches;

  @Parameter(
      names = {"--output-directory", "-o"},
      description = "The directory to output generated artifacts to.",
      required = true)
  private File outputDirectory;

  @Parameter(names = "--help", description = "Show this help text", help = true)
  private boolean help = false;

  @Parameter(names = "--stacktrace", description = "Show the stacktrace of an error")
  private boolean stacktrace = false;
}
