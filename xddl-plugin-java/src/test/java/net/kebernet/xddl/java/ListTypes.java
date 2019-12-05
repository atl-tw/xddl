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
import com.google.common.reflect.TypeToken;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.Id;
import net.kebernet.xddl.javatestutils.JavaTestCompiler;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import org.junit.Test;

public class ListTypes {

  @SuppressWarnings("UnstableApiUsage")
  @Test
  public void listOfSimpleTypes()
      throws IOException, IntrospectionException, ClassNotFoundException {
    ObjectMapper mapper = new ObjectMapper();
    Specification spec =
        mapper.readValue(
            StructureClassTest.class.getResourceAsStream("/listSimpleType.json"),
            Specification.class);
    Context ctx = new Context(mapper, spec);

    StructureClass parent = new StructureClass(ctx, spec.structures().get(0));
    File output = new File("build/test-gen/listOfSimpleTypes");
    output.mkdirs();
    parent.write(output);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    Class generated = loader.loadClass(Resolver.resolvePackageName(ctx) + ".Parent");
    BeanInfo beanInfo = Introspector.getBeanInfo(generated);
    Map<String, PropertyDescriptor> descriptors = new HashMap<>();
    for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
      descriptors.put(pd.getName(), pd);
    }

    Type token = new TypeToken<Set<String>>() {}.getType();

    assertThat(descriptors.get("listOfStrings").getReadMethod().getName())
        .isEqualTo("getListOfStrings");
    assertThat(descriptors.get("listOfStrings").getReadMethod().getGenericReturnType())
        .isEqualTo(token);
  }

  @SuppressWarnings("UnstableApiUsage")
  @Test
  public void listRefTypes() throws IOException, IntrospectionException, ClassNotFoundException {
    ObjectMapper mapper = new ObjectMapper();
    Specification spec =
        mapper.readValue(
            StructureClassTest.class.getResourceAsStream("/referenceList.json"),
            Specification.class);
    Context ctx = new Context(mapper, spec);

    StructureClass parent = new StructureClass(ctx, spec.structures().get(0));
    File output = new File("build/test-gen/referenceList");
    output.mkdirs();
    parent.write(output);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    Class generated = loader.loadClass(Resolver.resolvePackageName(ctx) + ".Parent");
    BeanInfo beanInfo = Introspector.getBeanInfo(generated);
    Map<String, PropertyDescriptor> descriptors = new HashMap<>();
    for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
      descriptors.put(pd.getName(), pd);
    }

    Type token = new TypeToken<List<String>>() {}.getType();

    assertThat(descriptors.get("listOfStrings").getReadMethod().getName())
        .isEqualTo("getListOfStrings");
    assertThat(descriptors.get("listOfStrings").getReadMethod().getGenericReturnType())
        .isEqualTo(token);
  }

  @Test
  public void listRefStructure()
      throws IOException, IntrospectionException, ClassNotFoundException, NoSuchFieldException {
    ObjectMapper mapper = new ObjectMapper();
    Specification spec =
        mapper.readValue(
            StructureClassTest.class.getResourceAsStream("/referenceToStructureList.json"),
            Specification.class);
    Context ctx = new Context(mapper, spec);

    StructureClass parent = new StructureClass(ctx, spec.structures().get(0));
    StructureClass child = new StructureClass(ctx, spec.structures().get(1));
    File output = new File("build/test-gen/referenceToStructureList");
    output.mkdirs();
    parent.write(output);
    child.write(output);

    ClassLoader loader = new JavaTestCompiler(output).compile();
    Class generated = loader.loadClass(Resolver.resolvePackageName(ctx) + ".Parent");
    BeanInfo beanInfo = Introspector.getBeanInfo(generated);
    Map<String, PropertyDescriptor> descriptors = new HashMap<>();
    for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
      descriptors.put(pd.getName(), pd);
    }

    Type token = new TypeToken<List<String>>() {}.getType();

    assertThat(descriptors.get("listOfChildren").getReadMethod().getName())
        .isEqualTo("getListOfChildren");
    assertThat(descriptors.get("listOfChildren").getReadMethod().getGenericReturnType().toString())
        .isEqualTo("java.util.List<xddl.Child>");

    assertThat(
            loader
                .loadClass(Resolver.resolvePackageName(ctx) + ".Child")
                .getAnnotationsByType(Entity.class))
        .asList()
        .isNotEmpty();
    assertThat(
            loader
                .loadClass(Resolver.resolvePackageName(ctx) + ".Child")
                .getDeclaredField("intProperty")
                .getAnnotationsByType(Id.class))
        .asList()
        .isNotEmpty();
  }
}
