package edu.demo.interview;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class CoreCheck {
    private static void check(boolean ok, String label) {
        if (!ok) throw new AssertionError(label);
    }
    private static KeywordMatcher.Keyword word(String id, String value) {
        return new KeywordMatcher.Keyword(id, value);
    }
    public static void main(String[] args) {
        List<KeywordMatcher.Keyword> words = Arrays.asList(word("k1", "政务服务"));
        check(KeywordMatcher.match("完善政务服务。", words).get(0).start == 2, "F01");
        check(KeywordMatcher.match("政务服务需要改进，政务服务应当便民。", words).size() == 2, "F02");
        check(KeywordMatcher.match("改善办事体验。", words).isEmpty(), "F03");
        check(KeywordMatcher.match("避免数字，鸿沟。", Arrays.asList(word("k2", "数字鸿沟"))).isEmpty(), "F07");
        List<KeywordMatcher.Hit> overlap = KeywordMatcher.match("政务服务", Arrays.asList(word("k3", "服务"), words.get(0)));
        check(overlap.size() == 2 && overlap.get(0).start == 0 && overlap.get(1).start == 2, "F08");
        check(KeywordMatcher.match("😀政务服务。", words).get(0).start == 1, "F09");
        check(KeywordMatcher.utf16Index("😀政务服务。", 1) == 2, "UTF-16 conversion");
        check(KeywordMatcher.match("哈哈哈", Arrays.asList(word("k4", "哈哈"))).size() == 2, "F10");
        check(KeywordMatcher.match("", words).isEmpty(), "F11");
        check(KeywordMatcher.match("政务服务", Collections.emptyList()).isEmpty(), "F12");
        AnswerSession corrected = new AnswerSession("a2", words);
        corrected.apply(new RecognitionEvent("a2", "s1", 1, "政务服务", false, 0, 100));
        check(corrected.hits().isEmpty(), "F05 partial ignored");
        corrected.apply(new RecognitionEvent("a2", "s1", 2, "改善办事体验。", true, 0, 100));
        check(corrected.hits().isEmpty(), "F05 corrected final");

        AnswerSession session = new AnswerSession("a1", Arrays.asList(word("k2", "数字鸿沟")));
        check(session.apply(new RecognitionEvent("a1", "s1", 1, "避免数字", false, 0, 8000)), "partial");
        check(session.hits().isEmpty(), "no partial matches");
        check(!session.apply(new RecognitionEvent("a1", "s1", 0, "乱序", false, 0, 8000)), "old revision");
        check(session.apply(new RecognitionEvent("a1", "s1", 2, "避免数字", true, 0, 8000)), "final 1");
        check(session.apply(new RecognitionEvent("a1", "s2", 1, "鸿沟。", true, 8000, 16000)), "final 2");
        check(session.finalText().equals("避免数字鸿沟。") && session.hits().get(0).start == 2, "F04");
        check(!session.apply(new RecognitionEvent("a1", "s2", 1, "鸿沟。", true, 8000, 16000)), "F06 duplicate");
        check(!session.apply(new RecognitionEvent("a1", "s2", 3, "临时", false, 8000, 16000)), "cannot reopen final");
        check(session.apply(new RecognitionEvent("a1", "s2", 4, "鸿沟！", true, 8000, 16000)), "final correction");
        session.finishing();
        session.complete();
        check(session.status() == AnswerSession.Status.COMPLETE, "complete");
        AnswerSession numeric = new AnswerSession("a3", Collections.emptyList());
        numeric.apply(new RecognitionEvent("a3", "10", 1, "十", true, -1, -1));
        numeric.apply(new RecognitionEvent("a3", "2", 1, "二", true, -1, -1));
        check(numeric.finalText().equals("二十"), "numeric segment order");
        System.out.println("CoreCheck: all checks passed");
    }
}
