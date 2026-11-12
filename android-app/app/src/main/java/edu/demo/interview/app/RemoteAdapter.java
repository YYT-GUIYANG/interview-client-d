package edu.demo.interview.app;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import edu.demo.interview.RecognitionAdapter;
import edu.demo.interview.RecognitionEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.json.JSONObject;

/** Compatible with the official float32/Done input; final semantics need B's adapter. */
public final class RemoteAdapter implements RecognitionAdapter {
    private static final long MAX_QUEUE_BYTES = 2L * 1024 * 1024;
    private final OkHttpClient client = new OkHttpClient.Builder().build();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final String url;
    private final Map<String, Long> revisions = new HashMap<>();
    private WebSocket socket;
    private Listener listener;
    private String answerId;
    private boolean ready;
    private boolean terminal;
    private boolean completeSignal;

    public RemoteAdapter(String url) {
        if (url == null || !url.startsWith("wss://"))
            throw new IllegalArgumentException("请使用 wss:// 地址");
        this.url = url;
    }

    @Override public synchronized void start(String id, Listener callback) {
        answerId = id;
        listener = callback;
        Request request = new Request.Builder().url(url).build();
        socket = client.newWebSocket(request, new WebSocketListener() {
            @Override public void onOpen(WebSocket ws, Response response) {
                synchronized (RemoteAdapter.this) {
                    if (terminal) { ws.cancel(); return; }
                    ready = true;
                }
                callback.onReady();
            }
            @Override public void onMessage(WebSocket ws, String message) {
                try {
                    JSONObject data = new JSONObject(message);
                    if (data.has("answer_id") && !answerId.equals(data.getString("answer_id"))) {
                        callback.onError("WRONG_ANSWER", "服务端回答号不一致");
                        return;
                    }
                    if (data.optBoolean("answer_complete", false)) {
                        synchronized (RemoteAdapter.this) { completeSignal = true; }
                        finishCallback(true, "服务端确认结束");
                        ws.close(1000, "complete");
                        return;
                    }
                    String text = data.optString("text", "");
                    String segment = data.has("segment_id") ? data.getString("segment_id")
                            : Integer.toString(data.optInt("segment", 0));
                    boolean isFinal = data.optBoolean("is_final", false);
                    long revision;
                    synchronized (RemoteAdapter.this) {
                        revision = data.has("revision") ? data.getLong("revision")
                                : revisions.getOrDefault(segment, 0L) + 1;
                        revisions.put(segment, Math.max(revision, revisions.getOrDefault(segment, 0L)));
                    }
                    // The official server has no source range. -1 signals unmapped data.
                    long sampleStart = data.optLong("sample_start", -1);
                    long sampleEnd = data.optLong("sample_end", -1);
                    callback.onEvent(new RecognitionEvent(answerId, segment, revision, text,
                            isFinal, sampleStart, sampleEnd, SystemClock.elapsedRealtimeNanos()));
                } catch (Exception e) {
                    callback.onError("BAD_EVENT", e.getMessage());
                }
            }
            @Override public void onFailure(WebSocket ws, Throwable cause, Response response) {
                finishCallback(false, "连接失败：" + cause.getMessage());
            }
            @Override public void onClosed(WebSocket ws, int code, String reason) {
                finishCallback(completeSignal, completeSignal ? "完成" : "连接关闭，缺少最终确认");
            }
        });
    }

    @Override public synchronized void feedAudio(short[] pcm16, int count, long sampleStart,
                                                   long capturedAtElapsedNanos) {
        if (!ready || terminal || socket == null) return;
        if (count <= 0 || count > pcm16.length) throw new IllegalArgumentException("sample count");
        if (socket.queueSize() > MAX_QUEUE_BYTES) {
            listener.onError("QUEUE_FULL", "发送队列超过 2 MiB");
            finishCallback(false, "网络积压，回答未完成");
            socket.cancel();
            return;
        }
        ByteBuffer bytes = ByteBuffer.allocate(count * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < count; i++) bytes.putFloat(pcm16[i] / 32768f);
        if (!socket.send(ByteString.of(bytes.array()))) {
            listener.onError("SEND_FAILED", "音频发送失败");
            finishCallback(false, "发送失败，回答未完成");
        }
        // Sample range and elapsed time are supplied by capture for later telemetry integration.
    }

    @Override public synchronized void finish() {
        if (!ready || terminal || socket == null) {
            finishCallback(false, "未建立连接");
            return;
        }
        if (!socket.send("Done")) {
            finishCallback(false, "结束信号发送失败");
            return;
        }
        handler.postDelayed(() -> {
            synchronized (RemoteAdapter.this) {
                if (!terminal) {
                    finishCallback(false, "等待最终确认超时");
                    socket.cancel();
                }
            }
        }, 5000);
    }

    @Override public synchronized void cancel() {
        if (socket != null) socket.cancel();
        finishCallback(false, "已取消");
    }

    private synchronized void finishCallback(boolean complete, String detail) {
        if (terminal) return;
        terminal = true;
        if (listener != null) listener.onFinished(complete, detail);
    }
}
