package com.sehs4542.gp.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ScoreActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);

        TextView usernameText = findViewById(R.id.text_username);
        TextView gameScoreText = findViewById(R.id.text_game_score);
        TextView totalScoreText = findViewById(R.id.text_total_score);
        Button backButton = findViewById(R.id.button_back_menu);

        SharedPreferences prefs = getSharedPreferences(UsernameActivity.PREFS_NAME, MODE_PRIVATE);
        String username = prefs.getString(UsernameActivity.KEY_USERNAME, getString(R.string.default_username));

        int gameNumber = getIntent().getIntExtra(GameActivity.EXTRA_GAME_NUMBER, 1);
        int gameScore = getIntent().getIntExtra(GameActivity.EXTRA_GAME_SCORE, 0);
        int totalScore = getIntent().getIntExtra(GameActivity.EXTRA_TOTAL_SCORE, gameScore);

        usernameText.setText(getString(R.string.player_name_template, username));
        gameScoreText.setText(getString(R.string.game_score_template, gameNumber, gameScore));
        totalScoreText.setText(getString(R.string.total_score_template, totalScore));

        backButton.setOnClickListener(view -> {
            Intent intent = new Intent(ScoreActivity.this, GameMenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}
