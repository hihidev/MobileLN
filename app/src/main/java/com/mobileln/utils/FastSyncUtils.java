package com.mobileln.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FastSyncUtils {

    private static final String FAST_SYNC_PREFERENCE = "fastsyncpref.xml";
    private static final String DOWNLOAD_ID = "download_id";
    private static final String PENDING_FAST_SYNC_WORK = "pending_fast_sync_work";

    private static final String TESTNET_FASTSYNC_DB_URL =
            "http://utxosets.blob.core.windows"
                    + ".net/public/utxo-snapshot-bitcoin-testnet-1445586.tar";
    private static final String TESTNET_FASTSYNC_SHA256 =
            "eabaaa717bb8eeaf603e383dd8642d9d34df8e767fccbd208b0c936b79c82742";

    private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static long startDownloadFastSyncDb(Context context) {
        DownloadManager.Request request = new DownloadManager.Request(
                Uri.parse(TESTNET_FASTSYNC_DB_URL))
                .setTitle("fastsync_db")
                .setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(false)
                .setAllowedOverRoaming(false);
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(
                Context.DOWNLOAD_SERVICE);
        long id = downloadManager.enqueue(request);
        SharedPreferences sp = context.getSharedPreferences(FAST_SYNC_PREFERENCE,
                Context.MODE_PRIVATE);
        sp.edit().putLong(DOWNLOAD_ID, id).apply();
        return id;
    }

    public static int getDownloadStatus(Context context) {
        SharedPreferences sp = context.getSharedPreferences(FAST_SYNC_PREFERENCE,
                Context.MODE_PRIVATE);
        long id = sp.getLong(DOWNLOAD_ID, 0);
        if (id == 0) {
            return DownloadManager.STATUS_FAILED;
        }
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(
                Context.DOWNLOAD_SERVICE);
        Cursor cursor = null;
        try {
            cursor = downloadManager.query(new DownloadManager.Query().setFilterById(id));
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    return cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                }
            }
        } finally {
            cursor.close();
        }
        return DownloadManager.STATUS_FAILED;
    }

    public static void clearAllDownloads(Context context) {
        SharedPreferences sp = context.getSharedPreferences(FAST_SYNC_PREFERENCE,
                Context.MODE_PRIVATE);
        long id = sp.getLong(DOWNLOAD_ID, 0);
        if (id == 0) {
            return;
        }
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(
                Context.DOWNLOAD_SERVICE);
        downloadManager.remove(id);
    }

    private static Uri getDownloadFileUri(Context context) {
        SharedPreferences sp = context.getSharedPreferences(FAST_SYNC_PREFERENCE,
                Context.MODE_PRIVATE);
        long id = sp.getLong(DOWNLOAD_ID, 0);
        if (id == 0) {
            return null;
        }
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(
                Context.DOWNLOAD_SERVICE);
        return downloadManager.getUriForDownloadedFile(id);
    }

    public static boolean copyFastSyncFileToInternalStorage(Context context) {
        Uri uri = FastSyncUtils.getDownloadFileUri(context);
        if (uri == null) {
            return false;
        }
        try {
            String dest = new File(context.getFilesDir(),
                    FileUtils.TMP_FASTSYNC_FILE).getCanonicalPath();
            try (InputStream in = new BufferedInputStream(
                    context.getContentResolver().openInputStream(uri));
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(dest))
            ) {
                byte[] buffer = new byte[1024 * 8];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    out.write(buffer, 0, length);
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean validateChecksum(Context context) {
        try {
            String file = new File(context.getFilesDir(),
                    FileUtils.TMP_FASTSYNC_FILE).getCanonicalPath();
            try {
                MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
                byte[] bytes = new byte[1024];
                int len;
                while ((len = in.read(bytes)) > 0) {
                    messageDigest.update(bytes, 0, len);
                }
                String digest = bytesToHex(messageDigest.digest());
                return digest.toLowerCase().equals(TESTNET_FASTSYNC_SHA256.toLowerCase());
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static double downloadProgress(Context context) {
        SharedPreferences sp = context.getSharedPreferences(FAST_SYNC_PREFERENCE,
                Context.MODE_PRIVATE);
        long id = sp.getLong(DOWNLOAD_ID, 0);
        if (id == 0) {
            return -1;
        }
        DownloadManager downloadManager = (DownloadManager) context.getSystemService(
                Context.DOWNLOAD_SERVICE);
        Cursor cursor = null;
        try {
            cursor = downloadManager.query(new DownloadManager.Query().setFilterById(id));
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long downloadedSize = cursor.getLong(
                            cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    long totalSize = cursor.getLong(
                            cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                    return ((double) downloadedSize) / totalSize;
                }
            }
        } finally {
            cursor.close();
        }
        return -1;
    }

    public static boolean isPendingFastSyncWork(Context context) {
        SharedPreferences sp = context.getSharedPreferences(FAST_SYNC_PREFERENCE,
                Context.MODE_PRIVATE);
        return sp.getBoolean(PENDING_FAST_SYNC_WORK, false);
    }

    public static void savePendingFastSyncWork(Context context, boolean pending) {
        SharedPreferences sp = context.getSharedPreferences(FAST_SYNC_PREFERENCE,
                Context.MODE_PRIVATE);
        sp.edit().putBoolean(PENDING_FAST_SYNC_WORK, pending).apply();
    }

    public static void extractFromInternalStorage(Context context) {
        try {
            String file = new File(context.getFilesDir(),
                    FileUtils.TMP_FASTSYNC_FILE).getCanonicalPath();
            decompressTar(file, FileUtils.getBitcoindDataFolderPath(context));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void removeInternalStorageFile(Context context) {
        try {
            String file = new File(context.getFilesDir(),
                    FileUtils.TMP_FASTSYNC_FILE).getCanonicalPath();
            new File(file).delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void decompressTar(String inputPath, String outputFolder) throws IOException {
        try (TarArchiveInputStream is = new TarArchiveInputStream(
                new BufferedInputStream(new FileInputStream(inputPath)))) {
            TarArchiveEntry entry;
            while ((entry = is.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                File outfile = new File(outputFolder, entry.getName());
                File parent = outfile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                IOUtils.copy(is, new FileOutputStream(outfile));
            }
        }
    }

}
