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
package net.kebernet.xddl.diff;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.List;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.plugins.Context;

public class SpecificationDiff {

  private final ObjectMapper mapper;
  private final Specification left;
  private final Specification right;
  private final HashMap<String, SchemaElement> leftActual = new HashMap<>();
  private final HashMap<String, SchemaElement> rightActual = new HashMap<>();

  public SpecificationDiff(ObjectMapper mapper, Specification left, Specification right) {
    this.mapper = mapper;
    this.left = left;
    this.right = right;
  }

  public Set<SchemaElement> diff() {
    LinkedHashSet<SchemaElement> leftSet = new LinkedHashSet<>();
    LinkedHashSet<SchemaElement> rightSet = new LinkedHashSet<>();
    Context ctx = new Context(mapper, left);
    this.process(leftSet, ctx);
    ctx = new Context(mapper, right);
    this.process(rightSet, ctx);

    leftSet.forEach(e -> leftActual.put(e.pathToDotNotation(), e));
    rightSet.forEach(e -> rightActual.put(e.pathToDotNotation(), e));
    return Sets.difference(leftSet, rightSet);
  }

  public SchemaElement left(String pathElement) {
    return leftActual.get(pathElement);
  }

  public SchemaElement right(String pathElement) {
    return rightActual.get(pathElement);
  }

  private void process(Set<SchemaElement> set, Context ctx) {
    Specification specification = ctx.getSpecification();
    Deque<String> pathTree = new LinkedList<>();
    if (specification.getEntryRef() != null) {
      Reference ref = new Reference();
      ref.setRef(specification.getEntryRef());
      visit(
          ctx,
          set,
          pathTree,
          (Structure)
              ctx.resolveReference(ref)
                  .orElseThrow(() -> ctx.stateException("Can't resolve reference", ref)));
    } else {
      ctx.getSpecification().structures().forEach(s -> visit(ctx, set, pathTree, s));
    }
  }

  private void visit(
      Context ctx, Set<SchemaElement> set, Deque<String> pathTree, Structure structure) {
    structure
        .getProperties()
        .forEach(
            p -> {
              pathTree.push(p.getName());
              BaseType type = ctx.resolve(p);
              if (type instanceof List) {
                type = ctx.resolve(((List) type).getContains());
              }
              set.add(new SchemaElement(pathTree, type));
              if (type instanceof Structure) {
                visit(ctx, set, pathTree, (Structure) type);
              }
            });
  }
}
