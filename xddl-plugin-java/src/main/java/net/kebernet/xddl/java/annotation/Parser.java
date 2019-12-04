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
package net.kebernet.xddl.java.annotation;

import static java.util.Optional.ofNullable;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.google.common.base.Joiner;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import net.kebernet.xddl.java.Resolver;
import net.kebernet.xddl.plugins.Context;

/**
 * This class uses com.github.javaparser to synthesize a Java class with a single field containing a
 * given list on annotations so that it can construct AnnotationSpec.Builders that are correctly
 * populated.
 *
 * <p>It then recurses the tree and replaces Type reference with the types from the list of imported
 * names where appropriate.
 */
public class Parser {

  private final Context context;

  public Parser(Context context) {
    this.context = context;
  }

  public List<AnnotationSpec.Builder> parse(List<String> imports, String annotations) {
    List<AnnotationSpec.Builder> builders = new ArrayList<>();
    Map<String, ClassName> replace =
        ofNullable(imports).orElse(Collections.emptyList()).stream()
            .map(s -> (ClassName) Resolver.parse(s, ""))
            .collect(Collectors.toMap(ClassName::simpleName, c -> c));
    String importBlock =
        Joiner.on('\n')
            .join(
                ofNullable(imports).orElse(Collections.emptyList()).stream()
                    .map(c -> "import " + c + ";")
                    .collect(Collectors.toList()));
    String clazz =
        importBlock
            + "\nclass Temp {"
            + '\n'
            + annotations
            + '\n'
            + " private Object field;"
            + "\n}";
    CompilationUnit compilationUnit = StaticJavaParser.parse(clazz);
    Optional<ClassOrInterfaceDeclaration> classOpt = compilationUnit.getClassByName("Temp");
    ClassOrInterfaceDeclaration temp =
        classOpt.orElseThrow(
            () -> new IllegalArgumentException("Unable to parse annotations " + annotations));
    List<AnnotationExpr> nodes =
        temp.findAll(FieldDeclaration.class).stream()
            .map(BodyDeclaration::getAnnotations)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

    nodes.forEach(
        n -> {
          final AnnotationSpec.Builder builder =
              (replace.containsKey(n.getName().getIdentifier()))
                  ? AnnotationSpec.builder(replace.get(n.getName().getIdentifier()))
                  : AnnotationSpec.builder((ClassName) Resolver.parse(n.getName().asString(), ""));
          List<ClassName> replacements = new ArrayList<>();
          if (n instanceof SingleMemberAnnotationExpr) {
            this.resolveNode(replace, replacements, n);
            builder.addMember(
                "value", CodeBlock.builder().add(n.toString(), replacements.toArray()).build());
          } else {
            n.getChildNodes().stream()
                .filter(c -> !(c instanceof Name))
                .forEach(
                    c -> {
                      this.resolveNode(replace, replacements, c);
                      builder.addMember(
                          "value",
                          CodeBlock.builder().add(n.toString(), replacements.toArray()).build());
                    });
          }
          builders.add(builder);
        });
    return builders;
  }

  /**
   * This takes a list of imports from SimpleNames to ClassNames and replaces the reference with $T,
   * appending the replacement class name to replacements
   *
   * @param replace The lookup of imported simple names
   * @param replacements A list of replacements
   * @param value the node to search for replacement
   * @return true if something was altered.
   */
  private boolean resolveNode(
      Map<String, ClassName> replace, List<ClassName> replacements, Node value) {
    @SuppressWarnings("unused")
    final AtomicBoolean didSomething = new AtomicBoolean(false);
    if (value instanceof MemberValuePair) {
      MemberValuePair pair = (MemberValuePair) value;
      didSomething.compareAndSet(false, resolveNode(replace, replacements, pair.getValue()));
    } else if (value instanceof FieldAccessExpr) {
      FieldAccessExpr field = (FieldAccessExpr) value;
      didSomething.compareAndSet(false, resolveFieldAccess(replace, replacements, field));
    } else if (value instanceof ArrayInitializerExpr) {
      ArrayInitializerExpr array = (ArrayInitializerExpr) value;
      array
          .getValues()
          .forEach(n -> didSomething.compareAndSet(false, resolveNode(replace, replacements, n)));
    } else if (value instanceof AnnotationExpr) {
      value
          .getChildNodes()
          .forEach(
              c -> didSomething.compareAndSet(false, this.resolveNode(replace, replacements, c)));
    } else if (value instanceof Name) {
      Name name = (Name) value;
      name.getQualifier()
          .ifPresent(q -> didSomething.compareAndSet(false, resolveNode(replace, replacements, q)));
      if (replace.containsKey(name.getIdentifier())) {
        replacements.add(replace.get(name.getIdentifier()));
        name.setIdentifier("$T");
        didSomething.compareAndSet(false, true);
      }
    } else if (value instanceof ClassExpr) {
      ClassExpr expr = (ClassExpr) value;
      didSomething.compareAndSet(
          false, resolveTypeRef(replace, replacements, expr.getType().asClassOrInterfaceType()));
    }
    return didSomething.get();
  }

  private boolean resolveTypeRef(
      Map<String, ClassName> replace, List<ClassName> replacements, ClassOrInterfaceType type) {
    boolean didSomething = false;
    if (type != null && replace.containsKey(type.getName().getIdentifier())) {
      replacements.add(replace.get(type.getName().getIdentifier()));
      type.getName().setIdentifier("$T");
      didSomething = true;
    }
    return didSomething;
  }

  private boolean resolveFieldAccess(
      Map<String, ClassName> replace, List<ClassName> replacements, FieldAccessExpr value) {
    boolean didSomething = false;
    if (value.getScope() instanceof FieldAccessExpr) {
      didSomething = resolveFieldAccess(replace, replacements, (FieldAccessExpr) value.getScope());
    } else if (value.getScope() instanceof NameExpr) {
      NameExpr name = (NameExpr) value.getScope();
      if (replace.containsKey(name.getName().getIdentifier())) {
        replacements.add(replace.get(name.getName().getIdentifier()));
        name.getName().setIdentifier("$T");
        didSomething = true;
      }
    }
    return didSomething;
  }
}
