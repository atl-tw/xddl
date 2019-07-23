package net.kebernet.xddl;

import java.util.Optional;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

public class Objects {

    public static <A, B> Optional<B> elvis(A object,
                                           Function<A, B> ab) {
        return ofNullable(object)
                .map(ab);
    }

    public static <A, B, C> Optional<C> elvis(A object,
                                              Function<A, B> ab,
                                              Function<B, C> bc) {
        return elvis(object, ab).map(bc);

    }
    public static <A, B, C, D> Optional<D> elvis(A object,
                                              Function<A, B> ab,
                                              Function<B, C> bc,
                                              Function<C, D> cd) {
        return elvis(object, ab, bc).map(cd);

    }
}
