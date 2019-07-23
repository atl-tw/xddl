package net.kebernet.xddl.jsonschema;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.kebernet.xddl.jsonschema.model.Definition;
import net.kebernet.xddl.jsonschema.model.Schema;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class JsonSchemaPluginTest {

    @Test
    public void basicTest() throws IOException {
        JsonSchemaPlugin instance = new JsonSchemaPlugin();
        ObjectMapper mapper = new ObjectMapper();
        Specification spec = mapper.readValue(JsonSchemaPlugin.class.getResourceAsStream("/sample.json"),
                Specification.class);
        Context ctx = new Context(mapper, spec);
        File basicDir = new File("build/test/basic");
        basicDir.getParentFile().mkdirs();
        instance.generateArtifacts(ctx, basicDir);
    }

    @Test
    public void testListType() throws IOException {
        JsonSchemaPlugin instance = new JsonSchemaPlugin();
        ObjectMapper mapper = new ObjectMapper();
        Specification spec = mapper.readValue(JsonSchemaPlugin.class.getResourceAsStream("/list.json"),
                Specification.class);
        Context ctx = new Context(mapper, spec);
        Schema schema = instance.createSchema(ctx);
        assertThat(schema.getDefinitions()).containsKey("HasAnArray");
        Definition def = schema.getDefinitions().get("HasAnArray");
        assertThat(def.getDescription()).isEqualTo("Has an array of string");
        assertThat(def.getProperties()).containsKey("values");
        Definition values = def.getProperties().get("values");
        assertThat(values.getType()).isEqualTo("array");
        assertThat(values.getMinLength()).isEqualTo(1);
        assertThat(values.getMaxLength()).isEqualTo(2);
        assertThat(values.getItems().getMinLength()).isEqualTo(4);
        assertThat(values.getItems().getMaxLength()).isEqualTo(5);
    }

}