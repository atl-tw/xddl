package net.kebernet.xddl.model;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=true)
public class Type extends BaseType {

    private CoreType core;
    private List<Value> examples;
    private List<Value> allowable;

    @Override
    public Type merge(Reference reference) {
        Type newValue = ModelUtil.merge(new Type(), this, reference);
        newValue.setCore(this.getCore());
        newValue.setAllowable(this.getAllowable());
        newValue.setExamples(this.getExamples());
        return newValue;
    }

}
