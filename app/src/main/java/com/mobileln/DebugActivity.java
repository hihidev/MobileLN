package com.mobileln;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.io.IOException;
import java.util.Map;

import com.mobileln.bitcoind.BitcoindConfig;
import com.mobileln.lightningd.LightningdConfig;
//import com.mobileln.utils.ExtractResourceUtils;
import com.mobileln.utils.FastSyncUtils;

public class DebugActivity extends AppCompatActivity {

    private static final String TAG = "DebugActivity";
    private Map<String, String> mBitcoindConfig = null;
    private Map<String, String> mLightningdConfig = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        findViewById(R.id.debug_read_bitcoin_config_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            mBitcoindConfig = BitcoindConfig.readConfig(DebugActivity.this);
                            Log.i(TAG, mBitcoindConfig.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
        findViewById(R.id.debug_read_default_bitcoin_config_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mBitcoindConfig = BitcoindConfig.readDefaultConfig();
                        Log.i(TAG, mBitcoindConfig.toString());
                    }
                });
        findViewById(R.id.debug_write_bitcoin_config_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            BitcoindConfig.saveConfig(DebugActivity.this, mBitcoindConfig);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

        findViewById(R.id.debug_read_ln_config_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            mLightningdConfig = LightningdConfig.readConfig(DebugActivity.this);
                            Log.i(TAG, mLightningdConfig.toString());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
        findViewById(R.id.debug_read_default_ln_config_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mLightningdConfig = LightningdConfig.readDefaultConfig();
                        Log.i(TAG, mLightningdConfig.toString());
                    }
                });
        findViewById(R.id.debug_write_ln_config_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        try {
                            LightningdConfig.saveConfig(DebugActivity.this, mLightningdConfig);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
        findViewById(R.id.debug_extract_all_executables_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
//                        try {
//                            ExtractResourceUtils.extractExecutablesIfNecessary(DebugActivity.this, true);
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
                    }
                });
        findViewById(R.id.debug_start_service_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        NodeService.startNodeService(DebugActivity.this);
                    }
                });
        findViewById(R.id.debug_stop_service_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        NodeService.stopNodeService(DebugActivity.this);
                    }
                });
        findViewById(R.id.debug_bitcoin_log_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent();
                        intent.setClass(DebugActivity.this, DebugLogActivity.class);
                        intent.putExtra("type", "bitcoind");
                        startActivity(intent);
                    }
                });
        findViewById(R.id.debug_lightning_log_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent();
                        intent.setClass(DebugActivity.this, DebugLogActivity.class);
                        intent.putExtra("type", "lightningd");
                        startActivity(intent);
                    }
                });
        findViewById(R.id.debug_download_file_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                long id = FastSyncUtils.startDownloadFastSyncDb(DebugActivity.this);
                Log.i(TAG, "download id: " + id);
            }
        });
        findViewById(R.id.debug_get_file_status_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int status = FastSyncUtils.getDownloadStatus(DebugActivity.this);
                Log.i(TAG, "download status: " + status);
            }
        });
        findViewById(R.id.debug_remove_file_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FastSyncUtils.clearAllDownloads(DebugActivity.this);
            }
        });
        findViewById(R.id.debug_verify_file_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean result = FastSyncUtils.validateChecksum(DebugActivity.this);
                Log.i(TAG, "result: " + result);
            }
        });
        findViewById(R.id.debug_copy_file_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "copy start");
                FastSyncUtils.copyFastSyncFileToInternalStorage(DebugActivity.this);
                Log.i(TAG, "copy done");
            }
        });
        findViewById(R.id.debug_extract_tar_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "Extract start");
                FastSyncUtils.extractFromInternalStorage(DebugActivity.this);
                Log.i(TAG, "Extract done");
            }
        });
    }
}
