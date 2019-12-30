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
package net.kebernet.xddl.unify;

import com.beust.jcommander.Parameter;
import java.io.File;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import net.kebernet.xddl.HasStacktrace;

@Data
@Builder
public class UnifyCommand implements HasStacktrace {
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
      names = {"--output-file", "-o"},
      description = "The file to output generated artifacts to.",
      required = true)
  private File outputFile;

  @Parameter(
      names = {"--scrub-patch", "-s"},
      description = "scrubs patch-delete operations from the original")
  private boolean scrubPatch;

  @Parameter(
      names = {"--new-version", "-nb"},
      description = "The version string of the unified file")
  private String newVersion;

  @Parameter(names = "--help", description = "Show this help text", help = true)
  private boolean help;

  @Parameter(names = "--stacktrace", description = "Show the stacktrace of an error")
  private boolean stacktrace;

  private Map<String, Object> vals;

  @Parameter(
      names = {"--vals-file", "-v"},
      description = "JSON file of values")
  private File valsFile;

  @Parameter(
      names = {"--evaluate-ognl", "-eval"},
      description = "Should we evaluate the OGNL in the file (default true)")
  @Builder.Default
  private boolean evaluateOgnl = true;
}
