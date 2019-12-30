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
package net.kebernet.xddl.diff;

import com.beust.jcommander.Parameter;
import java.io.File;
import java.util.List;
import lombok.Getter;
import net.kebernet.xddl.HasStacktrace;

@Getter
public class DiffCommand implements HasStacktrace {

  @Parameter(
      names = {"--left-file", "-l"},
      description = "The left hand file.",
      required = true)
  private File leftFile;

  @Parameter(
      names = {"--left-include-dir", "-ld"},
      description = "Directory(ies) to scan for *.xddl.json files to include.")
  private List<File> leftIncludes;

  @Parameter(
      names = {"--right-file", "-r"},
      description = "The left hand file.",
      required = true)
  private File rightFile;

  @Parameter(
      names = {"--right-include-dir", "-rd"},
      description = "Directory(ies) to scan for *.xddl.json files to include.")
  private List<File> rightIncludes;

  @Parameter(
      names = "--comparision",
      description = "Show comparision rather than just missing fields")
  private boolean comparison = false;

  @Parameter(names = "--help", description = "Show this help text", help = true)
  private boolean help = false;

  @Parameter(names = "--stacktrace", description = "Show the stacktrace of an error")
  private boolean stacktrace = false;
}
