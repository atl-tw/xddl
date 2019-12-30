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
package net.kebernet.xddl;

import com.beust.jcommander.JCommander;
import java.util.stream.Stream;
import lombok.Getter;
import net.kebernet.xddl.diff.DiffCommand;
import net.kebernet.xddl.diff.DiffRunner;
import net.kebernet.xddl.generate.GenerateCommand;
import net.kebernet.xddl.generate.GenerateRunner;
import net.kebernet.xddl.glide.GlideCommand;
import net.kebernet.xddl.glide.GlideRunner;
import net.kebernet.xddl.unify.UnifyCommand;
import net.kebernet.xddl.unify.UnifyRunner;

@Getter
public class Runner {

  public static void main(String... args) {
    GenerateCommand command = new GenerateCommand();
    DiffCommand diffCommand = new DiffCommand();
    UnifyCommand unifyCommand = UnifyCommand.builder().build();
    GlideCommand glideCommand = GlideCommand.builder().build();
    JCommander jCommander =
        JCommander.newBuilder()
            .addCommand("generate", command)
            .addCommand("diff", diffCommand)
            .addCommand("unify", unifyCommand)
            .addCommand("glide", glideCommand)
            .args(args)
            .build();
    try {

      switch (jCommander.getParsedCommand()) {
        case "generate":
          if (command.isHelp()) {
            jCommander.usage("generate");
            return;
          }
          GenerateRunner.builder()
              .outputDirectory(command.getOutputDirectory())
              .plugins(command.getFormats())
              .specificationFile(command.getInputFile())
              .includes(command.getIncludes())
              .vals(command.getVals())
              .valsFile(command.getValsFile())
              .build()
              .run();
          break;
        case "diff":
          if (diffCommand.isHelp()) {
            jCommander.usage("diff");
            return;
          }
          DiffRunner.builder().command(diffCommand).build().run();
          break;
        case "unify":
          if (unifyCommand.isHelp()) {
            jCommander.usage("unify");
            return;
          }
          UnifyRunner.builder().command(unifyCommand).build().run();
          break;
        case "glide":
          if (glideCommand.isHelp()) {
            jCommander.usage("glide");
            return;
          }
          GlideRunner.builder().command(glideCommand).build().run();
          break;
        default:
          throw new UnsupportedOperationException(
              "Unknown command " + jCommander.getParsedCommand());
      }

    } catch (Exception e) {
      jCommander.usage(jCommander.getParsedCommand());
      System.err.print("Error:");
      boolean isStackTrace =
          Stream.of(command, diffCommand, glideCommand, unifyCommand)
              .anyMatch(HasStacktrace::isStacktrace);
      if (isStackTrace) {
        e.printStackTrace();
      } else {
        System.err.println(e.toString());
      }
      System.exit(-1);
    }
  }
}
