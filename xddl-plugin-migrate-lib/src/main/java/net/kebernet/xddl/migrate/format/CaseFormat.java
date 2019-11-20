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
package net.kebernet.xddl.migrate.format;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public enum CaseFormat {
  LOWER_WORDS(
      new Implementation() {
        @Override
        public String[] tokens(String string) {
          return string.split("\\s");
        }

        @Override
        public String format(String[] tokens) {
          StringBuilder result = new StringBuilder();
          for (String s : tokens) {
            if (result.length() != 0) {
              result.append(' ');
            }
            result.append(s.toLowerCase());
          }
          return result.toString();
        }
      }),

  UPPER_WORDS(
      new Implementation() {
        @Override
        public String[] tokens(String string) {
          return LOWER_WORDS.implementation.tokens(string);
        }

        @Override
        public String format(String[] tokens) {
          StringBuilder result = new StringBuilder();
          for (String s : tokens) {
            if (result.length() != 0) {
              result.append(' ');
            }
            upperFirst(result, s);
          }
          return result.toString();
        }
      }),

  UPPER_CAMEL(
      new Implementation() {
        @Override
        public String[] tokens(String string) {
          List<String> result = new ArrayList<>();

          StringBuilder current = new StringBuilder();
          for (char c : string.toCharArray()) {
            if (Character.isUpperCase(c)) {
              if (current.length() != 0) {
                result.add(current.toString());
              }
              current = new StringBuilder().append(c);
            } else {
              current.append(c);
            }
          }
          if (current.length() > 0) {
            result.add(current.toString());
          }
          return result.toArray(new String[0]);
        }

        @Override
        public String format(String[] tokens) {
          StringBuilder result = new StringBuilder();
          for (String t : tokens) {
            upperFirst(result, t);
          }
          return result.toString();
        }
      }),

  LOWER_CAMEL(
      new Implementation() {
        @Override
        public String[] tokens(String string) {
          return UPPER_CAMEL.implementation.tokens(string);
        }

        @Override
        public String format(String[] tokens) {
          StringBuilder result = new StringBuilder();
          for (String t : tokens) {
            char[] chars = t.toCharArray();
            for (int i = 0; i < chars.length; i++) {
              if (i == 0 && result.length() != 0) {
                result.append(Character.toUpperCase(chars[i]));
              } else {
                result.append(Character.toLowerCase(chars[i]));
              }
            }
          }
          return result.toString();
        }
      }),

  LOWER_HYPHEN(
      new Implementation() {
        @Override
        public String[] tokens(String string) {
          return string.split("-");
        }

        @Override
        public String format(String[] tokens) {
          StringBuilder result = new StringBuilder();
          for (String t : tokens) {
            char[] chars = t.toCharArray();
            for (int i = 0; i < chars.length; i++) {
              if (i == 0 && result.length() != 0) {
                result.append("-");
              }
              result.append(Character.toLowerCase(chars[i]));
            }
          }
          return result.toString();
        }
      }),

  LOWER_SNAKE(
      new Implementation() {
        @Override
        public String[] tokens(String string) {
          return string.split("_");
        }

        @Override
        public String format(String[] tokens) {
          StringBuilder result = new StringBuilder();
          for (String t : tokens) {
            char[] chars = t.toCharArray();
            for (int i = 0; i < chars.length; i++) {
              if (i == 0 && result.length() != 0) {
                result.append("_");
              }
              result.append(Character.toLowerCase(chars[i]));
            }
          }
          return result.toString();
        }
      }),

  UPPER_SNAKE(
      new Implementation() {
        @Override
        public String[] tokens(String string) {
          return string.split("_");
        }

        @Override
        public String format(String[] tokens) {
          StringBuilder result = new StringBuilder();
          for (String t : tokens) {
            char[] chars = t.toCharArray();
            for (int i = 0; i < chars.length; i++) {
              if (i == 0 && result.length() != 0) {
                result.append("_");
              }
              result.append(Character.toUpperCase(chars[i]));
            }
          }
          return result.toString();
        }
      });

  private static void upperFirst(StringBuilder result, String s) {
    char[] chars = s.toCharArray();
    for (int i = 0; i < chars.length; i++) {
      if (i == 0) {
        result.append(Character.toUpperCase(chars[i]));
      } else {
        result.append(Character.toLowerCase(chars[i]));
      }
    }
  }

  private final Implementation implementation;

  CaseFormat(Implementation implementation) {
    this.implementation = implementation;
  }

  public Function<String, String> to(CaseFormat target) {
    return (s) -> target.implementation.format(this.implementation.tokens(s));
  }

  public Function<String, String> from(CaseFormat target) {
    return (s) -> this.implementation.format(target.implementation.tokens(s));
  }

  private interface Implementation {
    String[] tokens(String string);

    String format(String[] tokens);
  }
}
