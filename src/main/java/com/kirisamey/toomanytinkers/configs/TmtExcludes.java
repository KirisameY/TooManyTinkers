package com.kirisamey.toomanytinkers.configs;

import com.ibm.icu.impl.Pair;
import lombok.AllArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TmtExcludes {
    private static LocationMatcher<LocationMatcher<Boolean>> parts_materialsMatcher;
    private static LocationMatcher<Boolean> materialsMatcher;


    public static boolean isExcluded(ResourceLocation part, ResourceLocation material) {
        if (materialsMatcher.Match(material).anyMatch(b -> b)) return true;
        return parts_materialsMatcher.Match(part).anyMatch(
                matcher -> matcher.Match(material).anyMatch(
                        b -> b
                )
        );
    }


    static void setExcludes(LocationRule parts, LocationRule materials,
                            Map<ResourceLocation, LocationRule> byPartPrefixes,
                            Map<ResourceLocation, LocationRule> byPartExacts) {
        materialsMatcher = matcherFrom(materials);

        Map<String, @NotNull List<Pair<String, @NotNull LocationMatcher<Boolean>>>> prefixes = byPartPrefixes.entrySet().stream()
                .map(entry -> Pair.of(
                        entry.getKey().getNamespace(),
                        Pair.of(entry.getKey().getPath(), matcherFrom(entry.getValue()))
                )).collect(Collectors.groupingBy(
                        p -> p.first,
                        Collectors.mapping(p -> p.second, Collectors.toCollection(ArrayList::new))
                ));

        Map<String, @NotNull Map<String, @NotNull LocationMatcher<Boolean>>> exacts = byPartExacts.entrySet().stream()
                .map(entry -> Pair.of(
                        entry.getKey().getNamespace(),
                        Pair.of(entry.getKey().getPath(), matcherFrom(entry.getValue()))
                )).collect(Collectors.groupingBy(
                        p -> p.first,
                        Collectors.mapping(p -> p.second, Collectors.toMap(
                                p -> p.first,
                                p -> p.second
                        ))
                ));

        parts.prefixes.forEach(l -> {
            var list = prefixes.computeIfAbsent(l.getNamespace(), k -> new ArrayList<>());
            list.add(Pair.of(l.getPath(), new LocationMatcher.WildCard<>(true)));
        });

        parts.exacts.forEach(l -> {
            var map = exacts.computeIfAbsent(l.getNamespace(), k -> new HashMap<>());
            map.put(l.getPath(), new LocationMatcher.WildCard<>(true));
        });

        var map = getNamespaceMap(prefixes, exacts);
        parts_materialsMatcher = new LocationMatcher.Rule<>(map);
    }

    private static @NotNull LocationMatcher<Boolean> matcherFrom(LocationRule rule) {
        final HashMap<String, @NotNull List<Pair<String, Boolean>>> prefixes = rule.prefixes.stream()
                .map(location -> Pair.of(
                        location.getNamespace(),
                        Pair.of(location.getPath(), true))
                )
                .collect(Collectors.groupingBy(
                        p -> p.first,
                        HashMap::new,
                        Collectors.mapping(p -> p.second, Collectors.toList())
                ));
        final HashMap<String, @NotNull Map<String, Boolean>> exacts = rule.prefixes.stream()
                .map(location -> Pair.of(
                        location.getNamespace(),
                        Pair.of(location.getPath(), true))
                )
                .collect(Collectors.groupingBy(
                        p -> p.first,
                        HashMap::new,
                        Collectors.mapping(p -> p.second, Collectors.toMap(
                                p -> p.first,
                                p -> p.second
                        ))
                ));
        var map = getNamespaceMap(prefixes, exacts);
        return new LocationMatcher.Rule<>(map);
    }

    private static <T> @NotNull Map<String, LocationMatcher.RuleEntry<T>> getNamespaceMap(
            @NotNull Map<String, @NotNull List<Pair<String, T>>> prefixes,
            @NotNull Map<String, @NotNull Map<String, T>> exacts) {
        return Stream.concat(prefixes.keySet().stream(), exacts.keySet().stream()).distinct().collect(Collectors.toMap(
                ns -> ns,
                ns -> new LocationMatcher.RuleEntry<>(
                        prefixes.getOrDefault(ns, List.of()),
                        exacts.getOrDefault(ns, Map.of())
                )
        ));
    }


    record LocationRule(List<ResourceLocation> prefixes, List<ResourceLocation> exacts) {
    }


    private static abstract class LocationMatcher<T> {
        public abstract Stream<T> Match(ResourceLocation location);

        @AllArgsConstructor
        public static class WildCard<T> extends LocationMatcher<T> {

            private final T value;

            @Override public Stream<T> Match(ResourceLocation location) {
                return Stream.of(value);
            }
        }

        @AllArgsConstructor
        public static class Rule<T> extends LocationMatcher<T> {
            Map<String, RuleEntry<T>> namespaceMap;

            @Override public Stream<T> Match(ResourceLocation location) {
                var entry = namespaceMap.getOrDefault(location.getNamespace(), null);
                if (entry == null) return Stream.of();

                List<T> matches = new ArrayList<>();
                var match = entry.exacts().getOrDefault(location.getPath(), null);
                if (match != null) matches.add(match);

                return Stream.concat(
                        matches.stream(),
                        entry.prefixes.stream()
                                .filter(p -> location.getPath().startsWith(p.first))
                                .map(p -> p.second)
                );
            }
        }

        public record RuleEntry<T>(List<Pair<String, T>> prefixes, Map<String, T> exacts) {
        }
    }
}


