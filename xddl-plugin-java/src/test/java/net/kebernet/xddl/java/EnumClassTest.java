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
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import org.junit.Test;

public class EnumClassTest {

  @Test
  public void enumReferenceGeneration()
      throws IOException, ClassNotFoundException, IntrospectionException {
    ObjectMapper mapper = new ObjectMapper();
    Specification spec =
        mapper.readValue(
            StructureClassTest.class.getResourceAsStream("/enumReference.json"),
            Specification.class);
    Context ctx = new Context(mapper, spec);
    StructureClass struct = new StructureClass(ctx, spec.structures().get(0));
    File output = new File("build/test-gen/enumReference");
    struct.write(output);

    EnumClass enumC = new EnumClass(ctx, spec.types().get(0), spec.types().get(0));
    enumC.write(output);

    ClassLoader loader = new Compiler(output).compile();
    Class generated = loader.loadClass(Resolver.resolvePackageName(ctx) + ".Parent");
    Class enumType = loader.loadClass(Resolver.resolvePackageName(ctx) + ".OrdinalEnum");
    BeanInfo beanInfo = Introspector.getBeanInfo(generated);
    Map<String, PropertyDescriptor> descriptors = new HashMap<>();
    for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
      descriptors.put(pd.getName(), pd);
    }

    assertThat(descriptors.get("enumProperty").getReadMethod().getName())
        .isEqualTo("getEnumProperty");
    assertThat(descriptors.get("enumProperty").getReadMethod().getReturnType()).isEqualTo(enumType);
  }
}
