package org.edu_sharing.alfresco.service.search.cmis;

import java.util.Arrays;
import java.util.stream.Stream;

public class Filters {
    private static Predicate parenthesised(Predicate predicate) {
        return new Predicate("(", null, new Predicate(")", predicate, null));
    }

    public static Predicate and(Predicate... predicates) {
        if (predicates.length < 1) {
            throw new IllegalArgumentException("requires at leased 1 predicates");
        }

        if (predicates.length == 1) {
            return predicates[0];
        }

        return parenthesised(Arrays.stream(predicates).reduce((lhs, rhs) -> new Predicate("AND", lhs, rhs)).get());
    }

    public static Predicate or(Predicate... predicates) {
        if (predicates.length < 1) {
            throw new IllegalArgumentException("requires at leased 1 predicates");
        }

        if (predicates.length == 1) {
            return predicates[0];
        }

        return parenthesised(Arrays.stream(predicates).reduce((lhs, rhs) -> new Predicate("OR", lhs, rhs)).get());
    }
    public static <TL, TR> Predicate eq(TL lhs, TR rhs) {
        return new Predicate("=", Value.create(lhs), Value.create(rhs));
    }

    public static <TL, TR> Predicate neq(TL lhs, TR rhs) {
        return new Predicate("<>", Value.create(lhs), Value.create(rhs));
    }

    public static <TL, TR> Predicate gt(TL lhs, TR rhs) {
        return new Predicate(">", Value.create(lhs), Value.create(rhs));
    }

    public static <TL, TR> Predicate gte(TL lhs, TR rhs) {
        return new Predicate(">=", Value.create(lhs), Value.create(rhs));
    }

    public static <TL, TR> Predicate lt(TL lhs, TR rhs) {
        return new Predicate("<", Value.create(lhs), Value.create(rhs));
    }

    public static <TL, TR> Predicate lte(TL lhs, TR rhs) {
        return new Predicate("<=", Value.create(lhs), Value.create(rhs));
    }

    public static <TL, TR> Predicate like(TL lhs, TR rhs) {
        return new Predicate("LIKE", Value.create(lhs), Value.create(rhs));
    }

    public static <T> Predicate isNull(T arg) {
        return new Predicate("IS NULL", Value.create(arg), null);
    }


    public static <T> Predicate isNotNull(T arg) {
        return new Predicate("IS NOT NULL", Value.create(arg), null);
    }
}
