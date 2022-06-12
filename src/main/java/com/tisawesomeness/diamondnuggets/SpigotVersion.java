package com.tisawesomeness.diamondnuggets;

import org.bukkit.Bukkit;

public enum SpigotVersion {
    V1_16_5("1.16.5", 6),
    V1_17_1("1.17.1", 7),
    V1_18_2("1.18.2", 8),
    V1_19("1.19", 9);

    private final String version;
    public final int packFormat;
    SpigotVersion(String version, int packFormat) {
        this.version = version;
        this.packFormat = packFormat;
    }

    public static final SpigotVersion SERVER_VERSION = initVersion();

    private static SpigotVersion initVersion() {
        String bukkitVersion = Bukkit.getServer().getBukkitVersion();
        SpigotVersion version = from(bukkitVersion);
        if (version == null) {
            throw new IllegalStateException("Unsupported Spigot version: " + bukkitVersion);
        }
        return version;
    }
    public static SpigotVersion from(String version) {
        String shortVersion = version.substring(0, version.indexOf('-'));
        for (SpigotVersion v : values()) {
            if (shortVersion.equalsIgnoreCase(v.version)) {
                return v;
            }
        }
        return null;
    }

}
