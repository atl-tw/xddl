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

import static net.kebernet.xddl.model.Utils.isNullOrEmpty;
import static net.kebernet.xddl.model.Utils.neverNull;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.PatchDelete;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;

@Builder
public class Loader {

  private static final ObjectMapper MAPPER;
  private File main;
  private List<File> includes;
  private List<File> patches;
  private boolean scrubPatchesFromBaseline;

  static {
    MAPPER = new ObjectMapper();
    MAPPER.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    MAPPER.enable(SerializationFeature.INDENT_OUTPUT);
  }

  public static ObjectMapper mapper() {
    return MAPPER;
  }

  public Specification read() {
    Specification spec = null;
    try {
      spec = MAPPER.readValue(main, Specification.class);
    } catch (IOException e) {
      throw new RuntimeException(main.getPath() + " " + e.getMessage(), e);
    }
    scanDirectories("xddl", neverNull(this.includes), MAPPER, spec);
    if (scrubPatchesFromBaseline) {
      spec.getStructures().forEach(this::visitStructure);
    }

    if (!isNullOrEmpty(patches)) {
      Specification patch = new Specification();
      scanDirectories("patch", patches, MAPPER, patch);
      spec = Patcher.builder().specification(spec).patches(patch).build().apply();
    }

    return spec;
  }

  void scanDirectories(
      String suffix, Collection<File> files, ObjectMapper mapper, Specification specification) {
    neverNull(files)
        .forEach(
            f -> {
              if (!f.isDirectory()) {
                throw new RuntimeException(f.getAbsolutePath() + " is not a directory");
              }
              File[] xddls =
                  f.listFiles(
                      scan -> scan.getName().toLowerCase().endsWith("." + suffix + ".json"));
              if (xddls != null) {
                Arrays.stream(xddls).forEach(xddl -> readFile(xddl, mapper, specification));
              }
              File[] directories = f.listFiles(File::isDirectory);
              if (directories != null) {
                scanDirectories(suffix, Arrays.asList(directories), mapper, specification);
              }
            });
  }

  private void readFile(File xddl, ObjectMapper mapper, Specification specification) {
    try {
      BaseType type = mapper.readValue(xddl, BaseType.class);
      if (type instanceof Structure) {
        specification.structures().add((Structure) type);
      } else if (type instanceof Type) {
        specification.types().add((Type) type);
      }
    } catch (IOException e) {
      throw new RuntimeException(
          "Unable to parse " + xddl.getAbsolutePath() + "  " + e.getMessage(), e);
    }
  }

  private void visitStructure(Structure structure) {
    new ArrayList<>(neverNull(structure.getProperties()))
        .forEach(
            p -> {
              if (p instanceof PatchDelete) {
                structure.getProperties().remove(p);
              } else if (p instanceof net.kebernet.xddl.model.List) {
                net.kebernet.xddl.model.List list = (net.kebernet.xddl.model.List) p;
                if (list.getContains() instanceof Structure) {
                  visitStructure((Structure) list.getContains());
                }
              } else if (p instanceof Structure) {
                visitStructure((Structure) p);
              }
            });
  }
}
