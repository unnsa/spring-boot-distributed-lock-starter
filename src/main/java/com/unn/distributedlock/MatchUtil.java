package com.unn.distributedlock;

import java.util.*;

public class MatchUtil {
    private MatchUtil() {
    }

    public static <R> TwoConditionsBuilder<R> twoConditionsBuilder() {
        return new TwoConditionsBuilder<>();
    }

    public static <R> ThreeConditionsBuilder<R> threeConditionsBuilder() {
        return new ThreeConditionsBuilder<>();
    }


    public static void main(String[] args) {
//        MatchUtil.<Runnable>twoConditionsBuilder()
//                .put("a", "b", () -> System.out.println("a,b"))
//                .put("a", "c", () -> System.out.println("a,c"))
//                .put(__, "b", () -> System.out.println("__.d"))
//                .build("c", "b")
//                .ifPresent(Runnable::run);
        MatchUtil.<Runnable>threeConditionsBuilder()
                .put("a", "b", "c", () -> System.out.println("a,b,c"))
                .put("a", "c", "b", () -> System.out.println("a,c,b"))
                .put(__, "b", "c", () -> System.out.println("__,b,c"))
                .put(__, __, __, () -> System.out.println("others"))
                .build("z", "x", "f")
                .ifPresent(Runnable::run);
    }

    public static class TwoConditionsBuilder<R> {
        private final Map<Object, Map<Object, R>> c1TwoC2Map = new HashMap<>();
        private final Map<Object, R> c1Map__ = new HashMap<>();
        private final Map<Object, R> c2Map__ = new HashMap<>();

        public TwoConditionsBuilder<R> put(Object one, Object two, R r) {
            checkParam(one, two, r);
            if (two == __)
                c1Map__.put(one, r);
            if (one == __)
                c2Map__.put(two, r);
            final Map<Object, R> c2Map = Optional.ofNullable(c1TwoC2Map.get(one))
                    .orElseGet(HashMap::new);
            c2Map.put(two, r);
            c1TwoC2Map.put(one, c2Map);
            return this;
        }

        public Optional<R> build(Object one, Object two) {
            checkParam(one, two);
            final R r2 = c2Map__.get(two);
            if (r2 != null) {
                return Optional.of(r2);
            }
            final R r1 = c1Map__.get(one);
            if (r1 != null) {
                return Optional.of(r1);
            }
            return Optional.ofNullable(c1TwoC2Map.get(one))
                    .map(c2m -> c2m.get(two));
        }
    }


    private static void checkParam(Object... objs) {
        if (Objects.isNull(objs)) {
            return;
        }
        for (Object obj : objs) {
            if (Objects.isNull(obj)) {
                throw new IllegalArgumentException("must not be null");
            }
        }
    }


    public final static Pattern __ = Pattern.__;

    private enum Pattern {
        __
    }

    public static class ThreeConditionsBuilder<R> {
        private final List<Object> condition1List = new ArrayList<>();
        private final List<Object> condition2List = new ArrayList<>();
        private final List<Object> condition3List = new ArrayList<>();
        private final List<R> rList = new ArrayList<>();

        public ThreeConditionsBuilder<R> put(Object c1, Object c2, Object c3, R r) {
            checkParam(c1, c2, c3, r);
            condition1List.add(c1);
            condition2List.add(c2);
            condition3List.add(c3);
            rList.add(r);
            return this;
        }

        public Optional<R> build(Object c1, Object c2, Object c3) {
            checkParam(c1, c2, c3);
            for (int i = 0; i < condition1List.size(); i++) {
                Object o1 = condition1List.get(i);
                Object o2 = condition2List.get(i);
                Object o3 = condition3List.get(i);
                if ((o1.equals(c1) && o2.equals(c2) && o3.equals(c3))
                        || (o1 == __ && o2.equals(c2) && o3.equals(c3))
                        || (o1 == __ && o2 == __ && o3.equals(c3))
                        || (o1 == __ && o2.equals(c2) && o3 == __)
                        || (o1 == __ && o2 == __ && o3 == __)
                        || (o1.equals(c1) && o2 == __ && o3.equals(c3))
                        || (o1.equals(c1) && o2 == __ && o3 == __)
                        || (o1.equals(c1) && o2.equals(c2) && o3 == __)
                ) {
                    return Optional.ofNullable(rList.get(i));
                }
            }
            return Optional.empty();
        }

    }

}
