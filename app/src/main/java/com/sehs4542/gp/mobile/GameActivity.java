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
    static final String EXTRA_MEMORY_LEVEL = "extra_memory_level";
    static final int    GAME_NUMBER_MEMORY  = 1;

    // --- Memory game level configs (index = level - 1) ---
    static final int   MEMORY_LEVEL_COUNT   = 5;
    private static final int[] MEMORY_LEVEL_COLS    = {4, 4, 4, 4, 4};
    private static final int[] MEMORY_LEVEL_ROWS    = {4, 4, 4, 5, 5};
    private static final int[] MEMORY_LEVEL_TIME_SEC = {60, 50, 40, 55, 45};
    private static final long[] MEMORY_LEVEL_HIDE_MS = {700, 600, 500, 600, 500};

    private static final int MEMORY_MIN_SCORE = 10;

    private static final int WHAC_GRID_SIZE = 3;
    private static final int SNAKE_GRID_SIZE = 10;
    private static final int SNAKE_SCORE_PER_SEGMENT = 5;

    private static final long WHAC_DURATION_MS = 20000;
    private static final long WHAC_MOLE_INTERVAL_MS = 700;
    private static final long SNAKE_TICK_MS = 450;
    private static final String MOLE_EMOJI = "\uD83D\uDC39";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

    private FrameLayout gameContainer;
    private int gameNumber;
    private int memoryLevel;
    private String gameName;

    private CountDownTimer memoryTimer;

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
    private int pendingSnakeDx;
    private int pendingSnakeDy;
    private boolean hasPendingSnakeDirection;
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
        memoryLevel = getIntent().getIntExtra(EXTRA_MEMORY_LEVEL, 1);
        gameName = getGameName(gameNumber);

        gameTitle.setText(gameName);
        gameDescription.setText(getGameDescription(gameNumber));

        switch (gameNumber) {
            case GAME_NUMBER_MEMORY:
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
        if (memoryTimer != null) {
            memoryTimer.cancel();
        }
        if (whacTimer != null) {
            whacTimer.cancel();
        }
        handler.removeCallbacksAndMessages(null);
        whacRunning = false;
        snakeRunning = false;
    }

    private void setupMemoryGame() {
        int lvlIdx = memoryLevel - 1;
        int cols = MEMORY_LEVEL_COLS[lvlIdx];
        int rows = MEMORY_LEVEL_ROWS[lvlIdx];
        int pairs = (cols * rows) / 2;
        int timeSec = MEMORY_LEVEL_TIME_SEC[lvlIdx];
        long hideMs = MEMORY_LEVEL_HIDE_MS[lvlIdx];

        View memoryView = getLayoutInflater().inflate(R.layout.layout_game_memory, gameContainer, false);
        gameContainer.addView(memoryView);

        TextView levelText   = memoryView.findViewById(R.id.text_memory_level);
        TextView timerText   = memoryView.findViewById(R.id.text_memory_timer);
        TextView matchesText = memoryView.findViewById(R.id.text_memory_matches);
        TextView movesText   = memoryView.findViewById(R.id.text_memory_moves);
        GridLayout grid      = memoryView.findViewById(R.id.grid_memory);

        grid.setRowCount(rows);
        grid.setColumnCount(cols);

        levelText.setText(getString(R.string.memory_level_template, memoryLevel));
        timerText.setText(getString(R.string.memory_timer_template, timeSec));

        // Build card value list: enough symbols for 'pairs' pairs
        String[] allSymbols = {"🍎", "🍌", "🍇", "🍉", "🍓", "🍒", "🍍", "🥝", "🍑", "🍐",
                               "🍊", "🥭", "🍋", "🫐", "🥥", "🍈"};
        List<String> values = new ArrayList<>();
        for (int p = 0; p < pairs; p++) {
            values.add(allSymbols[p]);
            values.add(allSymbols[p]);
        }
        Collections.shuffle(values);

        List<Button> cards = new ArrayList<>();
        boolean[] matched = new boolean[values.size()];

        class MemoryState {
            int firstIndex = -1;
            boolean busy;
            int moves;
            int matches;
            int remainingSeconds = timeSec;
            boolean finished;
        }
        MemoryState state = new MemoryState();

        matchesText.setText(getString(R.string.memory_matches_template, state.matches, pairs));
        movesText.setText(getString(R.string.memory_moves_template, state.moves));

        int cardSize = dpToPx(60);
        for (int i = 0; i < values.size(); i++) {
            Button card = new Button(this);
            card.setAllCaps(false);
            card.setText(R.string.memory_card_back);
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = cardSize;
            params.height = cardSize;
            params.rowSpec = GridLayout.spec(i / cols);
            params.columnSpec = GridLayout.spec(i % cols);
            card.setLayoutParams(params);

            int index = i;
            card.setOnClickListener(view -> {
                if (state.busy || state.finished || matched[index]) {
                    return;
                }
                if (index == state.firstIndex) {
                    return;
                }
                card.setText(values.get(index));
                card.setEnabled(false);

                if (state.firstIndex == -1) {
                    state.firstIndex = index;
                    return;
                }

                state.moves += 1;
                movesText.setText(getString(R.string.memory_moves_template, state.moves));

                int previousIndex = state.firstIndex;
                Button previousCard = cards.get(previousIndex);
                if (values.get(previousIndex).equals(values.get(index))) {
                    matched[previousIndex] = true;
                    matched[index] = true;
                    state.matches += 1;
                    matchesText.setText(getString(R.string.memory_matches_template, state.matches, pairs));
                    state.firstIndex = -1;

                    if (state.matches == pairs) {
                        state.finished = true;
                        if (memoryTimer != null) {
                            memoryTimer.cancel();
                        }
                        int wrongMoves = Math.max(0, state.moves - state.matches);
                        int baseScore = pairs * 10;
                        int timeBonus = state.remainingSeconds * memoryLevel * 2;
                        int penalty = wrongMoves * 3;
                        int levelScore = Math.max(MEMORY_MIN_SCORE, baseScore + timeBonus - penalty);
                        handler.postDelayed(() -> finishMemoryLevel(levelScore), 400);
                    }
                } else {
                    state.busy = true;
                    handler.postDelayed(() -> {
                        previousCard.setText(R.string.memory_card_back);
                        previousCard.setEnabled(true);
                        card.setText(R.string.memory_card_back);
                        card.setEnabled(true);
                        state.firstIndex = -1;
                        state.busy = false;
                    }, hideMs);
                }
            });

            cards.add(card);
            grid.addView(card);
        }

        // Countdown timer
        memoryTimer = new CountDownTimer((long) timeSec * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                state.remainingSeconds = (int) (millisUntilFinished / 1000);
                timerText.setText(getString(R.string.memory_timer_template, state.remainingSeconds));
            }

            @Override
            public void onFinish() {
                if (state.finished) {
                    return;
                }
                state.finished = true;
                state.remainingSeconds = 0;
                timerText.setText(getString(R.string.memory_time_up));
                // Disable all remaining cards
                for (Button c : cards) {
                    c.setEnabled(false);
                }
                // Partial score: only matched pairs count, no time bonus
                int partialScore = state.matches * 10;
                handler.postDelayed(() -> finishMemoryLevel(partialScore), 800);
            }
        };
        memoryTimer.start();
    }

    /** After a memory level finishes, go to next level or ScoreActivity. */
    private void finishMemoryLevel(int levelScore) {
        SharedPreferences prefs = getSharedPreferences(UsernameActivity.PREFS_NAME, MODE_PRIVATE);
        int currentTotal = prefs.getInt(UsernameActivity.KEY_TOTAL_SCORE, 0);
        int updatedTotal = currentTotal + levelScore;
        prefs.edit().putInt(UsernameActivity.KEY_TOTAL_SCORE, updatedTotal).apply();

        Intent intent = new Intent(GameActivity.this, ScoreActivity.class);
        intent.putExtra(EXTRA_GAME_NUMBER, gameNumber);
        intent.putExtra(EXTRA_GAME_SCORE, levelScore);
        intent.putExtra(EXTRA_TOTAL_SCORE, updatedTotal);
        intent.putExtra(EXTRA_GAME_NAME, gameName);
        intent.putExtra(EXTRA_MEMORY_LEVEL, memoryLevel);
        startActivity(intent);
        finish();
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
        moleButton.setText(MOLE_EMOJI);
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
        if (hasPendingSnakeDirection) {
            return;
        }
        if (snakeDx == -dx && snakeDy == -dy) {
            return;
        }
        pendingSnakeDx = dx;
        pendingSnakeDy = dy;
        hasPendingSnakeDirection = true;
    }

    private void applyPendingSnakeDirection() {
        if (!hasPendingSnakeDirection) {
            return;
        }
        snakeDx = pendingSnakeDx;
        snakeDy = pendingSnakeDy;
        hasPendingSnakeDirection = false;
    }

    private void moveSnake() {
        if (snakeBody == null || snakeBody.isEmpty()) {
            return;
        }
        applyPendingSnakeDirection();
        Point head = snakeBody.peekFirst();
        Point newHead = new Point(head.x + snakeDx, head.y + snakeDy);
        if (newHead.x < 0 || newHead.x >= SNAKE_GRID_SIZE || newHead.y < 0 || newHead.y >= SNAKE_GRID_SIZE) {
            endSnakeGame();
            return;
        }
        boolean willGrow = snakeFood != null && newHead.equals(snakeFood);
        int index = 0;
        int size = snakeBody.size();
        for (Point segment : snakeBody) {
            boolean isTail = index == size - 1;
            if (segment.equals(newHead)) {
                if (willGrow || !isTail) {
                    endSnakeGame();
                    return;
                }
                break;
            }
            index++;
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
            int idx = segment.y * SNAKE_GRID_SIZE + segment.x;
            snakeCells.get(idx).setBackgroundColor(isHead ? headColor : bodyColor);
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
        int score = snakeBody == null ? 0 : snakeBody.size() * SNAKE_SCORE_PER_SEGMENT;
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
            case GAME_NUMBER_MEMORY:
                return getString(R.string.memory_game_level_name, memoryLevel);
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
            case GAME_NUMBER_MEMORY: {
                int lvlIdx = memoryLevel - 1;
                int cols = MEMORY_LEVEL_COLS[lvlIdx];
                int rows = MEMORY_LEVEL_ROWS[lvlIdx];
                int pairs = (cols * rows) / 2;
                int timeSec = MEMORY_LEVEL_TIME_SEC[lvlIdx];
                return getString(R.string.memory_level_description, memoryLevel, pairs, timeSec);
            }
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
        intent.putExtra(EXTRA_MEMORY_LEVEL, memoryLevel);
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
