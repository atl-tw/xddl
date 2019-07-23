package net.kebernet.xddl.model;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.*;

public class StructureTest {

    @Test
    public void testSimpleParse() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        Specification spec = mapper.readValue(StructureTest.class.getResourceAsStream("/sample.json"), Specification.class);

    }

}