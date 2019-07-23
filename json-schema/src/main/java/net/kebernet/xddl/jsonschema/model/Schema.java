package net.kebernet.xddl.jsonschema.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Schema {
    private String schema = "http://json-schema.org/draft-07/schema#";
    private String ref;
    private Map<String, Definition> definitions;

    public Map<String, Definition> definitions(){
        if(this.definitions == null){
            this.definitions = new HashMap<>();
        }
        return definitions;
    }
}
