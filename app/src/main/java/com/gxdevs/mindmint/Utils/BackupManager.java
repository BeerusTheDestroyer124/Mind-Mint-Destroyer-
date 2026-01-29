package com.gxdevs.mindmint.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.gxdevs.mindmint.db.MindMintRoomDatabase;
import com.gxdevs.mindmint.db.entities.DailyStatsEntity;
import com.gxdevs.mindmint.db.entities.FocusDailyStatEntity;
import com.gxdevs.mindmint.db.entities.FocusSessionEntity;
import com.gxdevs.mindmint.db.entities.FocusTopicEntity;
import com.gxdevs.mindmint.db.entities.HabitCachedStatsEntity;
import com.gxdevs.mindmint.db.entities.HabitCompletionEntity;
import com.gxdevs.mindmint.db.entities.HabitEntity;
import com.gxdevs.mindmint.db.entities.TaskEntity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.List;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class BackupManager {

    private static final String TAG = "BackupManager";
    private static final String STATIC_SALT = "MindMintSalt123";
    private static final String STATIC_IV = "MindMintIV123456"; // 16 bytes needed, doing logic below
    private static final String SECRET_KEY = "MindMintBackupKey"; // Hardcoded for simplicity

    public static class BackupData {
        public long timestamp;
        public int version;
        public List<TaskEntity> tasks;
        public List<HabitEntity> habits;
        public List<HabitCompletionEntity> habitCompletions;
        public List<HabitCachedStatsEntity> habitCachedStats;
        public List<FocusDailyStatEntity> focusDailyStats;
        public List<FocusSessionEntity> focusSessions;
        public List<DailyStatsEntity> dailyStats;
        public List<FocusTopicEntity> focusTopics;
        public Map<String, ?> defaultPrefs;
        public Map<String, ?> peaceCoinsPrefs;
    }

    public static void exportData(Context context, Uri uri) throws Exception {
        MindMintRoomDatabase db = MindMintRoomDatabase.getInstance(context);
        BackupData data = new BackupData();
        data.timestamp = System.currentTimeMillis();
        data.version = 1;
        data.tasks = db.taskDao().getAll();
        data.habits = db.habitDao().getAll();
        data.habitCompletions = db.habitCompletionDao().getAll();
        data.habitCachedStats = db.habitCachedStatsDao().getAll();
        data.focusDailyStats = db.focusDao().getAllDailyStats();
        data.focusSessions = db.focusDao().getAllSessions();
        data.dailyStats = db.dailyStatsDao().getAll();
        data.focusTopics = db.focusTopicDao().getAllTopicsSync();
        data.defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context).getAll();
        data.peaceCoinsPrefs = context.getSharedPreferences("PeaceCoinsPrefs", Context.MODE_PRIVATE).getAll();

        String json = new Gson().toJson(data);
        String encrypted = encrypt(json);

        try (OutputStream os = context.getContentResolver().openOutputStream(uri)) {
            if (os != null) {
                os.write(encrypted.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    public static void importData(Context context, Uri uri, boolean override) throws Exception {
        // Read
        StringBuilder sb = new StringBuilder();
        try (InputStream is = context.getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null)
                sb.append(line);
        }

        String decrypted = decrypt(sb.toString());
        BackupData data = new Gson().fromJson(decrypted, BackupData.class);

        MindMintRoomDatabase db = MindMintRoomDatabase.getInstance(context);

        if (override) {
            db.taskDao().deleteAll();
            db.habitDao().deleteAll();
            db.habitCompletionDao().deleteAll();
            db.habitCachedStatsDao().deleteAll();
            db.focusDao().deleteAllSessions();
            db.focusDao().deleteAllDailyStats();
            db.focusDao().deleteAllState();
            db.dailyStatsDao().deleteAll();
            db.focusTopicDao().deleteAll();

            // Re-insert
            if (data.tasks != null && !data.tasks.isEmpty())
                db.taskDao().insertAll(data.tasks);
            if (data.habits != null && !data.habits.isEmpty())
                db.habitDao().insertAll(data.habits);
            if (data.habitCompletions != null && !data.habitCompletions.isEmpty())
                db.habitCompletionDao().insertAll(data.habitCompletions);
            if (data.habitCachedStats != null) {
                for (HabitCachedStatsEntity e : data.habitCachedStats)
                    db.habitCachedStatsDao().insertOrUpdate(e);
            }
            if (data.focusDailyStats != null) {
                for (FocusDailyStatEntity e : data.focusDailyStats)
                    db.focusDao().insertOrReplaceDaily(e);
            }
            if (data.focusSessions != null) {
                for (FocusSessionEntity e : data.focusSessions)
                    db.focusDao().insertSession(e);
            }
            if (data.dailyStats != null) {
                for (DailyStatsEntity e : data.dailyStats)
                    db.dailyStatsDao().insertOrReplace(e);
            }
            if (data.focusTopics != null) {
                for (FocusTopicEntity e : data.focusTopics)
                    db.focusTopicDao().insert(e);
            }

            // Restore Prefs
            restorePrefs(PreferenceManager.getDefaultSharedPreferences(context), data.defaultPrefs, true);
            restorePrefs(context.getSharedPreferences("PeaceCoinsPrefs", Context.MODE_PRIVATE), data.peaceCoinsPrefs,
                    true);
        } else {
            // Merge (Try insert, ignore on failure)
            if (data.tasks != null) {
                for (TaskEntity item : data.tasks) {
                    try {
                        db.taskDao().insert(item);
                    } catch (Exception ignored) {
                    }
                }
            }
            if (data.habits != null) {
                for (HabitEntity item : data.habits) {
                    try {
                        db.habitDao().insert(item);
                    } catch (Exception ignored) {
                    }
                }
            }
            if (data.habitCompletions != null) {
                for (HabitCompletionEntity item : data.habitCompletions) {
                    try {
                        db.habitCompletionDao().insert(item);
                    } catch (Exception ignored) {
                    }
                }
            }
            if (data.habitCachedStats != null) {
                for (HabitCachedStatsEntity item : data.habitCachedStats) {
                    try {
                        db.habitCachedStatsDao().insertOrUpdate(item);
                    } catch (Exception ignored) {
                    }
                }
            }
            if (data.focusDailyStats != null) {
                for (FocusDailyStatEntity item : data.focusDailyStats) {
                    try {
                        db.focusDao().insertOrReplaceDaily(item);
                    } catch (Exception ignored) {
                    }
                }
            }
            if (data.focusSessions != null) {
                for (FocusSessionEntity item : data.focusSessions) {
                    try {
                        db.focusDao().insertSession(item);
                    } catch (Exception ignored) {
                    }
                }
            }
            if (data.dailyStats != null) {
                for (DailyStatsEntity item : data.dailyStats) {
                    try {
                        db.dailyStatsDao().insertOrReplace(item);
                    } catch (Exception ignored) {
                    }
                }
            }
            if (data.focusTopics != null) {
                for (FocusTopicEntity item : data.focusTopics) {
                    try {
                        db.focusTopicDao().insert(item);
                    } catch (Exception ignored) {
                    }
                }
            }

            // Merge Prefs
            restorePrefs(PreferenceManager.getDefaultSharedPreferences(context), data.defaultPrefs, false);
            restorePrefs(context.getSharedPreferences("PeaceCoinsPrefs", Context.MODE_PRIVATE), data.peaceCoinsPrefs,
                    false);
        }
    }

    private static void restorePrefs(SharedPreferences prefs, Map<String, ?> data, boolean clearFirst) {
        if (data == null)
            return;
        SharedPreferences.Editor editor = prefs.edit();
        if (clearFirst) {
            editor.clear();
        }

        for (Map.Entry<String, ?> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Boolean) {
                editor.putBoolean(key, (Boolean) value);
            } else if (value instanceof Float) {
                editor.putFloat(key, (Float) value);
            } else if (value instanceof Integer) {
                editor.putInt(key, (Integer) value);
            } else if (value instanceof Long) {
                editor.putLong(key, (Long) value);
            } else if (value instanceof String) {
                editor.putString(key, (String) value);
            }
        }
        editor.apply();
    }

    private static String encrypt(String strToEncrypt) {
        try {
            byte[] iv = new byte[16];
            System.arraycopy(STATIC_IV.getBytes(StandardCharsets.UTF_8), 0, iv, 0, Math.min(STATIC_IV.length(), 16));
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), STATIC_SALT.getBytes(), 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivspec);
            return Base64.encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);
        } catch (Exception e) {
            Log.e("Error while encrypting: ", e.toString());
        }
        return null;
    }

    private static String decrypt(String strToDecrypt) {
        try {
            byte[] iv = new byte[16];
            System.arraycopy(STATIC_IV.getBytes(StandardCharsets.UTF_8), 0, iv, 0, Math.min(STATIC_IV.length(), 16));
            IvParameterSpec ivspec = new IvParameterSpec(iv);

            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(SECRET_KEY.toCharArray(), STATIC_SALT.getBytes(), 65536, 256);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivspec);
            return new String(cipher.doFinal(Base64.decode(strToDecrypt, Base64.DEFAULT)));
        } catch (Exception e) {
            Log.e("Error while decrypting: ", e.toString());
        }
        return null;
    }
}
