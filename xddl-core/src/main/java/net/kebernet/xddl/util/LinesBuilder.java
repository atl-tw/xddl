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

import com.google.common.escape.CharEscaper;
import com.google.common.escape.CharEscaperBuilder;

/**
 * This is a simple builder for a string that lets you append lines in a format similar to JavaPoet
 * where:
 *
 * <ul>
 *   <li>$$ - escapes a $
 *   <li>$S - is an escaped string value
 *   <li>$L - is a literal string value
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class LinesBuilder {
  private final String[] LINE_ESCAPES = {"$$", "$S", "$L"};
  private static final CharEscaper STRING_ESCAPER =
      (CharEscaper)
          new CharEscaperBuilder()
              .addEscape('\\', "\\\\")
              .addEscape('"', "\\\"")
              .addEscape('\n', "\\n")
              .addEscape('\t', "\\t")
              .addEscape('\r', "\\r")
              .toEscaper();

  StringBuilder sb = new StringBuilder();
  private int indent = 0;

  /**
   * Appends the string value of the object
   *
   * @param o the object
   * @return this lines builder
   */
  public LinesBuilder append(Object o) {
    return append(String.valueOf(o));
  }

  /**
   * Appends a line to the builder
   *
   * @param line the line to append
   * @return this lines builder
   */
  public LinesBuilder append(String line) {
    for (int i = 0; i < indent; i++) {
      sb.append("  ");
    }
    sb.append(line).append('\n');
    return this;
  }

  /**
   * Appends a line replacing the escape tokens with the values
   *
   * @param line The line template
   * @param values The values to replace with
   * @return this lines builder
   */
  public LinesBuilder append(String line, Object... values) {
    StringBuilder lineBuilder = new StringBuilder();
    StringTemplate template = StringTemplate.of(line);
    StringTemplate.Match next = template.untilNextOf(LINE_ESCAPES);
    int matchOffset = 0;
    while (next != null) {
      lineBuilder.append(next.run);
      switch (next.token) {
        case "$$":
          lineBuilder.append("$");
          // A $$ escape doesn't have a value, so we need to backstep the matchOffset from the
          // template.
          matchOffset--;
          break;
        case "$L":
          String value = String.valueOf(values[next.matchNumber + matchOffset]);
          lineBuilder.append(value);
          break;
        case "$S":
          lineBuilder
              .append("\"")
              .append(STRING_ESCAPER.escape(String.valueOf(values[next.matchNumber + matchOffset])))
              .append("\"");
          break;
        default:
          throw new IllegalStateException("Unexpected token " + next.token);
      }
      next = template.untilNextOf(LINE_ESCAPES);
    }
    lineBuilder.append(template.tail());
    return this.append(lineBuilder.toString());
  }

  /**
   * Indents subsequent lines by one step
   *
   * @return this lines builder.
   */
  public LinesBuilder indent() {
    indent++;
    return this;
  }

  /**
   * Outdents subsequent lines by one step
   *
   * @return this lines builder
   */
  public LinesBuilder outdent() {
    indent--;
    if (indent < 0) {
      throw new IllegalStateException("Can't outdent any more");
    }
    return this;
  }

  @Override
  public String toString() {
    if (indent > 0) {
      throw new IllegalStateException("Un-outdented indent at level " + indent);
    }
    return sb.toString();
  }

  public LinesBuilder blank() {
    return append("");
  }
}
