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
package net.kebernet.xddl.markdown;

import static java.util.Optional.ofNullable;
import static net.kebernet.xddl.model.ModelUtil.*;
import static net.kebernet.xddl.model.Utils.ifNotNullOrEmpty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import net.kebernet.xddl.model.List;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;
import net.kebernet.xddl.plugins.Plugin;

public class MarkdownPlugin implements Plugin {
  private Context context;

  @Override
  public String getName() {
    return "markdown";
  }

  @Override
  public String generateArtifacts(Context context, File outputDirectory) throws IOException {
    this.context = context;
    String filename =
        ofNullable(System.getProperty("markdown.filename"))
                .orElseGet(() -> ofNullable(context.getSpecification().getTitle()).orElse("schema"))
            + ".md";

    File outputFile = new File(outputDirectory, filename);
    try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile, false))) {
      generateStructures(pw, context.getSpecification());
      pw.println();
      pw.println();
      generateTypes(pw, context.getSpecification());
      pw.println();
      pw.println();
      generateStructureDetails(pw, context.getSpecification());
      pw.println();
      pw.println();
      generateTypeDetails(pw, context.getSpecification());
      pw.println();
    }

    return null;
  }

  private void generateTypeDetails(PrintWriter pw, Specification specification) {}

  private void generateStructureDetails(PrintWriter pw, Specification specification) {}

  private void generateTypes(PrintWriter pw, Specification specification) {}

  private void generateStructures(PrintWriter pw, Specification specification) {
    pw.println("Structures");
    pw.println("----------");
    pw.println();
    specification
        .structures()
        .forEach(
            s -> {
              pw.print("### ");
              pw.print(s.getName());
              pw.println();
              pw.println(s.getDescription());
              pw.println();
              ifNotNullOrEmpty(s.getComment(), c -> pw.println("\n```" + c + "```"));
              pw.println("#### Properties");
              pw.println();
              s.getProperties()
                  .forEach(
                      p -> {
                        isaType(p, (t) -> generateType(pw, 0, t));
                        isaReference(p, (r) -> generateReference(pw, 0, r));
                        isaStructure(p, (sub) -> generateStructure(pw, 0, sub));
                      });
            });
  }

  private void generateStructure(PrintWriter pw, int i, Structure s) {}

  private void generateReference(PrintWriter pw, int indentationLevel, Reference ref) {
    int indent = indentationLevel + 1;
    int subindent = indent + 1;
    context
        .resolveReference(ref)
        .ifPresent(
            type -> {
              String typeLabel =
                  type instanceof Structure
                      ? "Structure"
                      : type instanceof List ? "List" : ((Type) type).getCore().toString();
              pw.println(
                  bullet(indent)
                      + "["
                      + ref.getName()
                      + " ("
                      + ref.getRef()
                      + ")](#"
                      + ref.getRef()
                      + ")");
              ifNotNullOrEmpty(ref.getDescription(), s -> pw.println(bullet(subindent) + s));
              ifNotNullOrEmpty(
                  ref.getComment(), s -> pw.println(bullet(subindent) + "```" + s + "```"));
            });
  }

  private void generateType(PrintWriter pw, int indentationLevel, Type type) {
    int indent = indentationLevel + 1;
    pw.println(bullet(indent) + type.getName() + " (" + type.getCore() + ")");
    int subindent = indent + 1;
    ifNotNullOrEmpty(type.getDescription(), s -> pw.println(bullet(subindent) + s));
    ifNotNullOrEmpty(type.getComment(), s -> pw.println(bullet(subindent) + "```" + s + "```"));
    ifNotNullOrEmpty(
        type.getExamples(),
        ex -> {
          pw.println(bullet(subindent) + "Examples:");
          ex.forEach(
              v ->
                  pw.println(
                      bullet(subindent) + code(subindent + 2, writeValueAsString(v.getValue()))));
        });
    ifNotNullOrEmpty(
        type.getExt(),
        ext -> {
          pw.println(bullet(subindent) + "Extensions:");
          pw.println("```json");
          pw.println(writeValueAsString(ext));
          pw.println("```");
        });
  }

  private String code(int indent, String value) {
    return indent(indent) + "'''\n" + indent(indent, value) + "\n" + indent(indent) + "'''\n";
  }

  private String writeValueAsString(Object value) {
    try {
      return context.getMapper().writeValueAsString(value);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private String bullet(int indent) {
    return indent(indent) + "* ";
  }

  private String indent(int indentationLevel) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < indentationLevel; i++) {
      sb.append("  ");
    }
    return sb.toString();
  }

  private String indent(int indetationLevel, String value) {
    return Joiner.on("\n" + indent(indetationLevel)).join(Splitter.on('\n').split(value));
  }
}
