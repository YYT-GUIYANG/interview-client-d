package edu.demo.interview.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import edu.demo.interview.AnswerSession;
import edu.demo.interview.KeywordMatcher;
import edu.demo.interview.RecognitionAdapter;
import edu.demo.interview.RecognitionEvent;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/** Interview client UI. Local uses sherpa-onnx JNI; remote needs B's WSS adapter. */
public final class MainActivity extends Activity {
    private final List<JSONObject> questions = new ArrayList<>();
    private Spinner questionChoice;
    private Spinner pathChoice;
    private EditText serverAddress;
    private EditText bearerTokenInput;
    private EditText accountField;
    private TextView questionText;
    private TextView statusView;
    private TextView finalView;
    private TextView liveView;
    private TextView hitView;
    private TextView identityView;
    private Button startButton;
    private Button stopButton;
    private Button saveButton;
    private AnswerSession session;
    private RecognitionAdapter adapter;
    private PcmRecorder recorder;
    private RecordStore store;
    private final IdentityGateway identity = new LocalTestIdentityGateway();
    private String answerId;
    private String questionId;
    private String recordAccountId;
    private String path;
    private long startElapsedNanos;
    private long totalSamples;
    private JSONArray trace = new JSONArray();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store = new RecordStore(this);
        identity.setLocalTestIdentity("local-test-user", "本机测试");
        try { loadQuestions(); } catch (Exception e) { throw new IllegalStateException("题目配置读取失败", e); }
        buildUi();
    }

    private void loadQuestions() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] block = new byte[4096];
        try (java.io.InputStream in = getAssets().open("question_config.json")) {
            for (int n; (n = in.read(block)) != -1;) out.write(block, 0, n);
        }
        JSONArray array = new JSONObject(out.toString("UTF-8")).getJSONArray("questions");
        for (int i = 0; i < array.length(); i++) questions.add(array.getJSONObject(i));
    }

    private TextView label(LinearLayout layout, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(17);
        view.setPadding(8, 12, 8, 8);
        layout.addView(view);
        return view;
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 24, 24, 24);
        scroll.addView(root);
        setContentView(scroll);
        label(root, "面试回答客户端 · D");
        identityView = label(root, "");
        refreshIdentityLabel();
        accountField = new EditText(this);
        accountField.setSingleLine(true);
        accountField.setHint("本机测试账号（非 C 正式认证）");
        accountField.setText("local-test-user");
        root.addView(accountField);
        Button applyIdentity = new Button(this);
        applyIdentity.setText("应用本机测试身份");
        applyIdentity.setOnClickListener(v -> {
            try {
                identity.setLocalTestIdentity(accountField.getText().toString().trim(), "本机测试");
                refreshIdentityLabel();
            } catch (Exception e) {
                statusView.setText("身份设置失败：" + e.getMessage());
            }
        });
        root.addView(applyIdentity);
        questionChoice = new Spinner(this);
        List<String> names = new ArrayList<>();
        for (JSONObject q : questions) names.add(q.optString("question_id") + " · " + q.optString("title"));
        questionChoice.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, names));
        root.addView(questionChoice);
        questionText = label(root, "");
        questionChoice.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                questionText.setText(questions.get(position).optString("question"));
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
        pathChoice = new Spinner(this);
        pathChoice.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item,
                new String[]{"模拟结果（无识别）", "远程 WSS（需 B 适配服务）", "本地 sherpa-onnx（真实模型）"}));
        root.addView(pathChoice);
        serverAddress = new EditText(this);
        serverAddress.setSingleLine(true);
        serverAddress.setHint("wss://电脑局域网IP:6007");
        root.addView(serverAddress);
        bearerTokenInput = new EditText(this);
        bearerTokenInput.setSingleLine(true);
        bearerTokenInput.setHint("远程 Bearer 令牌（与 B 一致；勿截进公开仓库）");
        root.addView(bearerTokenInput);
        startButton = new Button(this);
        startButton.setText("开始回答");
        root.addView(startButton);
        stopButton = new Button(this);
        stopButton.setText("停止并收尾");
        stopButton.setEnabled(false);
        root.addView(stopButton);
        saveButton = new Button(this);
        saveButton.setText("保存到本机测试档案");
        saveButton.setEnabled(false);
        root.addView(saveButton);
        Button records = new Button(this);
        records.setText("查看 / 删除测试档案");
        root.addView(records);
        statusView = label(root, "状态：待开始");
        label(root, "最终文本（黄色为关键词）：");
        finalView = label(root, "");
        label(root, "临时展示：");
        liveView = label(root, "");
        hitView = label(root, "命中：0");
        startButton.setOnClickListener(v -> startAnswer());
        stopButton.setOnClickListener(v -> stopAnswer());
        saveButton.setOnClickListener(v -> saveRecord());
        records.setOnClickListener(v -> showRecords());
    }

    private void refreshIdentityLabel() {
        IdentityGateway.Identity id = identity.current().orElse(null);
        if (id == null) {
            identityView.setText("身份：未设置。C 的正式账号校验待接入，不可用于真实考生数据。");
        } else {
            identityView.setText("身份：本机测试账号 " + id.accountId
                    + "（verified=" + id.verified + "；非正式多用户认证）");
        }
    }

    private void startAnswer() {
        int mode = pathChoice.getSelectedItemPosition();
        if ((mode == 1 || mode == 2)
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 101);
            return;
        }
        if (!identity.current().isPresent()) {
            statusView.setText("请先设置本机测试身份");
            return;
        }
        try {
            JSONObject question = questions.get(questionChoice.getSelectedItemPosition());
            questionId = question.getString("question_id");
            JSONArray source = question.getJSONArray("keywords");
            List<KeywordMatcher.Keyword> keywords = new ArrayList<>();
            for (int i = 0; i < source.length(); i++) {
                JSONObject entry = source.getJSONObject(i);
                keywords.add(new KeywordMatcher.Keyword(entry.getString("keyword_id"), entry.getString("keyword")));
            }
            answerId = UUID.randomUUID().toString();
            recordAccountId = identity.current().get().accountId;
            trace = new JSONArray();
            totalSamples = 0;
            session = new AnswerSession(answerId, keywords);
            path = mode == 0 ? "demo" : (mode == 1 ? "remote" : "local");
            if (mode == 0) adapter = new DemoAdapter();
            else if (mode == 1) adapter = new RemoteAdapter(
                    serverAddress.getText().toString().trim(),
                    bearerTokenInput.getText().toString().trim());
            else adapter = new LocalSherpaAdapter(this);
            startElapsedNanos = SystemClock.elapsedRealtimeNanos();
            startButton.setEnabled(false);
            questionChoice.setEnabled(false);
            pathChoice.setEnabled(false);
            stopButton.setEnabled(true);
            saveButton.setEnabled(false);
            finalView.setText("");
            liveView.setText("");
            statusView.setText("状态：连接中 / 启动中 · " + path);
            adapter.start(answerId, new RecognitionAdapter.Listener() {
                @Override public void onReady() { runOnUiThread(() -> {
                    if (session == null || session.status() != AnswerSession.Status.RECORDING) return;
                    try { appendTrace(new JSONObject().put("kind", "adapter_ready")
                            .put("elapsed_nanos", SystemClock.elapsedRealtimeNanos())); }
                    catch (Exception ignored) { }
                    statusView.setText("状态：录音中 · " + path);
                    if ("remote".equals(path) || "local".equals(path)) startMicrophone();
                }); }
                @Override public void onEvent(RecognitionEvent event) { runOnUiThread(() -> {
                    if (session != null && session.apply(event)) {
                        render();
                        try {
                            JSONObject log = new JSONObject();
                            log.put("kind", event.isFinal ? "final_display" : "partial_display");
                            log.put("segment_id", event.segmentId);
                            log.put("revision", event.revision);
                            log.put("sample_start", event.sampleStart);
                            log.put("sample_end", event.sampleEnd);
                            log.put("received_elapsed_nanos", event.receivedAtElapsedNanos);
                            log.put("display_elapsed_nanos", SystemClock.elapsedRealtimeNanos());
                            appendTrace(log);
                            if (event.isFinal) {
                                JSONObject keywordLog = new JSONObject();
                                keywordLog.put("kind", "keyword_display");
                                keywordLog.put("segment_id", event.segmentId);
                                keywordLog.put("hit_count", session.hits().size());
                                keywordLog.put("display_elapsed_nanos", SystemClock.elapsedRealtimeNanos());
                                appendTrace(keywordLog);
                            }
                        } catch (Exception ignored) { }
                    }
                }); }
                @Override public void onFinished(boolean complete, String detail) { runOnUiThread(() -> {
                    if (recorder != null) { recorder.stop(); recorder = null; }
                    if (session != null) {
                        session.finishing();
                        if (complete && !session.finalText().isEmpty()) session.complete();
                        else session.partial();
                        statusView.setText("状态：" + session.status() + " · " + detail);
                        try { appendTrace(new JSONObject().put("kind", "answer_finished")
                                .put("complete", complete).put("detail", detail)
                                .put("elapsed_nanos", SystemClock.elapsedRealtimeNanos())); }
                        catch (Exception ignored) { }
                        saveButton.setEnabled(true);
                    }
                    resetControls();
                }); }
                @Override public void onError(String code, String detail) { runOnUiThread(() -> {
                    statusView.setText("异常 " + code + "：" + detail);
                    try { appendTrace(new JSONObject().put("kind", "error")
                            .put("code", code).put("detail", detail)
                            .put("elapsed_nanos", SystemClock.elapsedRealtimeNanos())); }
                    catch (Exception ignored) { }
                }); }
            });
        } catch (Exception e) {
            statusView.setText("启动失败：" + e.getMessage());
            resetControls();
        }
    }

    private void startMicrophone() {
        recorder = new PcmRecorder();
        try {
            recorder.start(this, new PcmRecorder.Sink() {
                @Override public void onAudio(short[] audio, int count, long sampleStart, long capturedAt) {
                    try {
                        JSONObject log = new JSONObject();
                        log.put("kind", "capture_end");
                        log.put("sample_start", sampleStart);
                        log.put("sample_end", sampleStart + count);
                        log.put("captured_elapsed_nanos", capturedAt);
                        appendTrace(log);
                    } catch (Exception ignored) { }
                    totalSamples = sampleStart + count;
                    adapter.feedAudio(audio, count, sampleStart, capturedAt);
                }
                @Override public void onError(String detail) { runOnUiThread(() -> {
                    statusView.setText("采集失败：" + detail);
                    stopAnswer();
                }); }
            });
        } catch (Exception e) {
            statusView.setText("采集启动失败：" + e.getMessage());
            adapter.cancel();
        }
    }

    private void stopAnswer() {
        stopButton.setEnabled(false);
        statusView.setText("状态：停止采集，等待尾段");
        if (recorder != null) { recorder.stop(); recorder = null; }
        if (session != null) session.finishing();
        if (adapter != null) adapter.finish();
    }

    private void resetControls() {
        startButton.setEnabled(session == null || (session.status() != AnswerSession.Status.COMPLETE
                && session.status() != AnswerSession.Status.PARTIAL));
        stopButton.setEnabled(false);
        questionChoice.setEnabled(true);
        pathChoice.setEnabled(true);
    }

    private void render() {
        String text = session.finalText();
        SpannableString styled = new SpannableString(text);
        for (KeywordMatcher.Hit hit : session.hits()) {
            styled.setSpan(new BackgroundColorSpan(Color.YELLOW),
                    KeywordMatcher.utf16Index(text, hit.start), KeywordMatcher.utf16Index(text, hit.end),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        finalView.setText(styled);
        liveView.setText(session.displayText());
        hitView.setText("命中：" + session.hits().size());
    }

    private void appendTrace(JSONObject entry) {
        synchronized (trace) { trace.put(entry); }
    }

    private void saveRecord() {
        if (session == null) return;
        try {
            JSONObject record = new JSONObject();
            IdentityGateway.Identity id = identity.current().orElseThrow(() -> new IllegalStateException("无身份"));
            if (!recordAccountId.equals(id.accountId) || !identity.canAccess(recordAccountId))
                throw new SecurityException("当前测试身份与开始回答时不一致，请切回原账号保存");
            record.put("answer_id", answerId);
            record.put("account_id", recordAccountId);
            record.put("question_id", questionId);
            record.put("config_version", "1.0");
            record.put("path", path);
            record.put("status", session.status().name());
            record.put("final_text", session.finalText());
            record.put("started_elapsed_nanos", startElapsedNanos);
            record.put("saved_at_epoch_ms", System.currentTimeMillis());
            record.put("analysis_extension", new JSONObject()
                    .put("note", AnalysisPort.fromSession(answerId, totalSamples, session.finalText()).timingLimitation)
                    .put("original_sample_count", totalSamples)
                    .put("sample_rate", 16000));
            synchronized (trace) { record.put("trace", new JSONArray(trace.toString())); }
            store.save(record);
            saveButton.setEnabled(false);
            startButton.setEnabled(true);
            statusView.setText("已保存测试档案 · " + session.status());
        } catch (Exception e) { statusView.setText("保存失败：" + e.getMessage()); }
    }

    private void showRecords() {
        try {
            IdentityGateway.Identity id = identity.current().orElse(null);
            if (id == null) { statusView.setText("请先设置本机测试身份"); return; }
            JSONArray records = store.readForAccount(id.accountId, identity);
            String[] labels = new String[records.length()];
            for (int i = 0; i < records.length(); i++) {
                JSONObject item = records.getJSONObject(i);
                String aid = item.optString("answer_id");
                labels[i] = item.optString("question_id") + " / " + item.optString("status")
                        + " / " + (aid.length() >= 8 ? aid.substring(0, 8) : aid);
            }
            new AlertDialog.Builder(this).setTitle("本人测试档案").setItems(labels, (dialog, which) -> {
                try {
                    JSONObject item = store.readForAccount(id.accountId, identity).getJSONObject(which);
                    new AlertDialog.Builder(this).setTitle(item.optString("question_id"))
                            .setMessage(item.optString("final_text") + "\n状态：" + item.optString("status")
                                    + "\n路径：" + item.optString("path")
                                    + "\n账号：" + item.optString("account_id"))
                            .setPositiveButton("关闭", null)
                            .setNegativeButton("删除", (d, w) -> {
                                try { store.deleteForAccount(item.optString("answer_id"), id.accountId, identity); }
                                catch (Exception e) { statusView.setText("删除失败：" + e.getMessage()); }
                            }).show();
                } catch (Exception e) { statusView.setText("读取失败：" + e.getMessage()); }
            }).setPositiveButton("关闭", null).show();
        } catch (Exception e) { statusView.setText("读取失败：" + e.getMessage()); }
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == 101 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startAnswer();
        else if (code == 101) statusView.setText("录音权限未授予，无法开始本地/远程路径");
    }

    @Override protected void onDestroy() {
        if (recorder != null) recorder.stop();
        if (adapter != null && session != null && (session.status() == AnswerSession.Status.RECORDING
                || session.status() == AnswerSession.Status.FINISHING)) adapter.cancel();
        super.onDestroy();
    }
}
