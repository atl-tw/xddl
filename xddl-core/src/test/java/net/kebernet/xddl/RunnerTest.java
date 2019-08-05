package net.kebernet.xddl;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import net.kebernet.xddl.model.BaseType;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class RunnerTest {


    @Test
    public void testIncludes() throws IOException {
        Runner runner = Runner.builder()
                .includes(Collections.singletonList(new File("./src/test/resources/includes-pass")))
                .specificationFile(new File("./src/test/resources/empty.json"))
                .build();
        runner.run();
        Set<String> structs = runner.getContext()
                .getSpecification().structures().stream().map(BaseType::getName).collect(Collectors.toSet());
        Set<String> types = runner.getContext()
                .getSpecification().types().stream().map(BaseType::getName).collect(Collectors.toSet());

        assertThat(structs).containsAtLeast("Struct1", "Struct2");
        assertThat(types).contains("int_type");
    }

    @Test(expected = RuntimeException.class)
    public void testIncludesFail() throws IOException {
        Runner runner = Runner.builder()
                .includes(Collections.singletonList(new File("./src/test/resources/includes-reference")))
                .specificationFile(new File("./src/test/resources/empty.json"))
                .build();
        runner.run();
    }

}