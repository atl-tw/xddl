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
package net.kebernet.xddl.plantuml;

import static net.kebernet.xddl.model.Utils.*;

import com.google.common.base.CaseFormat;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.*;
import net.kebernet.xddl.model.*;
import net.kebernet.xddl.plugins.Context;
import net.kebernet.xddl.plugins.Plugin;

public class PlantUmlPlugin implements Plugin {

  public static final String LINE = " -- ";
  public static final String AGGREGATION = " o-- ";

  @Override
  public String getName() {
    return "plantuml";
  }

  @Override
  public String generateArtifacts(Context context, File outputDirectory) throws IOException {

    Specification specification = context.getSpecification();
    StringBuilder output = new StringBuilder("@startuml").append('\n').append('\n');

    specification
        .structures()
        .forEach(
            s -> {
              doStructure(s, context, output);
            });

    output.append("@enduml");

    File outputFile = new File(outputDirectory, context.createBaseFilename() + ".puml");
    if (!outputFile.getParentFile().exists() && !outputFile.getParentFile().mkdirs()) {
      throw new IOException("Unable to create " + outputFile.getParentFile().getAbsolutePath());
    }
    try (OutputStreamWriter writer =
        new OutputStreamWriter(new FileOutputStream(outputFile), Charsets.UTF_8)) {
      CharStreams.copy(new StringReader(output.toString()), writer);
    }

    return "OK";
  }

  private void doStructure(Structure structure, Context context, StringBuilder output) {
    StringBuilder clazz = new StringBuilder("class ").append(structure.getName()).append(" {\n");
    neverNull(structure.getProperties())
        .forEach(t -> this.doProperty(structure, t, context, clazz, output));
    clazz.append("\n}\n");
    output.append(clazz.toString());
  }

  private void doProperty(
      Structure structure, BaseType t, Context context, StringBuilder clazz, StringBuilder output) {

    String relationship = LINE;
    String ordinality = " \"1\" ";
    boolean showOrdinality = unboxOrFalse(t.getRequired());
    String typeLabel = "";
    boolean addRelationship = false;
    BaseType<?> parameterType = t;

    if (t instanceof List) {
      List list = (List) t;
      parameterType = list.getContains();
      ordinality = " \"0..n\" ";
      showOrdinality = true;
      relationship = AGGREGATION;
    }

    if (parameterType instanceof Type) {
      Type type = (Type) parameterType;
      if (!isNullOrEmpty(type.getAllowable())) {
        typeLabel = doEnum(type, context, output);
        addRelationship = true;
      } else {
        typeLabel = type.getCore().toString();
      }
    } else if (parameterType instanceof Reference) {
      Reference ref = (Reference) parameterType;
      typeLabel = ref.getRef();
      addRelationship = context.pointsToStructure(ref);
    }

    if (addRelationship) {
      output
          .append('\n')
          .append(structure.getName())
          .append(relationship)
          .append(showOrdinality ? ordinality : "")
          .append(typeLabel)
          .append("\n\n");
    }

    if (t instanceof List) {
      typeLabel = "List<" + typeLabel + ">";
    }

    clazz.append('\t').append(t.getName()).append(" : ").append(typeLabel).append('\n');
  }

  private String doEnum(Type type, Context context, StringBuilder output) {
    String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, type.getName() + "Type");
    StringBuilder clazz = new StringBuilder("enum ").append(name).append(" {").append('\n');
    type.getAllowable().forEach(v -> clazz.append('\t').append(v.getValue().asText()).append('\n'));
    clazz.append("}\n");
    output.append(clazz.toString());
    return name;
  }
}
