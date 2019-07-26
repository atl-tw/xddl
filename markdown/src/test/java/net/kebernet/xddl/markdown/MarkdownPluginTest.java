package net.kebernet.xddl.markdown;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.SerializationFeature;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.plugins.Context;
import org.junit.Test;

public class MarkdownPluginTest {

  @Test
  public void simpleTest() throws IOException {
    MarkdownPlugin instance = new MarkdownPlugin();
    ObjectMapper mapper = new ObjectMapper();
    mapper.enable(SerializationFeature.INDENT_OUTPUT);
    Specification spec =
        mapper.readValue(
            MarkdownPlugin.class.getResourceAsStream("/sample.json"), Specification.class);
    Context ctx = new Context(mapper, spec);
    File basicDir = new File("build/test/basic");
    basicDir.mkdirs();
    instance.generateArtifacts(ctx, basicDir);
  }
}
