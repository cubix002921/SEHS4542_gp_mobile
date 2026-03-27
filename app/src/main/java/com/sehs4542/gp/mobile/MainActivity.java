package com.sehs4542.gp.mobile;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button startButton = findViewById(R.id.button_start);
        startButton.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, UsernameActivity.class);
            startActivity(intent);
        });
    }
}
