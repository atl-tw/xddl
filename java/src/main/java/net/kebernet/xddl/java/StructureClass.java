package net.kebernet.xddl.java;

import java.io.File;
import java.io.IOException;
import javax.lang.model.element.Modifier;

import com.google.common.base.CaseFormat;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.Reference;
import net.kebernet.xddl.model.Structure;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;

import static net.kebernet.xddl.java.Resolver.resolvePackageName;
import static net.kebernet.xddl.model.Utils.*;

public class StructureClass {

    private final Context ctx;
    private final Structure structure;
    private final TypeSpec.Builder typeBuilder;
    private final String packageName;
    private final ClassName className;

    public StructureClass(Context context, Structure structure){
        this.ctx = context;
        this.structure = structure;
        this.packageName = resolvePackageName(context);
        this.className = ClassName.get(packageName, structure.getName());
        this.typeBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC);
        ifNotNullOrEmpty(structure.getDescription(),
                d-> typeBuilder.addJavadoc(d));

        neverNull(structure.getProperties())
                .stream()
                .map(this::doPropertyType)
                .peek(fieldSpec -> typeBuilder.addMethod(createGetter(fieldSpec)))
                .peek(fieldSpec -> typeBuilder.addMethod(createSetter(fieldSpec)))
                .peek(fieldSpec -> typeBuilder.addMethod(createBuilder(fieldSpec)))
                .forEach(typeBuilder::addField);

    }

    private MethodSpec createGetter(FieldSpec fieldSpec) {
        String prefix = fieldSpec.type == TypeName.BOOLEAN ? "is": "get";
        String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldSpec.name);
        return MethodSpec.methodBuilder(prefix+name)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc(fieldSpec.javadoc)
                .returns(fieldSpec.type)
                .addCode("return this."+fieldSpec.name+";\n")
                .build();
    }

    private MethodSpec createSetter(FieldSpec fieldSpec) {
        String prefix = "set";
        String name = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, fieldSpec.name);
        return MethodSpec.methodBuilder(prefix+name)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc(fieldSpec.javadoc)
                .returns(TypeName.VOID)
                .addParameter(
                        ParameterSpec.builder(fieldSpec.type, "value", Modifier.FINAL).build()
                )
                .addCode("this."+fieldSpec.name+" = value;\n")
                .build();
    }

    private MethodSpec createBuilder(FieldSpec fieldSpec) {
        String name = fieldSpec.name;
        return MethodSpec.methodBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .returns(className)
                .addParameter(
                        ParameterSpec.builder(fieldSpec.type, "value", Modifier.FINAL).build()
                )
                .addCode("this."+fieldSpec.name+" = value;\n" +
                        "return this;\n")
                .build();
    }

    public void write(File directory) throws IOException {
        JavaFile file = JavaFile.builder(packageName, typeBuilder.build()).build();
        file.writeTo(directory);
    }

    private FieldSpec doPropertyType(BaseType baseType) {
        BaseType resolvedType = baseType;
        if(baseType instanceof Reference){
            resolvedType = ctx.resolveReference((Reference) baseType).orElseThrow(
                    ()->ctx.stateException("Unable to resolve reference", baseType));
        }
        if(resolvedType instanceof Structure & baseType instanceof Reference){
            return doReferenceTo((Structure) resolvedType, ((Reference) baseType).getRef());
        }
        if(resolvedType instanceof Type){
            Type type = (Type) resolvedType;
            return isNullOrEmpty(type.getAllowable()) ?
                doType(type) :
                doEnum(baseType, type);
        }
        throw ctx.stateException("Unsupported type ", baseType);
    }

    private FieldSpec doReferenceTo(Structure resolvedType, String referenceName) {
        return FieldSpec.builder(
                ClassName.get(packageName, referenceName),
                resolvedType.getName(),
                Modifier.PRIVATE).build();
    }

    private FieldSpec doEnum(BaseType baseType, Type type) {
        throw ctx.stateException("Unsupported enum ", baseType);
    }

    private FieldSpec doType(Type type) {
        FieldSpec.Builder builder = FieldSpec.builder(Resolver.resolveType(ctx, type), type.getName(), Modifier.PRIVATE);
        ifNotNullOrEmpty(type.getDescription(), s-> builder.addJavadoc(s+"\n"));
        ifNotNullOrEmpty(type.getComment(), s-> builder.addJavadoc("Comment: "+s+"\n"));
        return builder.build();
    }
}
