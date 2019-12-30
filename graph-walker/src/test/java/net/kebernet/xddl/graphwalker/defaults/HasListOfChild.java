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
package net.kebernet.xddl.graphwalker.defaults;

import java.util.Arrays;
import java.util.List;
import net.kebernet.xddl.graphwalker.Child;

public class HasListOfChild {

  private List<Child> listOfChild = Arrays.asList(new Child(), new Child());
  private String whatever = "whatever";

  public List<Child> getListOfChild() {
    return listOfChild;
  }

  public String getWhatever() {
    return whatever;
  }
}
