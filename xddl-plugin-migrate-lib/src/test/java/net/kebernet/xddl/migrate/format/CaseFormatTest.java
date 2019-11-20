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

import com.google.common.truth.Truth;
import org.junit.Test;

public class CaseFormatTest {

  @Test
  public void testWordsToUpperCamel() {
    Truth.assertThat(CaseFormat.UPPER_WORDS.to(CaseFormat.UPPER_CAMEL).apply("This is a test"))
        .isEqualTo("ThisIsATest");
  }

  @Test
  public void testWordsToLowerCamel() {
    Truth.assertThat(CaseFormat.UPPER_WORDS.to(CaseFormat.LOWER_CAMEL).apply("This is a test"))
        .isEqualTo("thisIsATest");
  }

  @Test
  public void testUpperCamelToLowerCamel() {
    Truth.assertThat(CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL).apply("ThisIsATest"))
        .isEqualTo("thisIsATest");
  }

  @Test
  public void testLowerCamelToUpperCamel() {
    Truth.assertThat(CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL).apply("thisIsATest"))
        .isEqualTo("ThisIsATest");
  }

  @Test
  public void testLowerCamelToLowerHyphen() {
    Truth.assertThat(CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_HYPHEN).apply("thisIsATest"))
        .isEqualTo("this-is-a-test");
  }

  @Test
  public void testLowerHyphenToUpperCamel() {
    Truth.assertThat(CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL).apply("This-is-A-test"))
        .isEqualTo("ThisIsATest");
  }

  @Test
  public void testLowerHyphenToUpperSnake() {
    Truth.assertThat(CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_SNAKE).apply("This-is-A-test"))
        .isEqualTo("THIS_IS_A_TEST");
  }

  @Test
  public void testUpperSnakeToUpperCame1() {
    Truth.assertThat(CaseFormat.UPPER_SNAKE.to(CaseFormat.UPPER_CAMEL).apply("THIS1_IS_A_TEST"))
        .isEqualTo("This1IsATest");
  }

  @Test
  public void testConverLowerSnakeToUpperCamel() {
    Truth.assertThat(CaseFormat.LOWER_SNAKE.from(CaseFormat.UPPER_CAMEL).apply("ThisIsATest"))
        .isEqualTo("this_is_a_test");
  }

  @Test
  public void testLowerSnakeToUpperWords() {
    Truth.assertThat(CaseFormat.LOWER_SNAKE.to(CaseFormat.UPPER_WORDS).apply("this1_is_a_test"))
        .isEqualTo("This1 Is A Test");
  }

  @Test
  public void testLowerSnakeToLowerWords() {
    Truth.assertThat(CaseFormat.LOWER_SNAKE.to(CaseFormat.LOWER_WORDS).apply("this1_is_a_test"))
        .isEqualTo("this1 is a test");
  }
}
