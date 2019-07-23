package net.kebernet.xddl.model;

import lombok.Data;

@Data
public class Property {
    private String name;
    private String description;
    private Type type;
}
