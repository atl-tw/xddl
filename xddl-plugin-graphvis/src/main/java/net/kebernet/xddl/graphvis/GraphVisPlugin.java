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
package net.kebernet.xddl.graphvis;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.List;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.plugins.Context;
import net.kebernet.xddl.plugins.Plugin;

public class GraphVisPlugin implements Plugin {
  @Override
  public String getName() {
    return "graphvis";
  }

  @SuppressWarnings("unchecked")
  @Override
  public String generateArtifacts(Context context, File outputDirectory) throws IOException {
    String filename =
        ofNullable(System.getProperty("graphvis.filename"))
            .orElseGet(
                () ->
                    ofNullable(context.getSpecification().getTitle())
                        .orElse("schema")
                        .replaceAll(" ", "_")
                        .toLowerCase());

    File outputFile = new File(outputDirectory, filename + ".dot");
    if (!outputDirectory.exists()) {
      if (!outputDirectory.mkdirs()) {
        throw new IOException("Unable to create " + outputDirectory.getAbsolutePath());
      }
    }
    try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile, false))) {
      Specification spec = context.getSpecification();
      BaseType type = context.getReferences().get(spec.getEntryRef());
      if (!(type instanceof Structure)) {
        throw context.stateException("Specification entry ref is not a structure.", null);
      }
      pw.println("digraph " + filename + "{ ");
      context.getReferences().values().stream()
          .filter(e -> e instanceof Structure)
          .filter(e -> e.ext().get("graphvis") != null)
          .forEach(
              e ->
                  pw.println(
                      e.getName() + "[" + ((JsonNode) e.ext().get("graphvis")).asText() + "]"));

      Queue<Structure> toDo = new LinkedList<>();
      HashSet<Structure> visited = new HashSet<>();
      toDo.offer((Structure) type);

      do {
        Structure s = toDo.remove();
        s.getProperties().stream()
            .map(
                p -> {
                  BaseType result;
                  if (p instanceof List) {
                    result = ((List) p).getContains();
                    result
                        .ext()
                        .put("_gv_tmp", context.getMapper().valueToTree("[arrowhead=\"crow\"]"));
                  } else {
                    result = p;
                  }
                  return result;
                })
            .map(
                p ->
                    p instanceof Reference
                        ? context
                            .resolveReference((Reference) p)
                            .map(
                                r -> {
                                  r.setName(((Reference) p).getRef());
                                  return r;
                                })
                            .orElse(null)
                        : p)
            .filter(p -> p instanceof Structure)
            .map(p -> (Structure) p)
            .peek(
                p -> {
                  String opts = ofNullable(p.ext().get("_gv_tmp")).map(JsonNode::asText).orElse("");
                  p.ext().remove("_gv_tmp");
                  pw.println(s.getName() + " -> " + p.getName() + " " + opts + ";");
                })
            .filter(p -> !visited.contains(p))
            .peek(visited::add)
            .forEach(toDo::offer);

      } while (!toDo.isEmpty());
      pw.println("}");
    }
    Graphviz.fromFile(outputFile)
        .render(Format.PNG)
        .toFile(new File(outputDirectory, filename + ".png"));
    return outputFile.getAbsolutePath();
  }

  @SuppressWarnings("SameParameterValue")
  private String writeValueAsString(Context context, String s) {
    try {
      return context.getMapper().writeValueAsString(s);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }
}
