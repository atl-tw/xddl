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

import static net.kebernet.xddl.model.Utils.neverNull;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SemanticVersion implements Comparable<SemanticVersion> {
  List<Integer> versions;

  public SemanticVersion(String versions) {
    this.versions =
        Splitter.on('.').trimResults().omitEmptyStrings().splitToList(versions).stream()
            .map(Integer::valueOf)
            .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return Joiner.on('.').join(versions);
  }

  @Override
  public int compareTo(SemanticVersion o) {
    ArrayList<IntegerPair> pairs = new ArrayList<>();
    int max = Math.max(this.versions.size(), o.versions.size());
    for (int i = 0; i < max; i++) {
      Integer left = this.versions.size() > i ? this.versions.get(i) : null;
      Integer right = o.versions.size() > i ? o.versions.get(i) : null;
      pairs.add(new IntegerPair(left, right));
    }
    return pairs.stream().map(IntegerPair::compare).filter(p -> p != 0).findFirst().orElse(0);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SemanticVersion)) return false;
    SemanticVersion that = (SemanticVersion) o;
    return this.compareTo(that) == 0;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(toString());
  }

  private static class IntegerPair {
    final Integer left;
    final Integer right;

    private IntegerPair(Integer left, Integer right) {
      this.left = left;
      this.right = right;
    }

    int compare() {
      return neverNull(left).compareTo(neverNull(right));
    }
  }
}
