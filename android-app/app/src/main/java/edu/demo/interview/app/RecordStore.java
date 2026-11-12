package edu.demo.interview.app;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONArray;
import org.json.JSONObject;

/** Encrypted app-private prototype storage. This is not multi-user authentication. */
public final class RecordStore {
    private static final String KEY_ALIAS = "interview_d_records_v1";
    private final File file;
    public RecordStore(Context context) { file = new File(context.getFilesDir(), "records.enc"); }

    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey existing = (SecretKey) store.getKey(KEY_ALIAS, null);
        if (existing != null) return existing;
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return generator.generateKey();
    }

    public synchronized JSONArray read() throws Exception {
        if (!file.exists()) return new JSONArray();
        byte[] data = Files.readAllBytes(file.toPath());
        if (data.length < 13) throw new IllegalStateException("记录文件损坏");
        byte[] iv = new byte[12];
        System.arraycopy(data, 0, iv, 0, iv.length);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, iv));
        byte[] plaintext = cipher.doFinal(data, iv.length, data.length - iv.length);
        return new JSONArray(new String(plaintext, StandardCharsets.UTF_8));
    }

    private void write(JSONArray records) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        byte[] iv = cipher.getIV();
        byte[] encrypted = cipher.doFinal(records.toString().getBytes(StandardCharsets.UTF_8));
        byte[] data = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, data, 0, iv.length);
        System.arraycopy(encrypted, 0, data, iv.length, encrypted.length);
        File temporary = new File(file.getParentFile(), "records.tmp");
        Files.write(temporary.toPath(), data);
        if (!temporary.renameTo(file)) throw new IllegalStateException("记录文件替换失败");
    }

    public synchronized void save(JSONObject record) throws Exception {
        JSONArray old = read();
        JSONArray next = new JSONArray();
        for (int i = 0; i < old.length(); i++) {
            JSONObject item = old.getJSONObject(i);
            if (!item.getString("answer_id").equals(record.getString("answer_id"))) next.put(item);
        }
        next.put(record);
        write(next);
    }

    public synchronized void delete(String answerId) throws Exception {
        JSONArray old = read();
        JSONArray next = new JSONArray();
        for (int i = 0; i < old.length(); i++) {
            JSONObject item = old.getJSONObject(i);
            if (!item.getString("answer_id").equals(answerId)) next.put(item);
        }
        write(next);
    }

    /** Filter by account_id through IdentityGateway. Pending C's frozen ownership fields. */
    public synchronized JSONArray readForAccount(String accountId, IdentityGateway gateway)
            throws Exception {
        JSONArray all = read();
        JSONArray mine = new JSONArray();
        for (int i = 0; i < all.length(); i++) {
            JSONObject item = all.getJSONObject(i);
            String owner = item.optString("account_id", "");
            if (owner.isEmpty()) continue; // legacy unowned prototype rows are hidden
            if (gateway.canAccess(owner) && accountId.equals(owner)) mine.put(item);
        }
        return mine;
    }

    public synchronized void deleteForAccount(String answerId, String accountId,
                                               IdentityGateway gateway) throws Exception {
        JSONArray old = read();
        JSONArray next = new JSONArray();
        boolean removed = false;
        for (int i = 0; i < old.length(); i++) {
            JSONObject item = old.getJSONObject(i);
            if (item.getString("answer_id").equals(answerId)) {
                String owner = item.optString("account_id", "");
                if (!accountId.equals(owner) || !gateway.canAccess(owner))
                    throw new SecurityException("无权删除他人记录或未通过身份约束");
                removed = true;
                continue;
            }
            next.put(item);
        }
        if (!removed) throw new IllegalArgumentException("记录不存在");
        write(next);
    }
}
