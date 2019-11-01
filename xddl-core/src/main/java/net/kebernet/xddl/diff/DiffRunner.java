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
package net.kebernet.xddl.diff;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.model.Specification;

@Builder(access = AccessLevel.PUBLIC)
public class DiffRunner {

  private DiffCommand command;

  public void run() throws IOException {
    Specification left =
        Loader.builder()
            .main(command.getLeftFile())
            .includes(command.getLeftIncludes())
            .build()
            .read();

    Specification right =
        Loader.builder()
            .main(command.getRightFile())
            .includes(command.getRightIncludes())
            .build()
            .read();

    SpecificationDiff diff = new SpecificationDiff(Loader.mapper(), left, right);
    Set<SchemaElement> difference = diff.diff();
    Set<Boolean> results =
        difference.stream()
            .map(e -> command.isComparison() ? compare(e, diff) : output(e))
            .collect(Collectors.toSet());
    if (!results.isEmpty()) {
      System.exit(-1);
    }
  }

  private boolean output(SchemaElement e) {
    return true;
  }

  private boolean compare(SchemaElement e, SpecificationDiff diff) {
    System.err.println();
    System.err.println("left :" + diff.left(e.pathToDotNotation()));
    System.err.println("right:" + diff.right(e.pathToDotNotation()));
    return true;
  }
}
