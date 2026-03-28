package com.sehs4542.gp.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
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
        Button nextLevelButton = findViewById(R.id.button_next_level);

        SharedPreferences prefs = getSharedPreferences(UsernameActivity.PREFS_NAME, MODE_PRIVATE);
        String username = prefs.getString(UsernameActivity.KEY_USERNAME, getString(R.string.default_username));

        int gameNumber = getIntent().getIntExtra(GameActivity.EXTRA_GAME_NUMBER, 1);
        int gameScore  = getIntent().getIntExtra(GameActivity.EXTRA_GAME_SCORE, 0);
        int totalScore = getIntent().getIntExtra(GameActivity.EXTRA_TOTAL_SCORE, gameScore);
        int memoryLevel = getIntent().getIntExtra(GameActivity.EXTRA_MEMORY_LEVEL, 1);

        usernameText.setText(getString(R.string.player_name_template, username));
        totalScoreText.setText(getString(R.string.total_score_template, totalScore));

        if (gameNumber == GameActivity.GAME_NUMBER_MEMORY) {
            gameScoreText.setText(getString(R.string.memory_level_score_template, memoryLevel, gameScore));
        } else {
            gameScoreText.setText(getString(R.string.game_score_template, gameNumber, gameScore));
        }

        // Show "Next Level" button for memory game levels 1–4
        if (gameNumber == GameActivity.GAME_NUMBER_MEMORY && memoryLevel < GameActivity.MEMORY_LEVEL_COUNT) {
            nextLevelButton.setVisibility(View.VISIBLE);
            backButton.setVisibility(View.GONE);
            int nextLevel = memoryLevel + 1;
            nextLevelButton.setOnClickListener(view -> {
                Intent intent = new Intent(ScoreActivity.this, GameActivity.class);
                intent.putExtra(GameActivity.EXTRA_GAME_NUMBER, GameActivity.GAME_NUMBER_MEMORY);
                intent.putExtra(GameActivity.EXTRA_MEMORY_LEVEL, nextLevel);
                startActivity(intent);
                finish();
            });
        } else {
            backButton.setOnClickListener(view -> {
                Intent intent = new Intent(ScoreActivity.this, GameMenuActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
            });
        }
    }
}
