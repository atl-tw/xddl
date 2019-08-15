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
import static net.kebernet.xddl.model.Utils.asIterable;
import static net.kebernet.xddl.model.Utils.ifNotNullOrEmpty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import net.kebernet.xddl.model.List;
import net.kebernet.xddl.model.PatchDelete;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;
import net.kebernet.xddl.plugins.Plugin;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

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
            .orElseGet(
                () ->
                    ofNullable(context.getSpecification().getTitle())
                        .orElse("schema")
                        .replaceAll(" ", "_")
                        .toLowerCase());

    File outputFile = new File(outputDirectory, filename + ".md");
    try (PrintWriter pw = new PrintWriter(new FileWriter(outputFile, false))) {
      Specification spec = context.getSpecification();
      pw.println(ofNullable(spec.getTitle()).orElse("Untitled Specification"));
      pw.println("===========================================================");
      ofNullable(spec.getVersion()).ifPresent(v -> pw.println("_Version: " + v + "_"));
      pw.println();
      ofNullable(spec.getDescription())
          .ifPresent(
              s -> {
                pw.println(s);
                pw.println();
              });
      ofNullable(spec.getComment())
          .ifPresent(
              s -> {
                pw.println("```" + s + "```");
                pw.println();
              });
      pw.println();
      generateTOC(pw, spec);
      pw.println();
      pw.println();
      generateStructures(pw, spec);
      pw.println();
      pw.println();
      generateTypes(pw, spec);
      pw.println();
      ifNotNullOrEmpty(
          spec.getExt(),
          ext -> {
            pw.println("<a name=\"spec-extensions\"></a>");
            pw.println("Extensions");
            pw.println("----------");
            pw.println();
            ext.forEach(
                (key, value) -> {
                  pw.println(bullet(2) + key);
                  printJson(pw, 3, value);
                });
          });
    }

    Parser parser = Parser.builder().build();
    try (Reader reader = new FileReader(outputFile);
        FileWriter writer = new FileWriter(new File(outputDirectory, filename + ".html"))) {
      writer.append("<html><head></head><body>");
      Node document = parser.parseReader(reader);
      HtmlRenderer renderer = HtmlRenderer.builder().build();
      renderer.render(document, writer);
      writer.append("</body></html>");
    }
    return null;
  }

  private void generateTOC(PrintWriter pw, Specification spec) {
    pw.println("Contents");
    pw.println("--------");
    pw.println();
    pw.println("1. Structures");
    spec.structures().forEach(s -> pw.println("   1. [" + s.getName() + "](#" + s.getName() + ")"));
    pw.println("1. Types");
    spec.getTypes().forEach(s -> pw.println("   1. [" + s.getName() + "](#" + s.getName() + ")"));
    ifNotNullOrEmpty(
        spec.getExt(),
        e -> {
          pw.println("1. [Extensions](#spec-extensions)");
        });
  }

  private void generateTypes(PrintWriter pw, Specification specification) {
    pw.println("Types");
    pw.println("-----");
    pw.println();
    specification.types().forEach(t -> generateType(pw, 0, t, true));
  }

  private void generateStructures(PrintWriter pw, Specification specification) {
    pw.println("Structures");
    pw.println("----------");
    pw.println();
    specification
        .structures()
        .forEach(
            s -> {
              pw.println("<a name=\"" + s.getName() + "\"></a>");
              pw.print("### ");
              pw.print(s.getName());
              pw.println();
              ofNullable(s.getDescription())
                  .ifPresent(
                      d -> {
                        pw.println(d);
                        pw.println();
                      });

              ifNotNullOrEmpty(s.getComment(), c -> pw.println("\n```" + c + "```"));
              pw.println("#### Properties");
              pw.println();
              s.getProperties().stream()
                  .filter(t -> !(t instanceof PatchDelete))
                  .forEach(
                      p -> {
                        isaType(p, (t) -> generateType(pw, 0, t, false));
                        isaReference(p, (r) -> generateReferenceProperty(pw, 0, r));
                        isaStructure(p, (sub) -> generateStructureProperty(pw, 0, sub));
                        isaList(p, (list) -> generateListProperty(pw, 0, list));
                      });
            });
  }

  private void generateListProperty(PrintWriter pw, int indentationLevel, List list) {
    int indent = indentationLevel + 1;
    int subindent = indent + 1;
    if (list.getContains() instanceof Reference) {
      Reference ref = (Reference) list.getContains();
      pw.println(
          bullet(indent)
              + list.getName()
              + " (List of ["
              + ref.getRef()
              + "](#"
              + ref.getRef()
              + "))");
      ifNotNullOrEmpty(
          ref.getDescription(), s -> pw.println(bullet(subindent) + replaceLineBreaks(s)));
      ifNotNullOrEmpty(
          ref.getComment(),
          s -> pw.println(bullet(subindent) + "```" + replaceLineBreaks(s) + "```"));
    } else {
      pw.println(bullet(indent) + list.getName() + " (List of...)");
    }
    ifNotNullOrEmpty(
        list.getDescription(), s -> pw.println(bullet(indentationLevel) + replaceLineBreaks(s)));
    ifNotNullOrEmpty(
        list.getComment(), s -> pw.println(bullet(indent) + "```" + replaceLineBreaks(s) + "```"));
    isaType(list.getContains(), (t) -> generateType(pw, indent, t, false));
    isaStructure(list.getContains(), (sub) -> generateStructureProperty(pw, indent, sub));
    isaList(list.getContains(), (l) -> generateListProperty(pw, indent, l));
  }

  private void generateStructureProperty(PrintWriter pw, int indentationLevel, Structure struct) {
    int indent = indentationLevel + 1;
    pw.println(bullet(indent) + struct.getName() + " (Structure)");
    int subindent = indent + 1;
    ifNotNullOrEmpty(
        struct.getDescription(), s -> pw.println(bullet(subindent) + replaceLineBreaks(s)));
    ifNotNullOrEmpty(struct.getComment(), s -> pw.println(bullet(subindent) + "``\n" + s + "``"));
    pw.println(bullet(subindent) + " properties");
    struct.getProperties().stream()
        .filter(t -> !(t instanceof PatchDelete))
        .forEach(
            p -> {
              isaType(p, (t) -> generateType(pw, subindent + 1, t, false));
              isaReference(p, (r) -> generateReferenceProperty(pw, subindent + 1, r));
              isaStructure(p, (sub) -> generateStructureProperty(pw, subindent + 1, sub));
              isaList(p, (list) -> generateListProperty(pw, subindent + 1, list));
            });
  }

  private void generateReferenceProperty(PrintWriter pw, int indentationLevel, Reference ref) {
    int indent = indentationLevel + 1;
    int subindent = indent + 1;
    context
        .resolveReference(ref)
        .ifPresent(
            type -> {
              pw.println(
                  bullet(indent)
                      + ref.getName()
                      + " ("
                      + "["
                      + ref.getRef()
                      + "](#"
                      + ref.getRef()
                      + "))");
              ifNotNullOrEmpty(
                  ref.getDescription(), s -> pw.println(bullet(subindent) + replaceLineBreaks(s)));
              ifNotNullOrEmpty(
                  ref.getComment(),
                  s -> pw.println(bullet(subindent) + "```" + replaceLineBreaks(s) + "```"));
            });
  }

  private String replaceLineBreaks(String text) {
    return Joiner.on("<br>").join(Splitter.on("\n").split(text));
  }

  private void generateType(PrintWriter pw, int indentationLevel, Type type, boolean anchor) {
    int indent = indentationLevel;
    if (anchor) {
      pw.println("<a name=\"" + type.getName() + "\"></a>");
      pw.println("#### " + type.getName() + " (" + type.getCore() + ")");
    } else {
      indent++;
      pw.println(bullet(indent) + type.getName() + " (" + type.getCore() + ")");
    }
    int subindent = indent + 1;
    ifNotNullOrEmpty(
        type.getDescription(), s -> pw.println(bullet(subindent) + replaceLineBreaks(s)));
    ifNotNullOrEmpty(type.getComment(), s -> pw.println(bullet(subindent) + "``" + s + "``"));
    ifNotNullOrEmpty(
        type.getAllowable(),
        ex -> {
          pw.println(bullet(subindent) + "allowable values");
          ex.forEach(
              v ->
                  pw.println(
                      bullet(subindent + 1)
                          + "``"
                          + writeValueAsString(v.getValue())
                          + "``"
                          + ofNullable(v.getDescription()).map(s -> " " + s).orElse("")));
        });
    ifNotNullOrEmpty(
        type.getExamples(),
        ex -> {
          pw.println(bullet(subindent) + "examples");
          ex.forEach(
              v ->
                  pw.println(
                      bullet(subindent) + code(subindent + 1, writeValueAsString(v.getValue()))));
        });
    ifNotNullOrEmpty(
        type.getExt(),
        ext -> {
          pw.println(bullet(subindent) + "extensions");
          ext.forEach(
              (key, value) -> {
                pw.println(bullet(subindent + 1) + key);
                printJson(pw, subindent + 2, value);
              });
        });
  }

  private void printJson(PrintWriter pw, int indent, JsonNode value) {
    if (value.isArray()) {
      for (JsonNode node : asIterable(value.elements())) {
        printJson(pw, indent, node);
      }
    } else if (value.isObject()) {
      for (String field : asIterable(value.fieldNames())) {
        JsonNode fieldVal = value.get(field);
        pw.print(bullet(indent) + "``" + field);
        if (!fieldVal.isObject() && !fieldVal.isArray()) {
          pw.println(": " + writeValueAsString(fieldVal) + "``");
        } else {
          pw.println("``");
          printJson(pw, indent + 1, fieldVal);
        }
      }
    }
  }

  private String code(int indent, String value) {
    return indent(indent) + "''\n" + value + "''\n";
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
