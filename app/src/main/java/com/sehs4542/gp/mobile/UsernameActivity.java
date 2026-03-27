package com.sehs4542.gp.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class UsernameActivity extends AppCompatActivity {
    static final String PREFS_NAME = "game_prefs";
    static final String KEY_USERNAME = "username";
    static final String KEY_TOTAL_SCORE = "total_score";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_username);

        EditText usernameInput = findViewById(R.id.edit_username);
        Button continueButton = findViewById(R.id.button_continue);

        continueButton.setOnClickListener(view -> {
            String username = usernameInput.getText().toString().trim();
            if (username.isEmpty()) {
                Toast.makeText(this, R.string.username_required, Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit()
                    .putString(KEY_USERNAME, username)
                    .putInt(KEY_TOTAL_SCORE, 0)
                    .apply();

            Intent intent = new Intent(UsernameActivity.this, GameMenuActivity.class);
            startActivity(intent);
        });
    }
}
