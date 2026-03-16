package cd.senat.medical.util;

import java.util.Locale;

public final class Texts {
    private Texts() {}
    public static String up(String s) {
        return s == null ? null : s.trim().toUpperCase(Locale.ROOT);
    }
}
