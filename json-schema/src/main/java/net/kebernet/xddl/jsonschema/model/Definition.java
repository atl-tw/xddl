package net.kebernet.xddl.jsonschema.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Definition {
    private String title;
    private String description;
    private String format;
    private String type;
    private Map<String, Definition> properties;
    private List<String> required;
    private Boolean additionalProperties;
    private Integer minLength;
    private Integer maxLength;
    private String pattern;
    private Number minimum;
    private Number exclusiveMinimum;
    private Number maximum;
    private Number exclusiveMaximum;
    private PropertyNames propertyNames;
    private Integer minProperties;
    private Integer maxProperties;
    private Map<String, List<String>> dependencies;
    private Map<String, Definition> patternProperties;

    @JsonProperty("$ref")
    private String ref;
    private Definition items;

    public Map<String, Definition> properties(){
        if(this.properties == null){
            this.properties = new HashMap<>();
        }
        return this.properties;
    }

    public List<String> required(){
        if(this.required == null){
            this.required = new ArrayList<>();
        }
        return required;
    }

    @Data
    public static class PropertyNames {
        private String pattern;
    }
}
