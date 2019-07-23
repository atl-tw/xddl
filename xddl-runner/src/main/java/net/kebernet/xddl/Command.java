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

import com.beust.jcommander.Parameter;
import java.io.File;
import java.util.List;
import lombok.Data;

@Data
public class Command {

  @Parameter(
      names = {"--input-file", "-i"},
      description = "The specification file.",
      required = true)
  private File inputFile;

  @Parameter(
      names = {"--output-directory", "-o"},
      description = "The directory to output generated artifacts to.",
      required = true)
  private File outputDirectory;

  @Parameter(
      names = {"--format", "-f"},
      description = "The output plugin to generate",
      required = true)
  private List<String> formats;

  @Parameter(names = "--help", description = "Show this help text", help = true)
  private boolean help = false;
}
