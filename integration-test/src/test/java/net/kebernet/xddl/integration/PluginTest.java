package net.kebernet.xddl.integration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.kebernet.xddl.Runner;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class PluginTest {

    @Test
    public void jsonSchemaTestWithMain() throws IOException {
        Runner.main("--input-file", "src/test/resources/int-range.json",
                "--output-directory", "build/test/schema-test/",
                "--format", "json");
        File file = new File("build/test/schema-test/schema.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readValue(file, JsonNode.class);
        assertThat(node.get("definitions").get("HasRanges").get("properties")
                .get("exclusiveValue").get("exclusiveMinimum").intValue()).isEqualTo(2);

    }

    @Test
    public void jsonSchemaTestWithBuilder() throws IOException {
        Runner.builder()
                .outputDirectory( new File("build/test/schema-test-2/"))
               .plugins(Arrays.asList("json"))
               .specificationFile(new File( "src/test/resources/int-range.json"))
               .build()
               .run();

        File file = new File("build/test/schema-test-2/schema.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readValue(file, JsonNode.class);
        assertThat(node.get("definitions").get("HasRanges").get("properties")
                .get("exclusiveValue").get("exclusiveMinimum").intValue()).isEqualTo(2);

    }
}
