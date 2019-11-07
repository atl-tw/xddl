/*
 * Copyright 2019 Robert Cooper, ThoughtWorks
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
package net.kebernet.xddl.graphwalker.util;

import com.google.common.collect.Lists;
import java.lang.reflect.Field;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class Reflection {

  private Reflection() {}

  /**
   * Returns a list of fields from the start class up to the parent exclusively and marks the as
   * accessible
   *
   * @param startClass Start class
   * @param exclusiveParent Stop class
   * @return List of Field objects from the highest level parent to the start class.
   */
  public static List<Field> findFieldsUpTo(
      @Nonnull Class<?> startClass, @Nullable Class<?> exclusiveParent) {

    List<Field> currentClassFields =
        Lists.reverse(Lists.newArrayList(startClass.getDeclaredFields()));
    Class<?> parentClass = startClass.getSuperclass();

    if (parentClass != null && !(parentClass.equals(exclusiveParent))) {
      List<Field> parentClassFields = findFieldsUpTo(parentClass, exclusiveParent);
      currentClassFields.addAll(Lists.reverse(parentClassFields));
    }
    currentClassFields.forEach(f -> f.setAccessible(true));
    return Lists.reverse(currentClassFields);
  }
}
