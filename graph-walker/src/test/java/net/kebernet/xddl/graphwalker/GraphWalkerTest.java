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
package net.kebernet.xddl.graphwalker;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import net.kebernet.xddl.graphwalker.defaults.FieldsStrategy;
import org.junit.Test;

public class GraphWalkerTest {

  @Test
  public void simpleTest() {
    Child target = new Child();
    ArrayList<Object> visited = Lists.newArrayList();
    new GraphWalker(false)
        .withChildrenStrategy(o -> true, FieldsStrategy.create())
        .withVisitor(o -> true, visited::add)
        .walk(target);

    assertThat(visited)
        .isEqualTo(Lists.newArrayList(target, "parentFoo", "bar", "childFoo", "baz"));
  }

  @Test
  public void testNoCycles() {
    Child target = new Child();
    target.cycleRef = target;
    ArrayList<Object> visited = Lists.newArrayList();
    new GraphWalker(false)
        .withChildrenStrategy(o -> true, FieldsStrategy.create())
        .withVisitor(o -> true, visited::add)
        .walk(target);

    assertThat(visited)
        .isEqualTo(Lists.newArrayList(target, "parentFoo", "bar", "childFoo", "baz"));
  }

  @Test
  public void testVisitMultiple() {
    Child target = new Child();
    target.cycleRef = target;
    ArrayList<Object> visited = Lists.newArrayList();
    ArrayList<Object> visited2 = Lists.newArrayList();
    new GraphWalker(true)
        .withChildrenStrategy(o -> true, FieldsStrategy.create())
        .withVisitor(o -> true, visited::add)
        .withVisitor(o -> true, visited2::add)
        .walk(target);

    assertThat(visited)
        .isEqualTo(Lists.newArrayList(target, "parentFoo", "bar", "childFoo", "baz"));
    assertThat(visited2)
        .isEqualTo(Lists.newArrayList(target, "parentFoo", "bar", "childFoo", "baz"));
  }

  @Test
  public void testVisitExclusive() {
    Child target = new Child();
    target.cycleRef = target;
    ArrayList<Object> visited = Lists.newArrayList();
    ArrayList<Object> visited2 = Lists.newArrayList();
    new GraphWalker(false)
        .withChildrenStrategy(o -> true, FieldsStrategy.create())
        .withVisitor(o -> true, visited::add)
        .withVisitor(o -> true, visited2::add)
        .walk(target);

    assertThat(visited)
        .isEqualTo(Lists.newArrayList(target, "parentFoo", "bar", "childFoo", "baz"));
    assertThat(visited2).isEmpty();
  }
}
