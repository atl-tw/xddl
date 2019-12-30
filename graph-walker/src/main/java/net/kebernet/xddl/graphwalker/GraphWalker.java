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

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.concurrent.ThreadSafe;

@SuppressWarnings("unchecked")
@ThreadSafe
public class GraphWalker {

  private final List<Visitor> visitors = new CopyOnWriteArrayList<>();
  private final List<ChildrenStrategy> strategies = new CopyOnWriteArrayList<>();
  private final boolean visitMultipleTimes;
  private boolean bottomUp;

  public GraphWalker(boolean visitMultipleTimes) {
    this.visitMultipleTimes = visitMultipleTimes;
  }

  @SuppressWarnings("unused")
  public GraphWalker fromBottomUp() {
    this.bottomUp = true;
    return this;
  }

  public GraphWalker withVisitor(Predicate<?> accepts, Consumer<?> visit) {
    this.visitors.add(new Visitor(accepts, visit));
    return this;
  }

  public GraphWalker withChildrenStrategy(
      Predicate<?> accepts, Function<?, Map<String, ?>> readChildren) {
    //noinspection unchecked
    this.strategies.add(
        new ChildrenStrategy(accepts, (Function<Object, Map<String, ?>>) readChildren));
    return this;
  }

  public void walk(Object o) {
    Context context = new Context();
    this.walk(context, o);
  }

  private void walk(Context context, Object o) {
    if (o != null && !context.visited.contains(o)) {
      if (bottomUp) doChildren(context, o);
      visit(o);
      context.visited.add(o);
      if (!bottomUp) doChildren(context, o);
    }
  }

  private void doChildren(Context context, Object o) {
    this.strategies.stream()
        .filter(s -> s.accepts.test(o))
        .findFirst()
        .map(s -> (Map<String, Object>) s.children.apply(o))
        .ifPresent(children -> children.forEach((key, value) -> walk(context, value)));
  }

  private void visit(Object o) {
    Stream<Visitor> visitorStream = visitors.stream().filter(v -> v.accepts.test(o));
    if (visitMultipleTimes) {
      visitorStream.forEach(v -> v.visit.accept(o));
    } else {
      visitorStream.findFirst().ifPresent(v -> v.visit.accept(o));
    }
  }

  private static class Context {
    Set<Object> visited = Sets.newIdentityHashSet();
  }

  private static class ChildrenStrategy {
    private final Predicate<Object> accepts;
    private final Function<Object, Map<String, ?>> children;

    @SuppressWarnings("unchecked")
    private ChildrenStrategy(Predicate<?> accepts, Function<Object, Map<String, ?>> children) {
      this.accepts = (Predicate<Object>) accepts;
      this.children = children;
    }
  }

  private static class Visitor {
    private final Predicate<Object> accepts;
    private final Consumer<Object> visit;

    @SuppressWarnings("unchecked")
    private Visitor(Predicate<?> accepts, Consumer<?> visit) {
      this.accepts = (Predicate<Object>) accepts;
      this.visit = (Consumer<Object>) visit;
    }
  }
}
