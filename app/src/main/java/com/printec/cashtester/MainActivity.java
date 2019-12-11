package com.printec.cashtester;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import com.printec.pos.*;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private Button Run;
    private EditText Output;
    private boolean isBreak;
    private SharedPreferences prefs;
    private PosApi Api = new PosApi();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Run = findViewById(R.id.run_button);
        Output = findViewById(R.id.output);
        Output.setKeyListener(null);

        Api.checkPermissions();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case R.id.logfile:
                Intent intent = new Intent(this, LogActivity.class);
                intent.putExtra("logfile", prefs.getString("logfile",""));
                startActivity(intent);
                return true;
            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showError(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.error))
                .setMessage(message)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK", null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void onRun (View view) {

        if (Run.getText().toString().equals(getString(R.string.stop_button))) {
            isBreak = true;
            return;
        }

        if (!Api.checkPermissions())
            return;

        isBreak = false;

        String connection = prefs.getString("connection", "USB");
        String logfile = prefs.getString("logfile", "");

        if (Api.posOpen(connection, logfile)) {
//        if (Api.posOpen("192.168.0.104:5001", "log.txt")) {
//        if (Api.posOpen("USB", "log.txt")) {

            Log.i("OUTPUT","open success");

            Run.setText(R.string.stop_button);

            Api.posSet(PosApi.POS_AMOUNT, "100");
            Api.posSet(PosApi.POS_CURRENCY, "980");

            Api.posSend(PosApi.ACTION_PAYMENT);

            Output.setText("");

            final Handler handler = new Handler();

            handler.post(new Runnable() {

            void print_response(int response)
            {
                boolean isBreakPossible = false;
                String s = String.format("response = %X\n", response);
                Output.getText().append(s);
                for(PosApi.Param p : Api.posGetAll()) {
                    s= String.format("%s=%s\n", p.name, p.value);
                    Output.getText().append(s);
                    if (p.name.equals("msg_break") && p.value.equals("1"))
                        isBreakPossible = true;
                }
                Output.getText().append("\n");
                Run.setEnabled(isBreakPossible);
            }

                @Override
                public void run() {

                    int response = Api.posReceive(100);

                    if (isBreak) {
                        Api.posSend(PosApi.ACTION_BREAK);
                        response = Api.posReceive(100);
                        Output.getText().append(String.format(Locale.getDefault(),"BREAK(%d)\n",response));

                        Run.setText(R.string.run_button);
                        Api.posClose();

                        return;
                    }

                    if (response > 0 && response != PosApi.RESP_TIMEOUT) {
                        print_response(response);

                    } else if (response == 0) {
                        showError(getString(R.string.zero_code));
                    }

                    if ((response == PosApi.RESP_MESSAGE || response == PosApi.RESP_TIMEOUT || response == PosApi.RESP_IDENTIFIER))
                        handler.postDelayed(this, 0);
                    else {
                        Run.setEnabled(true);
                        Run.setText(R.string.run_button);

                        Api.posClose();
                    }
                }
            });

        } else {
            showError(getString(R.string.open_failed));
        }
    }
}
