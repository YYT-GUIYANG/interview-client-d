package edu.demo.interview;

/** One engine update. sampleEnd is exclusive; -1 means the engine cannot map it yet. */
public final class RecognitionEvent {
    public final String answerId;
    public final String segmentId;
    public final long revision;
    public final String text;
    public final boolean isFinal;
    public final long sampleStart;
    public final long sampleEnd;
    public final long receivedAtElapsedNanos;

    public RecognitionEvent(String answerId, String segmentId, long revision, String text,
                            boolean isFinal, long sampleStart, long sampleEnd) {
        this(answerId, segmentId, revision, text, isFinal, sampleStart, sampleEnd, -1);
    }

    public RecognitionEvent(String answerId, String segmentId, long revision, String text,
                            boolean isFinal, long sampleStart, long sampleEnd,
                            long receivedAtElapsedNanos) {
        if (answerId == null || answerId.isEmpty() || segmentId == null || segmentId.isEmpty()
                || revision < 0 || text == null || receivedAtElapsedNanos < -1) {
            throw new IllegalArgumentException("invalid recognition event");
        }
        this.answerId = answerId;
        this.segmentId = segmentId;
        this.revision = revision;
        this.text = text;
        this.isFinal = isFinal;
        this.sampleStart = sampleStart;
        this.sampleEnd = sampleEnd;
        this.receivedAtElapsedNanos = receivedAtElapsedNanos;
    }
}
