package net.kebernet.xddl.plugins;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.kebernet.xddl.model.Specification;
import net.kebernet.xddl.model.StructureTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class ContextTest {


    @Test(expected = IllegalStateException.class)
    public void testIllegalReference() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Specification spec = mapper.readValue(StructureTest.class.getResourceAsStream("/illegal_ref.json"), Specification.class);
        Context context = new Context(mapper, spec);
    }

}