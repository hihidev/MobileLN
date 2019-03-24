package com.mobileln;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.customtabs.CustomTabsIntent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.WriterException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONException;

import java.io.IOException;

import com.mobileln.lightningd.LightningCli;
import com.mobileln.utils.BtcSatUtils;
import com.mobileln.utils.QRUtils;

public class BitcoinWalletActivity extends AppCompatActivity {

    private ImageView mQRImageView;
    private Button mReceiveBtcAddrBtn;
    private EditText mBtcWithdrawalAddrEditText;
    private EditText mBtcWithdrawalAmountEditText;
    private Button mBtcWithdrawalBtn;
    private ImageView mBtcWithdrawalCameraImageView;
    private TextView mBtcBalanceTextView;
    private Button mGetTestCoinFromFaucetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitcoin_wallet);
        mQRImageView = findViewById(R.id.receive_payment_bitcoin_imageview);
        mReceiveBtcAddrBtn = findViewById(R.id.receive_my_btc_address_button);
        mBtcWithdrawalAddrEditText = findViewById(R.id.btc_withdrawal_address);
        mBtcWithdrawalBtn = findViewById(R.id.btc_withdrawal_button);
        mBtcWithdrawalAmountEditText = findViewById(R.id.btc_withdrawal_amount);
        mBtcBalanceTextView = findViewById(R.id.btc_wallet_balance_textview);
        mGetTestCoinFromFaucetButton = findViewById(R.id.receive_test_coin_from_faucet_button);
        mGetTestCoinFromFaucetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(BitcoinWalletActivity.this,
                        Uri.parse("https://tbtc.bitaps.com/"));
                copyBtcAddressToClipBoard("Deposit address copied.\nPlease paste it to the address field.");
            }
        });
        mGetTestCoinFromFaucetButton.setVisibility(
                NodeService.isTestnet() ? View.VISIBLE : View.GONE);
        mReceiveBtcAddrBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyBtcAddressToClipBoard("Deposit address copied");
            }
        });
        mBtcWithdrawalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showWithdrawalConfirmationDialog(mBtcWithdrawalAddrEditText.getText().toString(),
                        mBtcWithdrawalAmountEditText.getText().toString());
            }
        });
        mBtcWithdrawalCameraImageView = findViewById(R.id.btc_withdrawal_camera_btn);
        mBtcWithdrawalCameraImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentIntegrator integrator = new IntentIntegrator(BitcoinWalletActivity.this);
                integrator.setOrientationLocked(false);
                integrator.setPrompt("Scan Withdrawal Address");
                integrator.setCameraId(0);  // Use a specific camera of the device
                integrator.setBeepEnabled(false);
                integrator.setBarcodeImageEnabled(true);
                integrator.initiateScan();
            }
        });

        updateWithdrawalInfoAsync();
        updateWalletBalanceAsync();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() == null) {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
            } else {
                mBtcWithdrawalAddrEditText.setText(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void updateWithdrawalInfoAsync() {
        new AsyncTask<Void, Void, String>() {

            @Override
            protected String doInBackground(Void... voids) {
                try {
                    String[] addresses = LightningCli.newInstance().getMyBech32Addresses();
                    return addresses[addresses.length - 1];
                } catch (IOException | JSONException e) {
                    return null;
                }
            }

            @Override
            public void onPostExecute(String str) {
                if (str == null) {
                    Toast.makeText(BitcoinWalletActivity.this, "cannot get address",
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    Bitmap qrCode = QRUtils.encodeAsBitmap(str, getResources());
                    mQRImageView.setImageBitmap(qrCode);
                    mReceiveBtcAddrBtn.setText(str);
                } catch (WriterException e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }


    private void updateWalletBalanceAsync() {
        new AsyncTask<Void, Void, Long>() {

            @Override
            protected Long doInBackground(Void... voids) {
                try {
                    return LightningCli.newInstance().getConfirmedBtcBalanceInWallet();
                } catch (IOException | JSONException e) {
                    return Long.valueOf(-1);
                }
            }

            @Override
            protected void onPostExecute(Long result) {
                mBtcBalanceTextView.setText(BtcSatUtils.sat2String(result));
            }
        }.execute();
    }

    private void copyBtcAddressToClipBoard(String message) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(
                Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("addr", mReceiveBtcAddrBtn.getText());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(BitcoinWalletActivity.this, message,
                Toast.LENGTH_LONG).show();
    }

    private void showWithdrawalConfirmationDialog(final String address, final String amount) {
        final boolean withdrawAll = "all".equals(amount);
        final long longAmount = withdrawAll ? 0 : Long.valueOf(amount);
        new AlertDialog.Builder(this)
                .setTitle("BTC withdrawal")
                .setMessage("Withdrawal address:\t" + address + "\n" + "Amount:\t" + (withdrawAll
                        ? "all" : BtcSatUtils.sat2String(longAmount)) + "\n"
                        + "Do you CONFIRM the withdrawal?\n(CANNOT BE UNDONE)")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int whichButton) {
                        Toast.makeText(BitcoinWalletActivity.this, "Yaay",
                                Toast.LENGTH_SHORT).show();
                        new Thread() {
                            public void run() {
                                try {
                                    showWithdrawalSuccessResult(
                                            LightningCli.newInstance().withdrawBtc(address,
                                                    longAmount, withdrawAll));
                                } catch (IOException | JSONException e) {
                                    showWithdrawalFailedResult(e.getMessage());
                                }
                            }
                        }.start();
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }


    private void showWithdrawalSuccessResult(final String txid) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(BitcoinWalletActivity.this)
                        .setTitle("Withdrawal success")
                        .setMessage("txid:\t" + txid
                                + "It may take 60+ minutes (6 confirmations) to show in your "
                                + "withdrawal wallet")
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            }
        });
    }

    private void showWithdrawalFailedResult(final String errorMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(BitcoinWalletActivity.this)
                        .setTitle("Withdrawal failed")
                        .setMessage("Error: " + errorMsg)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setNeutralButton(android.R.string.ok, null)
                        .show();
            }
        });
    }
}
