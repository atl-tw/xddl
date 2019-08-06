package net.kebernet.xddl.java;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.reflect.TypeToken;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

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

        ClassLoader loader = new Compiler(output).compile();
        Class generated = loader.loadClass(Resolver.resolvePackageName(ctx) + ".Parent");
        BeanInfo beanInfo = Introspector.getBeanInfo(generated);
        Map<String, PropertyDescriptor> descriptors = new HashMap<>();
        for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
            descriptors.put(pd.getName(), pd);
        }

        Type token = new TypeToken<List<String>>(){}.getType();

        assertThat(descriptors.get("listOfStrings").getReadMethod().getName())
                .isEqualTo("getListOfStrings");
        assertThat(descriptors.get("listOfStrings").getReadMethod().getGenericReturnType()).isEqualTo(token);
    }
}
