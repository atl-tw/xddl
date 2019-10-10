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

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import net.kebernet.xddl.model.CoreType;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;
import org.junit.Test;

public class ResolverTest {

  @Test
  public void parseSimpleTypeName() {
    ClassName name = (ClassName) Resolver.parse("com.foo.Bar", "java.lang");
    assertEquals("com.foo", name.packageName());
    assertEquals("Bar", name.simpleName());
  }

  @Test
  public void parseNestedTypeNames() {
    ClassName name = (ClassName) Resolver.parse("com.foo.Bar.Baz", "java.lang");
    assertEquals("com.foo", name.packageName());
    assertEquals("Baz", name.simpleName());
    assertThat(name.simpleNames()).containsExactly("Bar", "Baz");
  }

  @Test
  public void integerTypes() {
    Type type = new Type();
    type.setCore(CoreType.INTEGER);
    Context context = new Context(new ObjectMapper(), new Specification());
    TypeName resolved = Resolver.resolveType(context, type);
    assertThat(resolved).isInstanceOf(ClassName.class);
    ClassName className = (ClassName) resolved;
    assertThat(className.packageName()).isEqualTo("java.lang");
    assertThat(className.simpleName()).isEqualTo("Integer");
    //
    type.setRequired(true);
    resolved = Resolver.resolveType(context, type);
    assertThat(resolved).isEqualTo(TypeName.INT);
  }

  @Test
  public void longTypes() {
    Type type = new Type();
    type.setCore(CoreType.LONG);
    Context context = new Context(new ObjectMapper(), new Specification());
    TypeName resolved = Resolver.resolveType(context, type);
    assertThat(resolved).isInstanceOf(ClassName.class);
    ClassName className = (ClassName) resolved;
    assertThat(className.packageName()).isEqualTo("java.lang");
    assertThat(className.simpleName()).isEqualTo("Long");
    //
    type.setRequired(true);
    resolved = Resolver.resolveType(context, type);
    assertThat(resolved).isEqualTo(TypeName.LONG);
  }

  @Test
  public void floatTypes() {
    Type type = new Type();
    type.setCore(CoreType.FLOAT);
    Context context = new Context(new ObjectMapper(), new Specification());
    TypeName resolved = Resolver.resolveType(context, type);
    assertThat(resolved).isInstanceOf(ClassName.class);
    ClassName className = (ClassName) resolved;
    assertThat(className.packageName()).isEqualTo("java.lang");
    assertThat(className.simpleName()).isEqualTo("Float");

    type.setRequired(true);
    resolved = Resolver.resolveType(context, type);
    assertThat(resolved).isEqualTo(TypeName.FLOAT);
  }

  @Test
  public void doubleTypes() {
    Type type = new Type();
    type.setCore(CoreType.DOUBLE);
    Context context = new Context(new ObjectMapper(), new Specification());
    TypeName resolved = Resolver.resolveType(context, type);
    assertThat(resolved).isInstanceOf(ClassName.class);
    ClassName className = (ClassName) resolved;
    assertThat(className.packageName()).isEqualTo("java.lang");
    assertThat(className.simpleName()).isEqualTo("Double");

    type.setRequired(true);
    resolved = Resolver.resolveType(context, type);
    assertThat(resolved).isEqualTo(TypeName.DOUBLE);
  }

  @Test
  public void booleanTypes() {
    Type type = new Type();
    type.setCore(CoreType.BOOLEAN);
    Context context = new Context(new ObjectMapper(), new Specification());
    TypeName resolved = Resolver.resolveType(context, type);
    assertThat(resolved).isInstanceOf(ClassName.class);
    ClassName className = (ClassName) resolved;
    assertThat(className.packageName()).isEqualTo("java.lang");
    assertThat(className.simpleName()).isEqualTo("Boolean");

    type.setRequired(true);
    resolved = Resolver.resolveType(context, type);
    assertThat(resolved).isEqualTo(TypeName.BOOLEAN);
  }

  @Test
  public void testStringTypes() {
    Type type = new Type();
    type.setCore(CoreType.STRING);
    Context context = new Context(new ObjectMapper(), new Specification());
    TypeName resolved = Resolver.resolveType(context, type);
    assertThat(resolved).isInstanceOf(ClassName.class);
    ClassName className = (ClassName) resolved;
    assertThat(className.packageName()).isEqualTo("java.lang");
    assertThat(className.simpleName()).isEqualTo("String");

    type.setCore(CoreType.TEXT);
    resolved = Resolver.resolveType(context, type);
    assertThat(resolved).isInstanceOf(ClassName.class);
    className = (ClassName) resolved;
    assertThat(className.packageName()).isEqualTo("java.lang");
    assertThat(className.simpleName()).isEqualTo("String");
  }
}
