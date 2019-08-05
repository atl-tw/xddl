package net.kebernet.xddl.model;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public interface HasExtensions {

    Map<String, JsonNode> ext();
}
