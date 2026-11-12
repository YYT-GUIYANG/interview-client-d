package edu.demo.interview.app;

import edu.demo.interview.RecognitionAdapter;
import edu.demo.interview.RecognitionEvent;

/** Contract-only demonstration. No microphone or model inference occurs. */
public final class DemoAdapter implements RecognitionAdapter {
    private Listener listener;
    private String answerId;
    @Override public void start(String id, Listener callback) {
        answerId = id;
        listener = callback;
        listener.onReady();
        listener.onEvent(new RecognitionEvent(id, "s1", 1, "政务", false, 0, 8000));
        listener.onEvent(new RecognitionEvent(id, "s1", 2, "政务服务", false, 0, 8000));
    }
    @Override public void feedAudio(short[] pcm16, int count, long sampleStart, long time) { }
    @Override public void finish() {
        listener.onEvent(new RecognitionEvent(answerId, "s1", 3, "政务服务需要兼顾老年人。", true, 0, 16000));
        listener.onFinished(true, "模拟事件结束");
    }
    @Override public void cancel() { if (listener != null) listener.onFinished(false, "已取消"); }
}
