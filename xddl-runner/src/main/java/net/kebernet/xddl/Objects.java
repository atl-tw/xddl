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
package net.kebernet.xddl;

import static java.util.Optional.ofNullable;

import java.util.Optional;
import java.util.function.Function;

public class Objects {

  public static <A, B> Optional<B> elvis(A object, Function<A, B> ab) {
    return ofNullable(object).map(ab);
  }

  public static <A, B, C> Optional<C> elvis(A object, Function<A, B> ab, Function<B, C> bc) {
    return elvis(object, ab).map(bc);
  }

  public static <A, B, C, D> Optional<D> elvis(
      A object, Function<A, B> ab, Function<B, C> bc, Function<C, D> cd) {
    return elvis(object, ab, bc).map(cd);
  }
}
