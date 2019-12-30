/*
 * Copyright 2019, 2020 Robert Cooper, ThoughtWorks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.kebernet.xddl.java;

import static java.util.Optional.ofNullable;
import static net.kebernet.xddl.model.ModelUtil.extensionValueAsString;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Splitter;
import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.kebernet.xddl.model.BaseType;
import net.kebernet.xddl.model.CoreType;
import net.kebernet.xddl.model.Type;
import net.kebernet.xddl.plugins.Context;

public abstract class Resolver {

  private static final Pattern packagePattern = Pattern.compile("([^A-Z]*)\\..*");
  private static final Pattern parameterPattern = Pattern.compile(".*(<(.*)>).*");

  private static final Map<CoreType, TypeName> BASIC_TYPES =
      new HashMap<CoreType, TypeName>() {
        {
          this.put(CoreType.STRING, ClassName.get(String.class));
          this.put(CoreType.TEXT, ClassName.get(String.class));
          this.put(CoreType.BINARY, ArrayTypeName.of(TypeName.BYTE));
          this.put(CoreType.BIG_DECIMAL, ClassName.get(BigDecimal.class));
          this.put(CoreType.BIG_INTEGER, ClassName.get(BigInteger.class));
          this.put(CoreType.DATE, ClassName.get(LocalDate.class));
          this.put(CoreType.TIME, ClassName.get(OffsetTime.class));
          this.put(CoreType.DATETIME, ClassName.get(OffsetDateTime.class));
        }
      };

  private static final Map<CoreType, PrimitivePair> PRIMITIVE_TYPES =
      new HashMap<CoreType, PrimitivePair>() {
        {
          this.put(CoreType.INTEGER, new PrimitivePair(ClassName.get(Integer.class), TypeName.INT));
          this.put(CoreType.LONG, new PrimitivePair(ClassName.get(Long.class), TypeName.LONG));
          this.put(CoreType.FLOAT, new PrimitivePair(ClassName.get(Float.class), TypeName.FLOAT));
          this.put(
              CoreType.DOUBLE, new PrimitivePair(ClassName.get(Double.class), TypeName.DOUBLE));
          this.put(
              CoreType.BOOLEAN, new PrimitivePair(ClassName.get(Boolean.class), TypeName.BOOLEAN));
        }
      };

  private static class PrimitivePair {
    private final ClassName object;
    private final TypeName primitive;

    private PrimitivePair(ClassName object, TypeName primitive) {
      this.object = object;
      this.primitive = primitive;
    }
  }

  public static String resolvePackageName(Context context) {
    Optional<String> optional =
        extensionValueAsString(context.getSpecification(), "java", "package");
    return optional.orElse("xddl")
        + ofNullable(context.getSpecification().getVersion())
            .map(v -> ".v" + v.replaceAll("\\.", "_"))
            .orElse("");
  }

  @SuppressWarnings("unused")
  public TypeName resolve(Context context, BaseType type) {
    if (type instanceof Type) {
      return resolveType(context, (Type) type);
    }
    throw new UnsupportedOperationException(
        "Cant do " + type.getClass().getCanonicalName() + " yet");
  }

  static TypeName resolveListType(Context context, net.kebernet.xddl.model.List list) {
    return extensionValueAsString(list, "java", "type")
        .map(s -> parse(s, resolvePackageName(context)))
        .orElse(ClassName.get(java.util.List.class));
  }

  @VisibleForTesting
  static TypeName resolveType(Context context, Type type) {
    Optional<TypeName> extension =
        extensionValueAsString(type, "java", "type")
            .map(s -> parse(s, resolvePackageName(context)));
    if (extension.isPresent()) {
      return extension.get();
    }
    if (PRIMITIVE_TYPES.containsKey(type.getCore())) {
      return Boolean.TRUE.equals(type.getRequired())
          ? PRIMITIVE_TYPES.get(type.getCore()).primitive
          : PRIMITIVE_TYPES.get(type.getCore()).object;
    }
    if (BASIC_TYPES.containsKey(type.getCore())) {
      return BASIC_TYPES.get(type.getCore());
    }
    throw context.stateException("Unable to resolve Java type for ", type);
  }

  public static TypeName parse(String name, String defaultPackage) {
    List<TypeName> parameterNames = null;
    Matcher paramMatcher = parameterPattern.matcher(name);
    if (paramMatcher.matches()) {
      parameterNames =
          Splitter.on(',').trimResults().splitToList(paramMatcher.group(2)).stream()
              .map(p -> parse(p, defaultPackage))
              .collect(Collectors.toList());
      name = name.replaceAll(paramMatcher.group(1), "").trim();
    }

    String classNames = name;
    String packageName = defaultPackage;
    Matcher matcher = packagePattern.matcher(name);
    if (matcher.matches()) {
      packageName = matcher.group(1);
      classNames = classNames.substring(packageName.length() + 1);
    }
    List<String> simpleNames =
        new ArrayList<>(Splitter.on(".").omitEmptyStrings().trimResults().splitToList(classNames));
    String first = simpleNames.remove(0);
    String[] rest = simpleNames.toArray(new String[0]);
    if (parameterNames != null) {
      return ParameterizedTypeName.get(
          ClassName.get(packageName, first, rest), parameterNames.toArray(new TypeName[0]));
    } else {
      return ClassName.get(packageName, first, rest);
    }
  }
}
