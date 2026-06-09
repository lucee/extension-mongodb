package org.lucee.mongodb.cache;

import lucee.commons.io.cache.CacheKeyFilter;
import java.util.regex.Pattern;

public class WildCardFilter implements CacheKeyFilter {

    private static final String SPECIALS = "{}[]().+\\^$";

    private final Pattern pattern;
    private final String wildcard;
    private final boolean ignoreCase;

    public WildCardFilter(String wildcard, boolean ignoreCase) {
        this.wildcard = wildcard;
        this.ignoreCase = ignoreCase;
        StringBuilder sb = new StringBuilder(wildcard.length());
        int len = wildcard.length();
        for (int i = 0; i < len; i++) {
            char c = wildcard.charAt(i);
            if (c == '*') sb.append(".*");
            else if (c == '?') sb.append('.');
            else if (SPECIALS.indexOf(c) != -1) sb.append('\\').append(c);
            else sb.append(c);
        }
        int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
        pattern = Pattern.compile(sb.toString(), flags);
    }

    @Override
    public boolean accept(String key) {
        return pattern.matcher(key).matches();
    }

    @Override
    public String toString() {
        return "WildcardFilter:" + wildcard;
    }

    public String toPattern() {
        return wildcard;
    }
}
