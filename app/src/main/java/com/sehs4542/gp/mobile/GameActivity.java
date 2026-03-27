package com.sehs4542.gp.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {
    static final String EXTRA_GAME_NUMBER = "extra_game_number";
    static final String EXTRA_GAME_SCORE = "extra_game_score";
    static final String EXTRA_TOTAL_SCORE = "extra_total_score";
    static final String EXTRA_GAME_NAME = "extra_game_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        int gameNumber = getIntent().getIntExtra(EXTRA_GAME_NUMBER, 1);
        String gameName = getString(R.string.game_title_template, gameNumber);

        TextView gameTitle = findViewById(R.id.text_game_title);
        TextView gameDescription = findViewById(R.id.text_game_description);
        Button completeButton = findViewById(R.id.button_complete_game);

        gameTitle.setText(gameName);
        gameDescription.setText(getString(R.string.game_description_template, gameNumber));

        completeButton.setOnClickListener(view -> {
            int gameScore = 10 * gameNumber;
            SharedPreferences prefs = getSharedPreferences(UsernameActivity.PREFS_NAME, MODE_PRIVATE);
            int currentTotal = prefs.getInt(UsernameActivity.KEY_TOTAL_SCORE, 0);
            int updatedTotal = currentTotal + gameScore;
            prefs.edit().putInt(UsernameActivity.KEY_TOTAL_SCORE, updatedTotal).apply();

            Intent intent = new Intent(GameActivity.this, ScoreActivity.class);
            intent.putExtra(EXTRA_GAME_NUMBER, gameNumber);
            intent.putExtra(EXTRA_GAME_SCORE, gameScore);
            intent.putExtra(EXTRA_TOTAL_SCORE, updatedTotal);
            intent.putExtra(EXTRA_GAME_NAME, gameName);
            startActivity(intent);
            finish();
        });
    }
}
