/**
 * MIT License
 *
 * Copyright (c) 2021 TriumphTeam
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.thienbao860.expansion.world;

import com.google.common.primitives.Ints;
import org.bukkit.Bukkit;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for detecting server version for legacy support :(
 */
public final class VersionHelper {

    private static final int V_1_21_5 = 1215;

    private static final int V1_21_0 = 1210;

    private static final int V1_16_5 = 1165;

    private static final int CURRENT_VERSION = getCurrentVersion();

    /**
     * Since 1.21.0, biomes are no longer enums, but keyed
     */
    public static final boolean BIOMES_ARE_KEYED = CURRENT_VERSION >= V1_21_0;

    /**
     * World#getGameTime was added in 1.16.5 to both Spigot and Paper
     */
    public static final boolean HAS_WORLD_GAMETIME_METHOD = CURRENT_VERSION >= V1_16_5;

    public static final boolean IS_PAPER = checkPaper();

    /**
     * Check if the server has access to the Paper API
     * Taken from <a href="https://github.com/PaperMC/PaperLib">PaperLib</a>
     *
     * @return True if on Paper server (or forks), false anything else
     */
    private static boolean checkPaper() {
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    /**
     * Gets the current server version
     *
     * @return A protocol like number representing the version, for example, 1.16.5 -> 1165
     */
    private static int getCurrentVersion() {
        // No need to cache since will only run once
        final Matcher matcher = Pattern.compile("(?<version>\\d+\\.\\d+)(?<patch>\\.\\d+)?").matcher(Bukkit.getBukkitVersion());

        final StringBuilder stringBuilder = new StringBuilder();
        if (matcher.find()) {
            stringBuilder.append(matcher.group("version").replace(".", ""));
            final String patch = matcher.group("patch");
            if (patch == null) stringBuilder.append("0");
            else stringBuilder.append(patch.replace(".", ""));
        }

        final Integer version = Ints.tryParse(stringBuilder.toString());

        // Should never fail
        if (version == null) {
            // Instead of failing, just assume latest version
            return V_1_21_5;
        }

        return version;
    }
}