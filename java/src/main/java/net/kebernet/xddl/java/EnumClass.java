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
package net.kebernet.xddl.java;

import static net.kebernet.xddl.model.Utils.ifNotNullOrEmpty;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.TypeSpec;
import java.io.Serializable;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;

public class EnumClass {
  private final Context ctx;
  private final BaseType base;
  private final Type resolved;

  public EnumClass(Context ctx, BaseType base, Type resolved) {
    this.ctx = ctx;
    this.base = base;
    this.resolved = resolved;
  }

  TypeSpec.Builder builder() {
    String name =
        base != null
            ? CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, base.getName())
            : CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, resolved.getName()) + "Type";
    TypeSpec.Builder builder = TypeSpec.enumBuilder(name).addSuperinterface(Serializable.class);
    resolved.getAllowable().forEach(v -> builder.addEnumConstant(v.getValue().asText()));
    if (base != null) {
      ifNotNullOrEmpty(base.getDescription(), s -> builder.addJavadoc(s + "\n"));
      ifNotNullOrEmpty(base.getComment(), s -> builder.addJavadoc("Comment: " + s + "\n"));
    } else {
      ifNotNullOrEmpty(resolved.getDescription(), s -> builder.addJavadoc(s + "\n"));
      ifNotNullOrEmpty(resolved.getComment(), s -> builder.addJavadoc("Comment: " + s + "\n"));
    }
    return builder;
  }
}
