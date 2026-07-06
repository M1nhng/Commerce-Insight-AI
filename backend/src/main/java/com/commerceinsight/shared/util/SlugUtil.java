package com.commerceinsight.shared.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * SlugUtil — utility for generating URL-friendly slugs from text.
 *
 * <p>Example: "Wireless Headphones Pro!" → "wireless-headphones-pro"
 *
 * <p>Architecture Rule: This is a stateless utility class.
 * Use static methods directly — do not inject as a bean.
 */
public final class SlugUtil {

    private static final Pattern NON_ASCII = Pattern.compile("[^\\p{ASCII}]");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9\\s-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-{2,}");
    private static final Pattern LEADING_TRAILING_HYPHENS = Pattern.compile("^-|-$");

    private SlugUtil() {
        // Utility class — prevent instantiation
    }

    /**
     * Generate a URL-safe slug from the given input string.
     *
     * @param input the raw string (e.g., a category name or product name)
     * @return a lowercase, hyphen-separated slug
     */
    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        // 1. Normalize Unicode characters (NFD decomposition)
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);

        // 2. Strip non-ASCII characters
        String ascii = NON_ASCII.matcher(normalized).replaceAll("");

        // 3. Lowercase
        String lower = ascii.toLowerCase(Locale.ROOT);

        // 4. Remove characters that are not alphanumeric, space, or hyphen
        String cleaned = NON_ALPHANUMERIC.matcher(lower).replaceAll("");

        // 5. Replace whitespace runs with a single hyphen
        String hyphenated = WHITESPACE.matcher(cleaned).replaceAll("-");

        // 6. Collapse multiple consecutive hyphens
        String collapsed = MULTIPLE_HYPHENS.matcher(hyphenated).replaceAll("-");

        // 7. Strip leading and trailing hyphens
        return LEADING_TRAILING_HYPHENS.matcher(collapsed).replaceAll("");
    }

    /**
     * Generate a unique slug by appending a suffix if the base slug is taken.
     *
     * @param base   the desired base slug
     * @param suffix a numeric suffix to append (e.g., 2 → "my-slug-2")
     * @return the suffixed slug
     */
    public static String slugifyWithSuffix(String base, int suffix) {
        String baseSlug = slugify(base);
        return baseSlug + "-" + suffix;
    }
}
