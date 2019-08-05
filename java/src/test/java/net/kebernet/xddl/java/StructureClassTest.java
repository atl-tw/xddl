package net.kebernet.xddl.java;


import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.CharStreams;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import org.joor.Reflect;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class StructureClassTest {

    @Test
    public void testBaseTypes() throws IOException, IntrospectionException {
        ObjectMapper mapper = new ObjectMapper();

        Specification spec = mapper.readValue(StructureClassTest.class.getResourceAsStream("/baseTypes.json"), Specification.class);
        Context ctx = new Context(mapper, spec);
        StructureClass structureClass = new StructureClass(ctx, spec.structures().get(0));
        File output = new File("build/test-gen/testBaseTypes");
        output.mkdirs();
        structureClass.write(output);
        String packageName = Resolver.resolvePackageName(ctx);
        String className = packageName+"."+spec.structures().get(0).getName();
        String source = CharStreams.toString(new FileReader(new File(output, className.replaceAll("\\.", "/")+".java")));
        Object compiled = Reflect.compile(className, source).create().get();
        Map<String, Field> fields = new HashMap<>();

        for(Field f : compiled.getClass().getDeclaredFields()){
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
        for(PropertyDescriptor pd: beanInfo.getPropertyDescriptors()){
            descriptors.put(pd.getName(), pd);
        }

        assertThat(descriptors.get("intProperty").getReadMethod().getName()).isEqualTo("getIntProperty");
        assertThat(descriptors.get("intProperty").getWriteMethod().getName()).isEqualTo("setIntProperty");
        assertThat(descriptors.get("boolProperty").getReadMethod().getName()).isEqualTo("isBoolProperty");

    }

    @Test
    public void testStructureReference() throws IOException, IntrospectionException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        ObjectMapper mapper = new ObjectMapper();
        Specification spec = mapper.readValue(StructureClassTest.class.getResourceAsStream("/structureReference.json"), Specification.class);
        Context ctx = new Context(mapper, spec);
        StructureClass parent = new StructureClass(ctx, spec.structures().get(0));
        StructureClass child = new StructureClass(ctx, spec.structures().get(1));
        File output = new File("build/test-gen/testStructureReference");
        output.mkdirs();
        parent.write(output);
        child.write(output);
        String packageName = Resolver.resolvePackageName(ctx);
        ClassLoader loader = new Compiler(output).compile();

        Class parentClass = loader.loadClass(packageName+".Parent");
        Class childClass = loader.loadClass(packageName+".Child");

        BeanInfo beanInfo = Introspector.getBeanInfo(parentClass);
        Map<String, PropertyDescriptor> descriptors = new HashMap<>();
        for(PropertyDescriptor pd: beanInfo.getPropertyDescriptors()){
            descriptors.put(pd.getName(), pd);
        }

        assertThat(descriptors.get("childProperty").getReadMethod().getName()).isEqualTo("getChildProperty");
        assertThat(descriptors.get("childProperty").getReadMethod().getReturnType()).isEqualTo(childClass);


    }

}