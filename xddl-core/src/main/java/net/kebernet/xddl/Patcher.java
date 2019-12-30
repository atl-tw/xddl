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
package net.kebernet.xddl;

import static java.lang.Boolean.TRUE;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import net.kebernet.xddl.model.*;
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
    patch.forEach(
        (key, value) -> {
          Structure original = spec.get(key);
          if (original == null && value != null && !TRUE.equals(value.getPatch()))
            // trying to create something that exists.
            throw ctx.stateException("Attempt to patch a Structure not found in original", value);
          else if (original == null && value != null)
            // value is patch, and it is a whole new thing
            specification.structures().add(value);
          else if (original != null)
            // just do the patch
            merge(ctx, original, value);
        });
    Set<String> namesToDelete =
        patches.deletions().stream().map(d -> d.getName()).collect(Collectors.toSet());
    specification.setStructures(
        specification.structures().stream()
            .filter(s -> !namesToDelete.contains(s.getName()))
            .collect(Collectors.toList()));
    specification.setTypes(
        specification.getTypes().stream()
            .filter(t -> !namesToDelete.contains(t.getName()))
            .collect(Collectors.toList()));
    return specification;
  }

  private void merge(Context ctx, Structure original, Structure patch) {
    if (Utils.neverNull(original.getProperties()).isEmpty())
      throw ctx.stateException("Structure has no properties", original);
    List<BaseType> missingNames =
        original.getProperties().stream()
            .filter(p -> p.getName() == null)
            .collect(Collectors.toList());
    if (!missingNames.isEmpty())
      throw ctx.stateException("Missing 'name' properties", missingNames);
    Map<String, BaseType> props =
        Utils.neverNull(original.getProperties()).stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(BaseType::getName, b -> b));
    original.ext().putAll(patch.ext());
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
                }
                if (orig instanceof PatchDelete) {
                  // Remove patch deletes from original.
                  original.getProperties().remove(orig);
                }
                if (orig instanceof net.kebernet.xddl.model.List
                    && p instanceof net.kebernet.xddl.model.List) {
                  net.kebernet.xddl.model.List o = (net.kebernet.xddl.model.List) orig;
                  net.kebernet.xddl.model.List pl = (net.kebernet.xddl.model.List) p;
                  if (o.getContains() instanceof Structure
                      && pl.getContains() instanceof Structure) {
                    merge(ctx, (Structure) o.getContains(), (Structure) pl.getContains());
                  } else if (o.getContains() instanceof Type && pl.getContains() instanceof Type) {
                    o.setContains(pl.getContains());
                  } else //noinspection StatementWithEmptyBody
                  if (o.getContains() instanceof Reference) {
                    // no op
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
