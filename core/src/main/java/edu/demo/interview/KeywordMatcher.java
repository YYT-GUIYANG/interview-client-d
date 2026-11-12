package edu.demo.interview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Literal, case-sensitive matching on the confirmed text. Offsets use Unicode code points. */
public final class KeywordMatcher {
    private KeywordMatcher() {}

    public static final class Keyword {
        public final String id;
        public final String value;
        public Keyword(String id, String value) {
            if (id == null || id.isEmpty() || value == null || value.isEmpty())
                throw new IllegalArgumentException("empty keyword");
            this.id = id;
            this.value = value;
        }
    }

    public static final class Hit {
        public final String keywordId;
        public final String keyword;
        public final int start;
        public final int end;
        public Hit(String keywordId, String keyword, int start, int end) {
            this.keywordId = keywordId;
            this.keyword = keyword;
            this.start = start;
            this.end = end;
        }
    }

    public static List<Hit> match(String text, List<Keyword> keywords) {
        if (text == null || keywords == null) throw new IllegalArgumentException("null input");
        List<Hit> hits = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Keyword keyword : keywords) {
            if (keyword == null) throw new IllegalArgumentException("null keyword");
            int from = 0;
            while (from < text.length()) {
                int utf16Start = text.indexOf(keyword.value, from);
                if (utf16Start < 0) break;
                int utf16End = utf16Start + keyword.value.length();
                int start = text.codePointCount(0, utf16Start);
                int end = text.codePointCount(0, utf16End);
                String key = keyword.id + "\u0000" + start + "\u0000" + end;
                if (seen.add(key)) hits.add(new Hit(keyword.id, keyword.value, start, end));
                from = text.offsetByCodePoints(utf16Start, 1); // overlapping occurrences count
            }
        }
        hits.sort(Comparator.comparingInt((Hit h) -> h.start)
                .thenComparingInt(h -> h.end).thenComparing(h -> h.keywordId));
        return Collections.unmodifiableList(hits);
    }

    /** Convert matching positions to indices usable by Android SpannableString. */
    public static int utf16Index(String text, int codePointIndex) {
        if (codePointIndex < 0 || codePointIndex > text.codePointCount(0, text.length()))
            throw new IndexOutOfBoundsException("code point index");
        return text.offsetByCodePoints(0, codePointIndex);
    }
}
