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
package net.kebernet.xddl.swift.model;

public class StringTemplate {

  private final String template;
  private int startIndex = 0;
  private int matchNumber = 0;

  private StringTemplate(String template) {
    this.template = template;
  }

  public static StringTemplate of(String value) {
    return new StringTemplate(value);
  }

  Match untilNextOf(String... possible) {
    for (int i = startIndex + 1; i <= template.length(); i++) {
      String run = template.substring(startIndex, i);
      for (String check : possible) {
        if (run.endsWith(check)) {
          startIndex = i;
          run = run.substring(0, run.length() - check.length());
          return new Match(check, run, matchNumber++);
        }
      }
    }
    return null;
  }

  public String tail() {
    return template.substring(startIndex);
  }

  public static class Match {
    public final String token;
    public final String run;
    public final int matchNumber;

    public Match(String token, String run, int matchNumber) {
      this.token = token;
      this.run = run;
      this.matchNumber = matchNumber;
    }
  }
}
