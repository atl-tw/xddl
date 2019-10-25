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
package net.kebernet.xddl.glide;

import static net.kebernet.xddl.model.Utils.neverNull;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import net.kebernet.xddl.SemanticVersion;
import net.kebernet.xddl.unify.UnifyCommand;
import net.kebernet.xddl.unify.UnifyRunner;

@Builder
public class GlideRunner {

  private GlideCommand command;

  public void run() throws IOException {
    File outputDirectory = command.getOutputDirectory();
    File baseline = new File(outputDirectory, "baseline.xddl.json");
    UnifyRunner.builder()
        .command(
            UnifyCommand.builder()
                .inputFile(command.getInputFile())
                .includes(command.getIncludes())
                .outputFile(baseline)
                .vals(command.getVals())
                .valsFile(command.getValsFile())
                .evaluateOgnl(false)
                .build())
        .build()
        .run();
    List<File> versions =
        Arrays.asList(neverNull(command.getPatches().listFiles(File::isDirectory)));
    File lastUnified = baseline;
    HashMap<SemanticVersion, File> versionLookup = new HashMap<>();
    List<SemanticVersion> ordered =
        versions.stream()
            .map(
                f -> {
                  SemanticVersion version = new SemanticVersion(f.getName());
                  versionLookup.put(version, f);
                  return version;
                })
            .sorted()
            .collect(Collectors.toList());
    for (SemanticVersion version : ordered) {
      File patches = versionLookup.get(version);
      File outputFile =
          new File(outputDirectory, patches.getName().replaceAll("\\.", "_") + ".xddl.json");
      UnifyRunner.builder()
          .command(
              UnifyCommand.builder()
                  .newVersion(version.toString())
                  .scrubPatch(true)
                  .inputFile(lastUnified)
                  .patches(Collections.singletonList(patches))
                  .outputFile(outputFile)
                  .evaluateOgnl(false)
                  .build())
          .build()
          .run();
      lastUnified = outputFile;
    }
  }
}
