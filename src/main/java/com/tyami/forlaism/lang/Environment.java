package com.tyami.forlaism.lang;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class Environment {

    private final Environment parent;
    private final Map<String, Double> numbers = new HashMap<>();
    private final Map<String, Vec3> vectors = new HashMap<>();

    public Environment(Environment parent) {
        this.parent = parent;
    }

    public double get(String name) {
        if (numbers.containsKey(name)) return numbers.get(name);
        if (vectors.containsKey(name)) throw new LangException(
                "forlaism.lang.error.not_number", name);
        if (parent != null) return parent.get(name);
        throw new LangException("forlaism.lang.error.unknown_variable", name);
    }

    public Vec3 getVec(String name) {
        if (vectors.containsKey(name)) return vectors.get(name);
        if (numbers.containsKey(name)) throw new LangException(
                "forlaism.lang.error.not_vector", name);
        if (parent != null) return parent.getVec(name);
        throw new LangException("forlaism.lang.error.unknown_variable", name);
    }

    public void set(String name, double value) {
        numbers.put(name, value);
        vectors.remove(name);
    }

    public void setVec(String name, Vec3 value) {
        vectors.put(name, value);
        numbers.remove(name);
    }

    public boolean hasOwn(String name) {
        return numbers.containsKey(name) || vectors.containsKey(name);
    }

    public boolean isVector(String name) {
        if (vectors.containsKey(name)) return true;
        if (numbers.containsKey(name)) return false;
        return parent != null && parent.isVector(name);
    }

    public Environment parent() { return parent; }

    public Map<String, Double> snapshot() { return new HashMap<>(numbers); }
}