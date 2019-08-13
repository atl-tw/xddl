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

import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.PatchDelete;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;

@Builder
public class Patcher {

  Specification specification;
  Specification patches;

  public Specification apply() {
    Context ctx = new Context(Loader.mapper(), specification);
    Map<String, Structure> spec =
        specification.structures().stream().collect(Collectors.toMap(Structure::getName, s -> s));
    Map<String, Structure> patch =
        patches.structures().stream().collect(Collectors.toMap(Structure::getName, s -> s));
    patch
        .entrySet()
        .forEach(
            e -> {
              Structure original = spec.get(e.getKey());
              merge(ctx, original, e.getValue());
            });
    return specification;
  }

  private void merge(Context ctx, Structure original, Structure patch) {
    Map<String, BaseType> props =
        original.getProperties().stream().collect(Collectors.toMap(BaseType::getName, b -> b));
    patch
        .getProperties()
        .forEach(
            p -> {
              if (props.containsKey(p.getName())) {
                BaseType orig = props.get(p.getName());
                if (p instanceof PatchDelete) {
                  if (orig == null) {
                    throw ctx.stateException(
                        "A PATCH_DELETE was found for "
                            + p.getName()
                            + " but it isn't a property in the original",
                        original);
                  }
                  original.getProperties().remove(orig);
                } else if (orig instanceof net.kebernet.xddl.model.List
                    && p instanceof net.kebernet.xddl.model.List) {
                  net.kebernet.xddl.model.List o = (net.kebernet.xddl.model.List) orig;
                  net.kebernet.xddl.model.List pl = (net.kebernet.xddl.model.List) p;
                  if (o.getContains() instanceof Structure
                      && pl.getContains() instanceof Structure) {
                    merge(ctx, (Structure) o.getContains(), (Structure) pl.getContains());
                  } else if (o.getContains() instanceof Type && pl.getContains() instanceof Type) {
                    o.setContains(pl.getContains());
                  } else {
                    throw ctx.stateException("Unable to merge on list contains ", p);
                  }
                } else if (orig instanceof Structure && p instanceof Structure) {
                  merge(ctx, (Structure) orig, (Structure) p);
                } else {
                  int index = original.getProperties().indexOf(orig);
                  original.getProperties().remove(orig);
                  original.getProperties().add(index, p);
                }
              } else {
                original.getProperties().add(p);
              }
            });
  }
}
