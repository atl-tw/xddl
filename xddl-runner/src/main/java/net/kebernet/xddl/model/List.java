package net.kebernet.xddl.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper=true)
@NoArgsConstructor
public class List extends BaseType<List> {
    BaseType type;

    @Override
    public List merge(Reference reference) {
        List newValue =  ModelUtil.merge(new List(), this, reference);
        newValue.setType(this.type);
        return newValue;
    }
}
