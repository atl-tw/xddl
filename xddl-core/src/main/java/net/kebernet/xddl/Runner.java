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

import com.beust.jcommander.JCommander;
import lombok.Getter;
import net.kebernet.xddl.diff.DiffCommand;
import net.kebernet.xddl.diff.DiffRunner;
import net.kebernet.xddl.generate.GenerateCommand;
import net.kebernet.xddl.generate.GenerateRunner;
import net.kebernet.xddl.unify.UnifyCommand;
import net.kebernet.xddl.unify.UnifyRunner;

@Getter
public class Runner {

  public static void main(String... args) {
    GenerateCommand command = new GenerateCommand();
    DiffCommand diffCommand = new DiffCommand();
    UnifyCommand unifyCommand = new UnifyCommand();
    JCommander jCommander;
    try {
      jCommander =
          JCommander.newBuilder()
              .addCommand("generate", command)
              .addCommand("diff", diffCommand)
              .addCommand("unify", unifyCommand)
              .args(args)
              .build();
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
        default:
          throw new UnsupportedOperationException(
              "Unknown command " + jCommander.getParsedCommand());
      }

    } catch (Exception e) {
      JCommander.newBuilder().addObject(command).build().usage();
      System.err.print("Error:");
      if (command.isStacktrace()) {
        e.printStackTrace();
      } else {
        System.err.println(e.getMessage());
      }
      System.exit(-1);
    }
  }
}
