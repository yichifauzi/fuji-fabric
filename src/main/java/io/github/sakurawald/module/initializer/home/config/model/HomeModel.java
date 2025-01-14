package io.github.sakurawald.module.initializer.home.config.model;

import io.github.sakurawald.core.structure.Position;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class HomeModel {

    public @NotNull Map<String, Map<String, Position>> homes = new HashMap<>();
}
