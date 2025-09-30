package com.tisawesomeness.diamondnuggets;

import java.util.Comparator;

public record PackFormat(int major, int minor) implements Comparable<PackFormat> {

    private static final Comparator<PackFormat> COMPARATOR = Comparator
            .comparingInt((PackFormat p) -> p.major)
            .thenComparingInt(p -> p.minor);

    @Override
    public int compareTo(PackFormat packFormat) {
        return COMPARATOR.compare(this, packFormat);
    }

    @Override
    public String toString() {
        if (minor == 0) {
            return String.valueOf(major);
        }
        return major + "." + minor;
    }

}
