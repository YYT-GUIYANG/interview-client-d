package edu.demo.interview.app;

import java.util.Collections;
import java.util.List;

/**
 * Extension boundary for fluency analysis (D10). Word-level timestamps are optional;
 * do not infer long pauses from trimmed audio length alone.
 */
public final class AnalysisPort {
    public static final class VadInterval {
        public final long sampleStart;
        public final long sampleEnd;
        public VadInterval(long sampleStart, long sampleEnd) {
            this.sampleStart = sampleStart;
            this.sampleEnd = sampleEnd;
        }
    }

    public static final class WordTiming {
        public final String token;
        public final float startSec;
        public final float endSec;
        public WordTiming(String token, float startSec, float endSec) {
            this.token = token;
            this.startSec = startSec;
            this.endSec = endSec;
        }
    }

    public static final class AnswerAnalysisInput {
        public final String answerId;
        public final long originalSampleCount;
        public final int sampleRate;
        public final List<VadInterval> vadIntervals;
        public final String finalText;
        /** Empty when word timestamps are unavailable. */
        public final List<WordTiming> wordTimings;
        public final String timingLimitation;

        public AnswerAnalysisInput(
                String answerId,
                long originalSampleCount,
                int sampleRate,
                List<VadInterval> vadIntervals,
                String finalText,
                List<WordTiming> wordTimings,
                String timingLimitation) {
            this.answerId = answerId;
            this.originalSampleCount = originalSampleCount;
            this.sampleRate = sampleRate;
            this.vadIntervals = vadIntervals == null ? Collections.emptyList() : vadIntervals;
            this.finalText = finalText == null ? "" : finalText;
            this.wordTimings = wordTimings == null ? Collections.emptyList() : wordTimings;
            this.timingLimitation = timingLimitation == null ? "" : timingLimitation;
        }
    }

    private AnalysisPort() {}

    public static AnswerAnalysisInput fromSession(
            String answerId, long originalSampleCount, String finalText) {
        return new AnswerAnalysisInput(
                answerId,
                originalSampleCount,
                16000,
                Collections.emptyList(),
                finalText,
                Collections.emptyList(),
                "尚未取得词级时间戳；不得用裁剪后音频长度推算原始长停顿");
    }
}
