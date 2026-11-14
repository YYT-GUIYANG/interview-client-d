package edu.demo.interview.app;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import edu.demo.interview.RecognitionAdapter;
import edu.demo.interview.RecognitionEvent;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
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
    private final OkHttpClient client;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final String url;
    private final String bearerToken;
    private final Map<String, Long> revisions = new HashMap<>();
    private WebSocket socket;
    private Listener listener;
    private String answerId;
    private boolean ready;
    private boolean terminal;
    private boolean completeSignal;
    private boolean finishSent;

    public RemoteAdapter(String url) {
        this(url, null);
    }

    /**
     * @param bearerToken optional Bearer token for B's LAN adapter; empty skips the header
     *                    (loopback lab only). Lab builds trust the course self-signed cert.
     */
    public RemoteAdapter(String url, String bearerToken) {
        if (url == null || !url.startsWith("wss://"))
            throw new IllegalArgumentException("请使用 wss:// 地址");
        this.url = url;
        this.bearerToken = bearerToken == null ? "" : bearerToken.trim();
        this.client = labClient();
    }

    /** Controlled-lab TrustManager for course self-signed WSS. Not for production. */
    private static OkHttpClient labClient() {
        try {
            X509TrustManager trustAll = new X509TrustManager() {
                @Override public void checkClientTrusted(X509Certificate[] chain, String authType) { }
                @Override public void checkServerTrusted(X509Certificate[] chain, String authType) { }
                @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            };
            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(null, new TrustManager[]{trustAll}, new SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(ssl.getSocketFactory(), trustAll)
                    .hostnameVerifier((hostname, session) -> true)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("无法初始化联调 TLS", e);
        }
    }

    @Override public synchronized void start(String id, Listener callback) {
        answerId = id;
        listener = callback;
        Request.Builder builder = new Request.Builder().url(url);
        if (!bearerToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        Request request = builder.build();
        socket = client.newWebSocket(request, new WebSocketListener() {
            @Override public void onOpen(WebSocket ws, Response response) {
                synchronized (RemoteAdapter.this) {
                    if (terminal) { ws.cancel(); return; }
                    // B's adapter accepts this control frame before any audio bytes.
                    boolean sent;
                    try { sent = ws.send(new JSONObject().put("answer_id", answerId).toString()); }
                    catch (Exception e) { sent = false; }
                    if (!sent) {
                        finishCallback(false, "回答号发送失败");
                        ws.cancel();
                        return;
                    }
                    ready = true;
                }
                callback.onReady();
            }
            @Override public void onMessage(WebSocket ws, String message) {
                try {
                    synchronized (RemoteAdapter.this) { if (terminal) return; }
                    JSONObject data = new JSONObject(message);
                    if (data.has("answer_id") && !answerId.equals(data.getString("answer_id"))) {
                        callback.onError("WRONG_ANSWER", "服务端回答号不一致");
                        return;
                    }
                    if (data.optBoolean("answer_complete", false)) {
                        synchronized (RemoteAdapter.this) {
                            if (!finishSent) {
                                callback.onError("EARLY_COMPLETE", "停止前收到完成信号");
                                finishCallback(false, "服务端完成信号过早");
                                ws.cancel();
                                return;
                            }
                        }
                        synchronized (RemoteAdapter.this) { completeSignal = true; }
                        finishCallback(true, "服务端确认结束");
                        ws.close(1000, "complete");
                        return;
                    }
                    if (data.has("error_code") || data.has("error")) {
                        String code = data.optString("error_code", data.optString("error", "REMOTE_ERROR"));
                        callback.onError(code, "服务端报告错误");
                        finishCallback(false, "服务端未完成：" + code);
                        ws.cancel();
                        return;
                    }
                    if (data.has("answer_complete")) {
                        finishCallback(false, "服务端明确标记未完成");
                        ws.cancel();
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
        if (socket.queueSize() + (long) count * 4 > MAX_QUEUE_BYTES) {
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
            socket.cancel();
        }
        // Sample range and elapsed time are supplied by capture for later telemetry integration.
    }

    @Override public synchronized void finish() {
        if (finishSent || terminal) return;
        if (!ready || terminal || socket == null) {
            finishCallback(false, "未建立连接");
            return;
        }
        finishSent = true;
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
