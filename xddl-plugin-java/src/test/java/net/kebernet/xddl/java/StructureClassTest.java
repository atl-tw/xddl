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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.HashMap;
import java.util.Map;
import net.kebernet.xddl.Loader;
import net.kebernet.xddl.javatestutils.JavaTestCompiler;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import org.joor.Reflect;
import org.junit.Test;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class StructureClassTest {

  @Test
  public void simpleTypesResolve() throws IOException, IntrospectionException {
    ObjectMapper mapper = new ObjectMapper();

    Specification spec =
        mapper.readValue(
            StructureClassTest.class.getResourceAsStream("/baseTypes.json"), Specification.class);
    Context ctx = new Context(mapper, spec);
    StructureClass structureClass = new StructureClass(ctx, spec.structures().get(0));
    File output = new File("build/test-gen/testBaseTypes");
    output.mkdirs();
    structureClass.write(output);
    String packageName = Resolver.resolvePackageName(ctx);
    String className = packageName + "." + spec.structures().get(0).getName();
    String source =
        CharStreams.toString(
            new FileReader(new File(output, className.replaceAll("\\.", "/") + ".java")));
    Object compiled = Reflect.compile(className, source).create().get();
    Map<String, Field> fields = new HashMap<>();

    for (Field f : compiled.getClass().getDeclaredFields()) {
      f.setAccessible(true);
      fields.put(f.getName(), f);
    }

    assertThat(fields.get("intProperty").getType()).isEqualTo(int.class);
    assertThat(fields.get("integerProperty").getType()).isEqualTo(Integer.class);
    assertThat(fields.get("stringProperty").getType()).isEqualTo(String.class);
    assertThat(fields.get("textProperty").getType()).isEqualTo(String.class);
    assertThat(fields.get("dateProperty").getType()).isEqualTo(LocalDate.class);
    assertThat(fields.get("timeProperty").getType()).isEqualTo(OffsetTime.class);
    assertThat(fields.get("dateTimeProperty").getType()).isEqualTo(OffsetDateTime.class);
    assertThat(fields.get("binProperty").getType()).isEqualTo(byte[].class);

    BeanInfo beanInfo = Introspector.getBeanInfo(compiled.getClass());

    Map<String, PropertyDescriptor> descriptors = new HashMap<>();
    for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
      descriptors.put(pd.getName(), pd);
    }

    assertThat(descriptors.get("intProperty").getReadMethod().getName())
        .isEqualTo("getIntProperty");
    assertThat(descriptors.get("intProperty").getWriteMethod().getName())
        .isEqualTo("setIntProperty");
    assertThat(descriptors.get("boolProperty").getReadMethod().getName())
        .isEqualTo("isBoolProperty");
  }

  @Test
  public void structureReferenceResolves()
      throws IOException, IntrospectionException, ClassNotFoundException {
    ObjectMapper mapper = new ObjectMapper();
    Specification spec =
        mapper.readValue(
            StructureClassTest.class.getResourceAsStream("/structureReference.json"),
            Specification.class);
    Context ctx = new Context(mapper, spec);
    StructureClass parent = new StructureClass(ctx, spec.structures().get(0));
    StructureClass child = new StructureClass(ctx, spec.structures().get(1));
    File output = new File("build/test-gen/testStructureReference");
    output.mkdirs();
    parent.write(output);
    child.write(output);
    String packageName = Resolver.resolvePackageName(ctx);
    ClassLoader loader = new JavaTestCompiler(output).compile();

    Class parentClass = loader.loadClass(packageName + ".Parent");
    Class childClass = loader.loadClass(packageName + ".Child");

    BeanInfo beanInfo = Introspector.getBeanInfo(parentClass);
    Map<String, PropertyDescriptor> descriptors = new HashMap<>();
    for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
      descriptors.put(pd.getName(), pd);
    }

    assertThat(descriptors.get("childProperty").getReadMethod().getName())
        .isEqualTo("getChildProperty");
    assertThat(descriptors.get("childProperty").getReadMethod().getReturnType())
        .isEqualTo(childClass);
  }

  @Test
  public void nestedEnumGeneration()
      throws IOException, IntrospectionException, ClassNotFoundException {
    ObjectMapper mapper = new ObjectMapper();
    Specification spec =
        mapper.readValue(
            StructureClassTest.class.getResourceAsStream("/nestedEnum.json"), Specification.class);
    Context ctx = new Context(mapper, spec);
    StructureClass struct = new StructureClass(ctx, spec.structures().get(0));
    File output = new File("build/test-gen/nestedEnum");
    struct.write(output);
    ClassLoader loader = new JavaTestCompiler(output).compile();
    Class generated = loader.loadClass(Resolver.resolvePackageName(ctx) + ".Parent");
    Class enumType =
        loader.loadClass(Resolver.resolvePackageName(ctx) + ".Parent$EnumPropertyType");
    BeanInfo beanInfo = Introspector.getBeanInfo(generated);
    Map<String, PropertyDescriptor> descriptors = new HashMap<>();
    for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
      descriptors.put(pd.getName(), pd);
    }

    assertThat(descriptors.get("enumProperty").getReadMethod().getName())
        .isEqualTo("getEnumProperty");
    assertThat(descriptors.get("enumProperty").getReadMethod().getReturnType()).isEqualTo(enumType);
  }

  @Test
  public void structureNested() throws IOException, IntrospectionException, ClassNotFoundException {
    ObjectMapper mapper = new ObjectMapper();
    Specification spec =
        mapper.readValue(
            StructureClassTest.class.getResourceAsStream("/nestedStructure.json"),
            Specification.class);
    Context ctx = new Context(mapper, spec);
    StructureClass parent = new StructureClass(ctx, spec.structures().get(0));
    File output = new File("build/test-gen/nestedStructure");
    output.mkdirs();
    parent.write(output);
    String packageName = Resolver.resolvePackageName(ctx);
    ClassLoader loader = new JavaTestCompiler(output).compile();

    Class parentClass = loader.loadClass(packageName + ".Parent");
    Class childClass = loader.loadClass(packageName + ".Parent$ChildOfTheCornType");

    BeanInfo beanInfo = Introspector.getBeanInfo(parentClass);
    Map<String, PropertyDescriptor> descriptors = new HashMap<>();
    for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
      descriptors.put(pd.getName(), pd);
    }

    assertThat(descriptors.get("childOfTheCorn").getReadMethod().getName())
        .isEqualTo("getChildOfTheCorn");
    assertThat(descriptors.get("childOfTheCorn").getReadMethod().getReturnType())
        .isEqualTo(childClass);
  }

  @Test
  public void testOGNLVersion()
      throws IOException, IntrospectionException, ClassNotFoundException, IllegalAccessException,
          InstantiationException, InvocationTargetException {
    Specification spec =
        Loader.builder().main(new File("src/test/resources/ognl.json")).build().read();
    Context ctx = new Context(Loader.mapper(), spec);
    StructureClass parent = new StructureClass(ctx, spec.structures().get(0));
    File output = new File("build/test-gen/ognl");
    output.mkdirs();
    parent.write(output);
    String packageName = Resolver.resolvePackageName(ctx);
    ClassLoader loader = new JavaTestCompiler(output).compile();

    Class parentClass = loader.loadClass(packageName + ".Parent");
    BeanInfo beanInfo = Introspector.getBeanInfo(parentClass);
    Map<String, PropertyDescriptor> descriptors = new HashMap<>();
    for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
      descriptors.put(pd.getName(), pd);
    }

    String version =
        (String) descriptors.get("version").getReadMethod().invoke(parentClass.newInstance());
    assertThat(version).isEqualTo("0.333.0");
  }
}
