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

import static net.kebernet.xddl.model.Utils.isNullOrEmpty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import net.kebernet.xddl.model.*;
import net.kebernet.xddl.ognl.OgnlTemplater;

@Builder
public class Loader {

  private static final ObjectMapper MAPPER;
  private File main;
  private List<File> includes;
  private List<File> patches;
  private File valsFile;
  private Map<String, Object> vals;
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
    Specification spec;
    spec = readWithoutEvaluate();
    new OgnlTemplater(spec, vals).run();

    return spec;
  }

  public Specification readWithoutEvaluate() {
    Specification spec;
    try {
      spec = MAPPER.readValue(main, Specification.class);

      scanDirectories("xddl", Utils.neverNull(this.includes), MAPPER, spec, false);
      if (scrubPatchesFromBaseline) {
        spec.getStructures().forEach(this::visitStructure);
        spec.setDeletions(null);
      }

      if (!isNullOrEmpty(patches)) {
        Specification patch = new Specification();
        scanDirectories("patch", patches, MAPPER, patch, true);
        scanDirectories("xddl", patches, MAPPER, patch, true);
        spec = Patcher.builder().specification(spec).patches(patch).build().apply();
      }
      if (vals == null) {
        vals = new HashMap<>();
      }
      if (valsFile != null && valsFile.exists()) {
        if (valsFile.isDirectory()) {
          throw new IllegalStateException(
              "valsFile " + valsFile.getAbsolutePath() + " cannot be a directory");
        }
        Map fromFile = mapper().readValue(valsFile, Map.class);
        //noinspection unchecked
        vals.putAll(fromFile);
      }

    } catch (IOException e) {
      throw new RuntimeException(main.getPath() + " " + e.getMessage(), e);
    }
    return spec;
  }

  @SuppressWarnings("WeakerAccess")
  void scanDirectories(
      String suffix,
      Collection<File> files,
      ObjectMapper mapper,
      Specification specification,
      boolean isPatch) {
    Utils.neverNull(files)
        .forEach(
            f -> {
              if (!f.isDirectory()) {
                throw new RuntimeException(f.getAbsolutePath() + " is not a directory");
              }
              File[] xddls =
                  f.listFiles(
                      scan -> scan.getName().toLowerCase().endsWith("." + suffix + ".json"));
              if (xddls != null) {
                Arrays.stream(xddls)
                    .forEach(xddl -> readFile(xddl, mapper, specification, isPatch));
              }
              File[] directories = f.listFiles(File::isDirectory);
              if (directories != null) {
                scanDirectories(suffix, Arrays.asList(directories), mapper, specification, isPatch);
              }
            });
  }

  private void readFile(
      File xddl, ObjectMapper mapper, Specification specification, boolean isPatch) {
    try {
      BaseType type = mapper.readValue(xddl, BaseType.class);
      if (type instanceof Structure) {
        Structure read = (Structure) type;
        read.setSourceFile(xddl);
        read.setPatch(isPatch);
        specification.structures().add(read);
      } else if (type instanceof Type) {
        Type read = (Type) type;
        read.setSourceFile(xddl);
        read.setPatch(isPatch);
        specification.types().add(read);
      } else if (type instanceof PatchDelete) {
        PatchDelete read = (PatchDelete) type;
        read.setSourceFile(xddl);
        read.setPatch(true);
        specification.deletions().add(read);
      }
    } catch (IOException e) {
      throw new RuntimeException(
          "Unable to parse " + xddl.getAbsolutePath() + "  " + e.getMessage(), e);
    }
  }

  private void visitStructure(Structure structure) {
    new ArrayList<>(Utils.neverNull(structure.getProperties()))
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
              p.ext().remove("migration");
            });
  }
}
