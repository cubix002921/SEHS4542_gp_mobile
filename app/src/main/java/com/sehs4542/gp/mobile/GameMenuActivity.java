package com.sehs4542.gp.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GameMenuActivity extends AppCompatActivity {
    private TextView greetingText;
    private TextView totalScoreText;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_menu);

        prefs = getSharedPreferences(UsernameActivity.PREFS_NAME, MODE_PRIVATE);

        greetingText = findViewById(R.id.text_greeting);
        totalScoreText = findViewById(R.id.text_total_score);

        Button game1Button = findViewById(R.id.button_game1);
        Button game2Button = findViewById(R.id.button_game2);
        Button game3Button = findViewById(R.id.button_game3);
        Button game4Button = findViewById(R.id.button_game4);
        Button game5Button = findViewById(R.id.button_game5);
        Button game6Button = findViewById(R.id.button_game6);

        game1Button.setOnClickListener(view -> launchGame(1));
        game2Button.setOnClickListener(view -> launchGame(2));
        game3Button.setOnClickListener(view -> launchGame(3));
        game4Button.setOnClickListener(view -> launchGame(4));
        game5Button.setOnClickListener(view -> launchGame(5));
        game6Button.setOnClickListener(view -> launchGame(6));
    }

    @Override
    protected void onResume() {
        super.onResume();
        String username = prefs.getString(UsernameActivity.KEY_USERNAME, getString(R.string.default_username));
        int totalScore = prefs.getInt(UsernameActivity.KEY_TOTAL_SCORE, 0);

        greetingText.setText(getString(R.string.greeting_template, username));
        totalScoreText.setText(getString(R.string.total_score_template, totalScore));
    }

    private void launchGame(int gameNumber) {
        Intent intent = new Intent(GameMenuActivity.this, GameActivity.class);
        intent.putExtra(GameActivity.EXTRA_GAME_NUMBER, gameNumber);
        startActivity(intent);
    }
}
