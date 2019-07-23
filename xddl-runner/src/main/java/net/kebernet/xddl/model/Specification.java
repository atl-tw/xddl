package net.kebernet.xddl.model;

import java.util.List;

import lombok.Data;

@Data
public class Specification {
    private String description;
    private List<Type> types;
    private List<Structure> structures;
}
