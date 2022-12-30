package com.tisawesomeness.diamondnuggets;

import org.bukkit.Bukkit;

import java.util.Comparator;

public enum SpigotVersion {
    V1_16_2("1.16.2", 6),
    V1_17("1.17", 7),
    V1_18("1.18", 8),
    V1_19("1.19", 9),
    V1_19_3("1.19.3", 12);

    private final Version version;
    public final int packFormat;
    SpigotVersion(String version, int packFormat) {
        this.version = Version.parse(version);
        this.packFormat = packFormat;
    }

    public static final SpigotVersion SERVER_VERSION = initVersion();

    private static SpigotVersion initVersion() {
        String bukkitVersion = Bukkit.getServer().getBukkitVersion();
        SpigotVersion version = findLatestPackVersion(bukkitVersion);
        if (version == null) {
            throw new IllegalStateException("Unsupported Spigot version: " + bukkitVersion);
        }
        return version;
    }
    public static SpigotVersion findLatestPackVersion(String version) {
        String shortVersion = version.substring(0, version.indexOf('-'));
        Version currentVersion = Version.parse(shortVersion);
        for (int i = values().length - 1; i >= 0; i--) {
            SpigotVersion v = values()[i];
            if (currentVersion.compareTo(v.version) >= 0) {
                return v;
            }
        }
        return null;
    }

    public boolean supportsRecipeBookCategory() {
        return this.compareTo(V1_19_3) >= 0;
    }

    private record Version(int major, int minor, int patch) implements Comparable<Version> {

        private static final Comparator<Version> COMPARATOR = Comparator
                .comparingInt((Version v) -> v.major)
                .thenComparingInt(v -> v.minor)
                .thenComparingInt(v -> v.patch);

        public static Version parse(String str) {
            String[] split = str.split("\\.");
            int major = Integer.parseInt(split[0]);
            int minor = Integer.parseInt(split[1]);
            int patch = split.length > 2 ? Integer.parseInt(split[2]) : 0;
            return new Version(major, minor, patch);
        }

        @Override
        public int compareTo(Version version) {
            return COMPARATOR.compare(this, version);
        }

    }

}
