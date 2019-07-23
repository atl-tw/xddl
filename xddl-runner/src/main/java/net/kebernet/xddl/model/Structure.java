package net.kebernet.xddl.model;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class Structure extends BaseType {
    List<BaseType> properties;

    @Override
    public Structure merge(Reference reference) {
        Structure newValue =  ModelUtil.merge(new Structure(), this, reference);
        newValue.setProperties(this.properties);
        return newValue;
    }
}
