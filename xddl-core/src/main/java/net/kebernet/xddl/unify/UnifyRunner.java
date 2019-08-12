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

import java.io.File;
import java.io.IOException;
import lombok.Builder;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.model.Specification;

@Builder
public class UnifyRunner {

  private UnifyCommand command;

  public void run() throws IOException {
    Specification base =
        Loader.builder()
            .main(command.getInputFile())
            .includes(command.getIncludes())
            .patches(command.getPatches())
            .build()
            .read();
    Loader.mapper()
        .writeValue(new File(command.getOutputDirectory(), command.getInputFile().getName()), base);
  }
}
