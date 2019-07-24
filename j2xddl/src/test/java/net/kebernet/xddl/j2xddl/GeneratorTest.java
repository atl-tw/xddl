package net.kebernet.xddl.j2xddl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.Structure;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;


public class GeneratorTest {


    private final ObjectMapper mapper = new ObjectMapper();
    {
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    public void testSimpleClass() throws IOException {
        HashSet<Class> classes = new HashSet<>();
        classes.add(SimpleTest.class);
        Generator generator = new Generator(mapper, classes);
        Specification specification =  generator.generate();
        Structure structure = specification.getStructures().get(0);
        assertThat(structure.getName()).isEqualTo("SimpleTest");
        assertThat(structure.getProperties().size()).isEqualTo(3);
        Set<String> names = structure.getProperties().stream().map(BaseType::getName).collect(Collectors.toSet());
        assertThat(names).containsExactly("field1", "field2", "field3");

        mapper.writeValue(new File("build/Simple.xddl.json"), specification);
    }

}