package com.mobileln;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;

import com.mobileln.bitcoind.BitcoinCli;
import com.mobileln.bitcoind.Bitcoind;
import com.mobileln.bitcoind.BitcoindState;
import com.mobileln.lightningd.LightningCli;
import com.mobileln.lightningd.Lightningd;
import com.mobileln.lightningd.LightningdState;
import com.mobileln.utils.FastSyncUtils;
import com.mobileln.utils.WalletStateSharedPrefs;

public class NodeService extends Service {

    private static final String TAG = "NodeService";
    private static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";
    private static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";
    private static final int NOTIFICATION_ID = 689777;

    public static class NodeState {
        public static final int DISCONNECTING = -1;
        public static final int UNKNOWN = 0;
        public static final int ALL_DISCONNECTED = 1;
        public static final int DOWNLOAD_FASTSYNC = 2;
        public static final int STARTING_BITCOIND = 3;
        public static final int SYNCING_BITCOIND = 4;
        public static final int STARTING_LIGHTNINGD = 5;
        public static final int ALL_READY = 6;
    }

    private static volatile int sCurrentState = NodeState.ALL_DISCONNECTED;
    private static volatile boolean sRunning = false;
    private static volatile boolean sTestnet = true;

    private static ArrayList<Runnable> sCurrentStateUpdatedCallback = new ArrayList<>();
    private static final Object mCurrentStateUpdateLock = new Object();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_START_FOREGROUND_SERVICE:
                    startForegroundService();
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    stopForegroundService();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public static void startNodeService(Context context) {
        Intent intent = new Intent(ACTION_START_FOREGROUND_SERVICE);
        intent.setClass(context, NodeService.class);
        context.startService(intent);
    }

    public static void stopNodeService(Context context) {
        Intent intent = new Intent(ACTION_STOP_FOREGROUND_SERVICE);
        intent.setClass(context, NodeService.class);
        context.startService(intent);
    }

    private void startForegroundService() {
        sRunning = true;
        startForeground(NOTIFICATION_ID, getNotification());
        new Thread() {
            public void run() {
                try {
                    int i = 0;
                    while (sRunning && FastSyncUtils.isPendingFastSyncWork(NodeService.this)) {
                        Log.i(TAG, "Waiting for fast sync work: " + i++);
                        int status = FastSyncUtils.getDownloadStatus(NodeService.this);
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            Log.i(TAG, "Download done, coping file to internal storage");
                            try {
                                FastSyncUtils.copyFastSyncFileToInternalStorage(NodeService.this);
                                Log.i(TAG, "Copied to internal storage, check checksum now");
                                boolean result = FastSyncUtils.validateChecksum(NodeService.this);
                                Log.i(TAG, "Validation result: " + result);
                                if (!result) {
                                    Log.i(TAG, "Invalid file, restart download");
                                    FastSyncUtils.clearAllDownloads(NodeService.this);
                                    FastSyncUtils.startDownloadFastSyncDb(NodeService.this);
                                    continue;
                                }
                                Log.i(TAG, "Checksum correct, extract files now");
                                FastSyncUtils.extractFromInternalStorage(NodeService.this);
                                Log.i(TAG, "Extract finish!");
                                FastSyncUtils.savePendingFastSyncWork(NodeService.this, false);
                                break;
                            } finally {
                                FastSyncUtils.removeInternalStorageFile(NodeService.this);
                            }
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            FastSyncUtils.clearAllDownloads(NodeService.this);
                            FastSyncUtils.startDownloadFastSyncDb(NodeService.this);
                        }
                        Thread.sleep(1000);
                        Log.i(TAG, "Download progress: " + FastSyncUtils.downloadProgress(
                                NodeService.this));
                        setCurrentState(NodeState.DOWNLOAD_FASTSYNC, true);
                    }
                    if (!sRunning) {
                        return;
                    }
                    Bitcoind.getInstance().startService(getApplicationContext());
                    setCurrentState(NodeState.STARTING_BITCOIND, false);
                    i = 0;
                    while (sRunning && BitcoindState.getInstance().getCurrentState()
                            <= BitcoindState.STATE_STARTING) {
                        Log.i(TAG, "Waiting for bitcoind start: " + i++);
                        Thread.sleep(1000);
                    }
                    if (!sRunning) {
                        return;
                    }
                    i = 0;
                    while (sRunning && BitcoindState.getInstance().getCurrentState()
                            <= BitcoindState.STATE_SYNCING) {
                        Log.i(TAG, "Waiting for bitcoind sync ready: " + i++);
                        Thread.sleep(1000);
                        setCurrentState(NodeState.SYNCING_BITCOIND, true);
                    }
                    if (!sRunning) {
                        return;
                    }
                    if (sRunning) {
                        setCurrentState(NodeState.STARTING_LIGHTNINGD, false);
                        Lightningd.getInstance().startService(getApplicationContext());
                    }
                    if (!sRunning) {
                        return;
                    }
                    i = 0;
                    while (sRunning && LightningdState.getInstance().getCurrentState()
                            != LightningdState.STATE_READY) {
                        Log.i(TAG, "Waiting for lightningd ready: " + i++);
                        Thread.sleep(1000);
                    }
                    if (!sRunning) {
                        return;
                    }
                    LightningCli.newInstance().updateCachedInOutboundCapacity();
                    // TODO: No hard code
                    // Only cache top 1000 watch-only index to monitor unconfirmed balance
                    // 640K ram should be enough
                    final int MAX_WATCH_ONLY_INDEX = 1000;
                    final WalletStateSharedPrefs prefs = new WalletStateSharedPrefs(
                            getApplicationContext());
                    int maxIndex = prefs.getWatchOnlyMaxIndex();
                    if (maxIndex < MAX_WATCH_ONLY_INDEX) {
                        boolean done = false;
                        while (!done) {
                            try {
                                String[] addresses = LightningCli.newInstance().getListAddrs(
                                        MAX_WATCH_ONLY_INDEX);
                                for (String address : addresses) {
                                    BitcoinCli.addWatchOnlyAddress(address);
                                }
                                prefs.saveWatchOnlyMaxIndex(MAX_WATCH_ONLY_INDEX);
                                done = true;
                            } catch (Exception e) {
                                e.printStackTrace();
                                Thread.sleep(1000);
                            }
                        }
                    }

                    setCurrentState(NodeState.ALL_READY, false);
                } catch (Exception e) {
                    e.printStackTrace();
                    stopForegroundService();
                }
            }
        }.start();
    }

    private Notification getNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.node_service_title))
                .setContentIntent(pendingIntent)
                .setContentText(getString(R.string.node_service_content_text))
                .setSmallIcon(R.drawable.ic_stat_flash)
                .setOngoing(true);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    getString(R.string.notification_channel_id),
                    getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.app_name));
            channel.setShowBadge(false);
            notificationManager.createNotificationChannel(channel);
            builder.setChannelId(getString(R.string.notification_channel_id));
        }
        return builder.build();
    }

    private void stopForegroundService() {
        sRunning = false;
        Lightningd.getInstance().stopService(getApplicationContext());
        Bitcoind.getInstance().stopService(getApplicationContext());
        stopForeground(true);
        setCurrentState(NodeState.DISCONNECTING, false);
        new Thread() {
            public void run() {
                while (Bitcoind.getInstance().isRunning()) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                setCurrentState(NodeState.ALL_DISCONNECTED, false);
            }
        }.start();
    }

    public static int getCurrentState() {
        return sCurrentState;
    }

    private void setCurrentState(int state, boolean force) {
        if (state != sCurrentState || force) {
            Log.i(TAG, "setCurrentState: " + state);
            sCurrentState = state;
            synchronized (mCurrentStateUpdateLock) {
                for (Runnable runnable : sCurrentStateUpdatedCallback) {
                    runnable.run();
                }
            }
        }
    }

    public static void addCurrentStateUpdatedCallback(Runnable runnable) {
        synchronized (mCurrentStateUpdateLock) {
            sCurrentStateUpdatedCallback.add(runnable);
        }
    }

    public static void removeCurrentStateUpdatedCallback(Runnable runnable) {
        synchronized (mCurrentStateUpdateLock) {
            sCurrentStateUpdatedCallback.remove(runnable);
        }
    }

    public static boolean isTestnet() {
        return sTestnet;
    }

    public static int getMinConfirmation() {
        if (sTestnet) {
            return 1;
        } else {
            return 1;
        }
    }

    public static boolean isRunning() {
        return sRunning;
    }
}
