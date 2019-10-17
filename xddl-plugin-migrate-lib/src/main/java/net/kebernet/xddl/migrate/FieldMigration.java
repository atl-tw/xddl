package net.kebernet.xddl.migrate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface FieldMigration {
    JsonNode migrate(ObjectNode root, ObjectNode local, String fieldName, JsonNode current);
}
