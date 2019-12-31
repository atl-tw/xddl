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
package net.kebernet.xddl.swift;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.Serializable;
import java.util.Comparator;
import net.kebernet.xddl.SemanticVersion;

public class VersionExtensionComparator implements Comparator<File>, Serializable {

  @Override
  public int compare(File o1, File o2) {
    return new SemanticVersion(toSemVerString(o1.getName()))
        .compareTo(new SemanticVersion(toSemVerString(o2.getName())));
  }

  @VisibleForTesting
  static String toSemVerString(String fileName) {
    int v = fileName.lastIndexOf("V");
    if (v != -1) {
      fileName = fileName.substring(v + 1);
      return fileName.replaceAll("_", ".");
    } else {
      return "0";
    }
  }
}
