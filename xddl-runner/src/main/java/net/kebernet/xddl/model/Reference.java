package net.kebernet.xddl.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class Reference extends BaseType {
    String ref;

    @Override
    public Reference merge(Reference reference) {
        return ModelUtil.merge(new Reference(), this, reference);
    }
}
