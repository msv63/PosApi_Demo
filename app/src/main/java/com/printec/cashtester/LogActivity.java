package com.printec.cashtester;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class LogActivity extends AppCompatActivity {

    private TextView txtView;
    private String logfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        Button btn_clear = findViewById(R.id.clear_log);
        txtView = findViewById(R.id.log);
        txtView.setMovementMethod(new ScrollingMovementMethod());

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        logfile = getIntent().getStringExtra("logfile");

        try {
            InputStream inputStream = openFileInput(logfile);
            int i;

            i = inputStream.read();
            while (i != -1)
            {
                byteArrayOutputStream.write(i);
                i = inputStream.read();
            }
            inputStream.close();

        } catch (FileNotFoundException e) {
            btn_clear.setEnabled(false);
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }

        txtView.setText(byteArrayOutputStream.toString());

    }

    public void onClearLog(View view) {
        File f = new File(getApplicationContext().getFilesDir(),logfile);
        if (f.isFile()) {
            f.delete();
            txtView.setText("");
            view.setEnabled(false);
        }
    }
}
