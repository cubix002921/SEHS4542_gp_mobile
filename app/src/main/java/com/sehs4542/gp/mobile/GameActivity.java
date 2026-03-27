package com.sehs4542.gp.mobile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {
    static final String EXTRA_GAME_NUMBER = "extra_game_number";
    static final String EXTRA_GAME_SCORE = "extra_game_score";
    static final String EXTRA_TOTAL_SCORE = "extra_total_score";
    static final String EXTRA_GAME_NAME = "extra_game_name";

    private static final int MEMORY_GRID_SIZE = 4;
    private static final int MEMORY_PAIRS = 8;
    private static final int WHAC_GRID_SIZE = 3;
    private static final int SNAKE_GRID_SIZE = 10;

    private static final long MEMORY_HIDE_DELAY_MS = 700;
    private static final long WHAC_DURATION_MS = 20000;
    private static final long WHAC_MOLE_INTERVAL_MS = 700;
    private static final long SNAKE_TICK_MS = 450;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private FrameLayout gameContainer;
    private int gameNumber;
    private String gameName;

    private CountDownTimer whacTimer;
    private Runnable whacMoleRunnable;
    private boolean whacRunning;
    private int whacScore;
    private int whacMoleIndex = -1;
    private List<Button> whacButtons;
    private TextView whacTimerText;
    private TextView whacScoreText;

    private Runnable snakeRunnable;
    private boolean snakeRunning;
    private Deque<Point> snakeBody;
    private Point snakeFood;
    private int snakeDx = 1;
    private int snakeDy = 0;
    private List<TextView> snakeCells;
    private TextView snakeScoreText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        gameContainer = findViewById(R.id.game_container);
        TextView gameTitle = findViewById(R.id.text_game_title);
        TextView gameDescription = findViewById(R.id.text_game_description);

        gameNumber = getIntent().getIntExtra(EXTRA_GAME_NUMBER, 1);
        gameName = getGameName(gameNumber);

        gameTitle.setText(gameName);
        gameDescription.setText(getGameDescription(gameNumber));

        switch (gameNumber) {
            case 1:
                setupMemoryGame();
                break;
            case 2:
                setupWhacGame();
                break;
            case 3:
                setupSnakeGame();
                break;
            default:
                setupPlaceholderGame();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (whacTimer != null) {
            whacTimer.cancel();
        }
        handler.removeCallbacksAndMessages(null);
        whacRunning = false;
        snakeRunning = false;
    }

    private void setupMemoryGame() {
        View memoryView = getLayoutInflater().inflate(R.layout.layout_game_memory, gameContainer, false);
        gameContainer.addView(memoryView);

        TextView matchesText = memoryView.findViewById(R.id.text_memory_matches);
        TextView movesText = memoryView.findViewById(R.id.text_memory_moves);
        GridLayout grid = memoryView.findViewById(R.id.grid_memory);
        grid.setRowCount(MEMORY_GRID_SIZE);
        grid.setColumnCount(MEMORY_GRID_SIZE);

        List<String> values = new ArrayList<>();
        String[] symbols = new String[]{"A", "B", "C", "D", "E", "F", "G", "H"};
        for (String symbol : symbols) {
            values.add(symbol);
            values.add(symbol);
        }
        Collections.shuffle(values);

        List<Button> cards = new ArrayList<>();
        boolean[] matched = new boolean[values.size()];
        int[] firstIndex = {-1};
        boolean[] busy = {false};
        int[] moves = {0};
        int[] matches = {0};

        matchesText.setText(getString(R.string.memory_matches_template, matches[0]));
        movesText.setText(getString(R.string.memory_moves_template, moves[0]));

        int cardSize = dpToPx(64);
        for (int i = 0; i < values.size(); i++) {
            Button card = new Button(this);
            card.setAllCaps(false);
            card.setText(R.string.memory_card_back);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cardSize;
            params.height = cardSize;
            params.rowSpec = GridLayout.spec(i / MEMORY_GRID_SIZE);
            params.columnSpec = GridLayout.spec(i % MEMORY_GRID_SIZE);
            card.setLayoutParams(params);

            int index = i;
            card.setOnClickListener(view -> {
                if (busy[0] || matched[index]) {
                    return;
                }
                if (index == firstIndex[0]) {
                    return;
                }
                card.setText(values.get(index));
                card.setEnabled(false);

                if (firstIndex[0] == -1) {
                    firstIndex[0] = index;
                    return;
                }

                moves[0] += 1;
                movesText.setText(getString(R.string.memory_moves_template, moves[0]));

                int previousIndex = firstIndex[0];
                Button previousCard = cards.get(previousIndex);
                if (values.get(previousIndex).equals(values.get(index))) {
                    matched[previousIndex] = true;
                    matched[index] = true;
                    matches[0] += 1;
                    matchesText.setText(getString(R.string.memory_matches_template, matches[0]));
                    firstIndex[0] = -1;

                    if (matches[0] == MEMORY_PAIRS) {
                        int baseScore = MEMORY_PAIRS * 10;
                        int penalty = Math.max(0, (moves[0] - matches[0]) * 2);
                        int finalScore = Math.max(10, baseScore - penalty);
                        handler.postDelayed(() -> finishGame(finalScore), 400);
                    }
                } else {
                    busy[0] = true;
                    handler.postDelayed(() -> {
                        previousCard.setText(R.string.memory_card_back);
                        previousCard.setEnabled(true);
                        card.setText(R.string.memory_card_back);
                        card.setEnabled(true);
                        firstIndex[0] = -1;
                        busy[0] = false;
                    }, MEMORY_HIDE_DELAY_MS);
                }
            });

            cards.add(card);
            grid.addView(card);
        }
    }

    private void setupWhacGame() {
        View whacView = getLayoutInflater().inflate(R.layout.layout_game_whac, gameContainer, false);
        gameContainer.addView(whacView);

        whacTimerText = whacView.findViewById(R.id.text_whac_timer);
        whacScoreText = whacView.findViewById(R.id.text_whac_score);
        Button startButton = whacView.findViewById(R.id.button_whac_start);
        GridLayout grid = whacView.findViewById(R.id.grid_whac);
        grid.setRowCount(WHAC_GRID_SIZE);
        grid.setColumnCount(WHAC_GRID_SIZE);

        whacButtons = new ArrayList<>();
        int holeSize = dpToPx(80);
        for (int i = 0; i < WHAC_GRID_SIZE * WHAC_GRID_SIZE; i++) {
            Button hole = new Button(this);
            hole.setAllCaps(false);
            hole.setText("");
            hole.setEnabled(false);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = holeSize;
            params.height = holeSize;
            params.rowSpec = GridLayout.spec(i / WHAC_GRID_SIZE);
            params.columnSpec = GridLayout.spec(i % WHAC_GRID_SIZE);
            hole.setLayoutParams(params);

            int index = i;
            hole.setOnClickListener(view -> {
                if (!whacRunning) {
                    return;
                }
                if (index == whacMoleIndex) {
                    whacScore += 1;
                    whacScoreText.setText(getString(R.string.whac_score_template, whacScore));
                    showMole();
                }
            });

            whacButtons.add(hole);
            grid.addView(hole);
        }

        whacTimerText.setText(getString(R.string.whac_timer_template, (int) (WHAC_DURATION_MS / 1000)));
        whacScoreText.setText(getString(R.string.whac_score_template, 0));

        startButton.setOnClickListener(view -> startWhacGame(startButton));
    }

    private void startWhacGame(Button startButton) {
        if (whacRunning) {
            return;
        }
        whacRunning = true;
        whacScore = 0;
        whacMoleIndex = -1;
        whacScoreText.setText(getString(R.string.whac_score_template, whacScore));
        startButton.setEnabled(false);

        for (Button hole : whacButtons) {
            hole.setEnabled(true);
        }

        if (whacTimer != null) {
            whacTimer.cancel();
        }
        whacTimer = new CountDownTimer(WHAC_DURATION_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                whacTimerText.setText(getString(R.string.whac_timer_template, (int) (millisUntilFinished / 1000)));
            }

            @Override
            public void onFinish() {
                whacRunning = false;
                clearMole();
                if (whacMoleRunnable != null) {
                    handler.removeCallbacks(whacMoleRunnable);
                }
                finishGame(whacScore);
            }
        };
        whacTimer.start();

        whacMoleRunnable = new Runnable() {
            @Override
            public void run() {
                if (!whacRunning) {
                    return;
                }
                showMole();
                handler.postDelayed(this, WHAC_MOLE_INTERVAL_MS);
            }
        };
        handler.post(whacMoleRunnable);
    }

    private void showMole() {
        if (whacButtons == null || whacButtons.isEmpty()) {
            return;
        }
        int previousIndex = whacMoleIndex;
        clearMole();
        int nextIndex = random.nextInt(whacButtons.size());
        if (whacButtons.size() > 1) {
            while (nextIndex == previousIndex) {
                nextIndex = random.nextInt(whacButtons.size());
            }
        }
        whacMoleIndex = nextIndex;
        Button moleButton = whacButtons.get(whacMoleIndex);
        moleButton.setText("\uD83D\uDC39");
    }

    private void clearMole() {
        if (whacMoleIndex >= 0 && whacButtons != null && whacMoleIndex < whacButtons.size()) {
            whacButtons.get(whacMoleIndex).setText("");
        }
        whacMoleIndex = -1;
    }

    private void setupSnakeGame() {
        View snakeView = getLayoutInflater().inflate(R.layout.layout_game_snake, gameContainer, false);
        gameContainer.addView(snakeView);

        snakeScoreText = snakeView.findViewById(R.id.text_snake_score);
        GridLayout grid = snakeView.findViewById(R.id.grid_snake);
        grid.setRowCount(SNAKE_GRID_SIZE);
        grid.setColumnCount(SNAKE_GRID_SIZE);

        int cellSize = dpToPx(22);
        snakeCells = new ArrayList<>();
        for (int i = 0; i < SNAKE_GRID_SIZE * SNAKE_GRID_SIZE; i++) {
            TextView cell = new TextView(this);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cellSize;
            params.height = cellSize;
            params.rowSpec = GridLayout.spec(i / SNAKE_GRID_SIZE);
            params.columnSpec = GridLayout.spec(i % SNAKE_GRID_SIZE);
            cell.setLayoutParams(params);
            cell.setBackgroundColor(ContextCompat.getColor(this, R.color.snake_empty));
            snakeCells.add(cell);
            grid.addView(cell);
        }

        Button upButton = snakeView.findViewById(R.id.button_snake_up);
        Button leftButton = snakeView.findViewById(R.id.button_snake_left);
        Button downButton = snakeView.findViewById(R.id.button_snake_down);
        Button rightButton = snakeView.findViewById(R.id.button_snake_right);

        upButton.setOnClickListener(view -> setSnakeDirection(0, -1));
        leftButton.setOnClickListener(view -> setSnakeDirection(-1, 0));
        downButton.setOnClickListener(view -> setSnakeDirection(0, 1));
        rightButton.setOnClickListener(view -> setSnakeDirection(1, 0));

        int startX = SNAKE_GRID_SIZE / 2;
        int startY = SNAKE_GRID_SIZE / 2;
        snakeBody = new ArrayDeque<>();
        snakeBody.addFirst(new Point(startX, startY));
        snakeBody.addLast(new Point(startX - 1, startY));
        snakeBody.addLast(new Point(startX - 2, startY));
        snakeDx = 1;
        snakeDy = 0;

        snakeFood = spawnSnakeFood();
        snakeRunning = true;
        updateSnakeGrid();

        snakeRunnable = new Runnable() {
            @Override
            public void run() {
                if (!snakeRunning) {
                    return;
                }
                moveSnake();
                handler.postDelayed(this, SNAKE_TICK_MS);
            }
        };
        handler.postDelayed(snakeRunnable, SNAKE_TICK_MS);
    }

    private void setSnakeDirection(int dx, int dy) {
        if (snakeDx == -dx && snakeDy == -dy) {
            return;
        }
        snakeDx = dx;
        snakeDy = dy;
    }

    private void moveSnake() {
        if (snakeBody == null || snakeBody.isEmpty()) {
            return;
        }
        Point head = snakeBody.peekFirst();
        Point newHead = new Point(head.x + snakeDx, head.y + snakeDy);
        if (newHead.x < 0 || newHead.x >= SNAKE_GRID_SIZE || newHead.y < 0 || newHead.y >= SNAKE_GRID_SIZE) {
            endSnakeGame();
            return;
        }
        Point tail = snakeBody.peekLast();
        boolean hitTail = newHead.equals(tail);
        for (Point segment : snakeBody) {
            if (segment.equals(newHead)) {
                if (!segment.equals(tail)) {
                    endSnakeGame();
                    return;
                }
                break;
            }
        }
        boolean willGrow = snakeFood != null && newHead.equals(snakeFood);
        if (hitTail && willGrow) {
            endSnakeGame();
            return;
        }
        snakeBody.addFirst(newHead);
        if (willGrow) {
            snakeFood = spawnSnakeFood();
            if (!snakeRunning) {
                return;
            }
        } else {
            snakeBody.removeLast();
        }
        updateSnakeGrid();
    }

    private Point spawnSnakeFood() {
        List<Point> openCells = new ArrayList<>();
        for (int y = 0; y < SNAKE_GRID_SIZE; y++) {
            for (int x = 0; x < SNAKE_GRID_SIZE; x++) {
                Point candidate = new Point(x, y);
                boolean occupied = false;
                for (Point segment : snakeBody) {
                    if (segment.equals(candidate)) {
                        occupied = true;
                        break;
                    }
                }
                if (!occupied) {
                    openCells.add(candidate);
                }
            }
        }
        if (openCells.isEmpty()) {
            endSnakeGame();
            return null;
        }
        return openCells.get(random.nextInt(openCells.size()));
    }

    private void updateSnakeGrid() {
        if (snakeCells == null) {
            return;
        }
        int emptyColor = ContextCompat.getColor(this, R.color.snake_empty);
        int bodyColor = ContextCompat.getColor(this, R.color.snake_body);
        int headColor = ContextCompat.getColor(this, R.color.snake_head);
        int foodColor = ContextCompat.getColor(this, R.color.snake_food);

        for (TextView cell : snakeCells) {
            cell.setBackgroundColor(emptyColor);
        }
        if (snakeFood != null) {
            snakeCells.get(snakeFood.y * SNAKE_GRID_SIZE + snakeFood.x).setBackgroundColor(foodColor);
        }
        boolean isHead = true;
        for (Point segment : snakeBody) {
            int index = segment.y * SNAKE_GRID_SIZE + segment.x;
            snakeCells.get(index).setBackgroundColor(isHead ? headColor : bodyColor);
            isHead = false;
        }
        snakeScoreText.setText(getString(R.string.snake_score_template, snakeBody.size()));
    }

    private void endSnakeGame() {
        if (!snakeRunning) {
            return;
        }
        snakeRunning = false;
        handler.removeCallbacks(snakeRunnable);
        int score = snakeBody == null ? 0 : snakeBody.size() * 5;
        finishGame(score);
    }

    private void setupPlaceholderGame() {
        Button completeButton = new Button(this);
        completeButton.setText(R.string.complete_game);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER;
        completeButton.setLayoutParams(params);
        completeButton.setOnClickListener(view -> finishGame(10 * gameNumber));
        gameContainer.addView(completeButton);
    }

    private String getGameName(int gameNumber) {
        switch (gameNumber) {
            case 1:
                return getString(R.string.game_name_memory);
            case 2:
                return getString(R.string.game_name_whac);
            case 3:
                return getString(R.string.game_name_snake);
            default:
                return getString(R.string.game_title_template, gameNumber);
        }
    }

    private String getGameDescription(int gameNumber) {
        switch (gameNumber) {
            case 1:
                return getString(R.string.game_description_memory);
            case 2:
                return getString(R.string.game_description_whac);
            case 3:
                return getString(R.string.game_description_snake);
            default:
                return getString(R.string.game_description_template, gameNumber);
        }
    }

    private void finishGame(int gameScore) {
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
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        ));
    }
}
