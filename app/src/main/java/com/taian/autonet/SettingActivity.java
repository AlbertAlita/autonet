package com.taian.autonet;


import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.taian.autonet.client.handler.WrapNettyClient;
import com.taian.autonet.client.net.Net;
import com.taian.autonet.client.utils.Utils;

import io.reactivex.annotations.Nullable;


public class SettingActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        findViewById(R.id.back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        final EditText portEt = findViewById(R.id.port);
        final EditText ipEt = findViewById(R.id.ip);
        TextView mac = findViewById(R.id.mac);
        mac.setText(getString(R.string.mac, AppApplication.getMacAdress()));

        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String port = portEt.getText().toString();
                String ip = ipEt.getText().toString();
                if (TextUtils.isEmpty(ip)) {
                    Toast.makeText(SettingActivity.this, R.string.pls_input_ip, Toast.LENGTH_LONG).show();
                    return;
                }
                if (TextUtils.isEmpty(port)) {
                    Toast.makeText(SettingActivity.this, R.string.pls_input_port, Toast.LENGTH_LONG).show();
                    return;
                }
                if (!Utils.isNumber(port)) {
                    Toast.makeText(SettingActivity.this, R.string.pls_input_right_port, Toast.LENGTH_LONG).show();
                    return;
                }
                AppApplication.setIP(ip);
                AppApplication.setPort(Integer.parseInt(port));
                WrapNettyClient.getInstance().resetIp();
                setResult(RESULT_OK);
                finish();
            }
        });

        String ip = AppApplication.getIP();
        int port = AppApplication.getPort();
        portEt.setText(String.valueOf(port == -1 ? Net.port : port));
        ipEt.setText(TextUtils.isEmpty(ip) ? Net.URL : ip);
    }
}
