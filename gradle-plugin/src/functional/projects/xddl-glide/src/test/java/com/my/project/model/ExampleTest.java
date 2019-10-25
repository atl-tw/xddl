package com.my.project.model;


import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.my.project.model.v1_0.Team;
import org.junit.Test;

public class ExampleTest {



    @Test
    public void example() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        new File("build/test/output/").mkdirs();
        JsonNode tree = mapper.readTree(
                ExampleTest.class.getResourceAsStream("/MyTeam-v1.0.json")
        );

        // First we read a file
        com.my.project.model.v1_0.Team step1 = mapper.treeToValue(tree,
                Team.class
        );
        System.out.println("Read MyTeam-v1.0.json as:\n"+mapper.writeValueAsString(step1)+"\n\n");
        mapper.writeValue(new File("build/test/output/1.0.json"), step1);


        //Next we migrate the team to the v1.0.1 version:
        com.my.project.model.v1_0_1.migration.Team firstMigration =
                new com.my.project.model.v1_0_1.migration.Team();
        firstMigration.apply((ObjectNode) tree, tree);

        //Now we can parse map it to the new version:
        com.my.project.model.v1_0_1.Team step2 = mapper.treeToValue(tree,
                com.my.project.model.v1_0_1.Team.class
        );


        System.out.println("Migrated to v1.0.1:\n"+mapper.writeValueAsString(step2)+"\n\n");
        mapper.writeValue(new File("build/test/output/1.0.1.json"), step2);

        //Now we can migrate it to the v1.0.2 version
        com.my.project.model.v1_0_2.migration.Team secondMigration =
                new com.my.project.model.v1_0_2.migration.Team();
        secondMigration.apply((ObjectNode) tree, tree);

        //Now we can parse map it to the third version:
        com.my.project.model.v1_0_2.Team step3 = mapper.treeToValue(tree,
                com.my.project.model.v1_0_2.Team.class
        );
        System.out.println("Migrated to v1.0.2:\n"+mapper.writeValueAsString(step3)+"\n\n");
        mapper.writeValue(new File("build/test/output/1.0.2.json"), step3);



    }
}