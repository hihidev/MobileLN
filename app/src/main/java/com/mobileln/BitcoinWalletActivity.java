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
import android.util.Pair;
import android.view.View;
import android.widget.ArrayAdapter;
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

import com.mobileln.bitcoind.BitcoinCli;
import com.mobileln.lightningd.LightningCli;
import com.mobileln.utils.BtcSatUtils;
import com.mobileln.utils.QRUtils;
import com.mobileln.utils.UIUtils;

public class BitcoinWalletActivity extends AppCompatActivity {

    private ImageView mQRImageView;
    private Button mReceiveBtcAddrBtn;
    private EditText mBtcWithdrawalAddrEditText;
    private EditText mBtcWithdrawalAmountEditText;
    private Button mBtcWithdrawalBtn;
    private ImageView mBtcWithdrawalCameraImageView;
    private TextView mBtcBalanceTextView;
    private TextView mBtcUnconfirmedBalanceTextView;
    private View mBtcUnconfirmedBalanceLayout;
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
        mBtcUnconfirmedBalanceTextView = findViewById(R.id.btc_wallet_unconfirmed_balance_textview);
        mBtcUnconfirmedBalanceLayout = findViewById(R.id.btc_wallet_unconfirmed_balance_layout);
        mGetTestCoinFromFaucetButton = findViewById(R.id.receive_test_coin_from_faucet_button);
        mGetTestCoinFromFaucetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showTestnetFaucetDialog();
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
                    UIUtils.showErrorToast(BitcoinWalletActivity.this, e.getMessage());
                    return null;
                }
            }

            @Override
            public void onPostExecute(String str) {
                if (str == null) {
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
        new AsyncTask<Void, Void, Pair<Long, Long>>() {

            @Override
            protected Pair<Long, Long> doInBackground(Void... voids) {
                try {
                    long unconfirmedBtcBalance = BitcoinCli.getUnconfirmedBalance(1);
                    long confirmedBalance =
                            LightningCli.newInstance().getConfirmedBtcBalanceInWallet();
                    return Pair.create(unconfirmedBtcBalance, confirmedBalance);
                } catch (IOException | JSONException e) {
                    UIUtils.showErrorToast(BitcoinWalletActivity.this, e.getMessage());
                    return null;
                }
            }

            @Override
            protected void onPostExecute(Pair<Long, Long> result) {
                if (result == null) {
                    return;
                }
                if (result.first != 0) {
                    mBtcUnconfirmedBalanceLayout.setVisibility(View.VISIBLE);
                    mBtcUnconfirmedBalanceTextView.setText(
                            (result.first > 0 ? "+" : "") + BtcSatUtils.sat2String(result.first));
                } else {
                    mBtcUnconfirmedBalanceLayout.setVisibility(View.GONE);
                }
                mBtcBalanceTextView.setText(BtcSatUtils.sat2String(result.second));
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

    private void showTestnetFaucetDialog() {
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this);
        builderSingle.setTitle("Select website");
        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.select_dialog_singlechoice);
        arrayAdapter.add("https://coinfaucet.eu/en/btc-testnet/");
        arrayAdapter.add("https://testnet-faucet.mempool.co/");
        arrayAdapter.add("http://tbtc.bitaps.com/");
        arrayAdapter.add("http://bitcoinfaucet.uo1.net/");
        arrayAdapter.add("https://kuttler.eu/en/bitcoin/btc/faucet/");
        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(BitcoinWalletActivity.this,
                        Uri.parse(arrayAdapter.getItem(which)));
                copyBtcAddressToClipBoard(
                        "Deposit address copied.\nPlease paste it to the address field.");
            }
        });
        builderSingle.show();
    }
}
