package net.kebernet.xddl.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
@JsonTypeInfo(use= JsonTypeInfo.Id.NAME, property="@type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Reference.class, name = "Reference"),
        @JsonSubTypes.Type(value = Structure.class, name = "Structure"),
        @JsonSubTypes.Type(value = Type.class, name = "Type"),
        @JsonSubTypes.Type(value = List.class, name = "List")
})
public abstract class BaseType<T extends BaseType> {
    private String name;
    private String description;
    private Map<String, JsonNode> ext;
    private boolean required;

    public Map<String, JsonNode> ext(){
        if(this.ext == null){
            this.ext = new HashMap<>();
        }
        return this.ext;
    }

    public abstract T merge(Reference reference);
}
