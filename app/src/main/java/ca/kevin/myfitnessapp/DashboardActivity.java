package ca.kevin.myfitnessapp;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DashboardActivity extends Activity {

    private EditText goalInput;
    private TextView caloriesText, suggestionBox;
    private FirebaseFirestore firestore;
    private FirebaseUser user;

    private final List<List<String>> upperWorkouts = new ArrayList<>();
    private final List<List<String>> lowerWorkouts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        firestore = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        goalInput = findViewById(R.id.editGoal);
        caloriesText = findViewById(R.id.textCaloriesBurned);
        suggestionBox = findViewById(R.id.textSuggestions);

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            finish();
        });

        findViewById(R.id.btnSetGoal).setOnClickListener(v -> saveGoal());
        findViewById(R.id.btnAddUpperWorkout).setOnClickListener(v -> suggestWorkout("upper"));
        findViewById(R.id.btnAddLowerWorkout).setOnClickListener(v -> suggestWorkout("lower"));
        findViewById(R.id.btnCompleteWorkout).setOnClickListener(v -> recordWorkout());

        initializeWorkoutLists();
        updateCalories();
    }

    private void initializeWorkoutLists() {
        upperWorkouts.add(List.of("Push ups", "Hinged Dumbbell Rows", "Shoulder Press", "Bicep Curls", "Dumbbell Flyes", "Tricep Kickbacks"));
        upperWorkouts.add(List.of("Incline Dumbbell Press", "Dumbbell Lateral Raises", "Dumbbell Front Raises", "Dumbbell Shrugs", "Hammer Curls", "Overhead Triceps Extensions"));
        upperWorkouts.add(List.of("Flat Dumbbell Bench Press", "One-arm Dumbbell Row", "Dumbbell Arnold Press", "Concentration Curls", "Reverse Flyes", "Dumbbell Skull Crushers"));

        lowerWorkouts.add(List.of("Dumbbell Goblet Squats", "Dumbbell Lunges", "Dumbbell Romanian Deadlifts", "Dumbbell Step Ups", "Dumbbell Glute Bridges", "Dumbbell Calf Raises"));
        lowerWorkouts.add(List.of("Dumbbell Sumo Squats", "Bulgarian Split Squats", "Dumbbell Side Lunges", "Single-leg Deadlifts", "Dumbbell Hip Thrusts", "Dumbbell Jump Squats"));
    }

    private void suggestWorkout(String type) {
        List<String> routine;
        String heading;

        if (type.equals("upper")) {
            routine = upperWorkouts.get(new Random().nextInt(upperWorkouts.size()));
            heading = "Upper Body Workout (30 min - Dumbbells)";
        } else {
            routine = lowerWorkouts.get(new Random().nextInt(lowerWorkouts.size()));
            heading = "Lower Body Workout (30 min - Dumbbells)";
        }

        Workout suggestion = new Workout.WorkoutBuilder()
                .setTitle(heading)
                .setDuration(30)
                .addMoves(routine)
                .build();

        suggestionBox.setText(suggestion.toString());
    }

    private void saveGoal() {
        if (user == null) return;

        String input = goalInput.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Enter a goal", Toast.LENGTH_SHORT).show();
            return;
        }

        int goal = Integer.parseInt(input);
        Map<String, Object> data = new HashMap<>();
        data.put("goal", goal);

        firestore.collection("goals")
                .document(user.getUid())
                .set(data)
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Goal saved", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> {
                    Log.e("SaveGoalError", "Failed to save goal", e);
                    Toast.makeText(this, "Failed to save goal: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void recordWorkout() {
        if (user == null) return;

        Map<String, Object> workout = new HashMap<>();
        workout.put("title", "Workout Completed");
        workout.put("calories", 300);
        workout.put("sets", 3);
        workout.put("reps", 12);
        workout.put("timestamp", System.currentTimeMillis());
        workout.put("userId", user.getUid());

        firestore.collection("workouts")
                .add(workout)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Workout recorded", Toast.LENGTH_SHORT).show();
                    updateCalories();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to record workout", Toast.LENGTH_SHORT).show());
    }

    private void updateCalories() {
        if (user == null) return;

        firestore.collection("workouts")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(query -> {
                    int total = 0;
                    for (DocumentSnapshot doc : query) {
                        Long cal = doc.getLong("calories");
                        if (cal != null) total += cal;
                        Log.d("WorkoutRead", doc.getData().toString());
                    }
                    caloriesText.setText(total + " kcal");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not load workouts", Toast.LENGTH_SHORT).show();
                    Log.e("Dashboard", "updateCalories: ", e);
                });
    }

    static class Workout {
        private final String title;
        private final int duration;
        private final List<String> moves;

        private Workout(String title, int duration, List<String> moves) {
            this.title = title;
            this.duration = duration;
            this.moves = moves;
        }

        public static class WorkoutBuilder {
            private String title;
            private int duration;
            private final List<String> moves = new ArrayList<>();

            public WorkoutBuilder setTitle(String title) {
                this.title = title;
                return this;
            }

            public WorkoutBuilder setDuration(int duration) {
                this.duration = duration;
                return this;
            }

            public WorkoutBuilder addMove(String move) {
                this.moves.add(move);
                return this;
            }

            public WorkoutBuilder addMoves(List<String> moveList) {
                this.moves.addAll(moveList);
                return this;
            }

            public Workout build() {
                return new Workout(title, duration, moves);
            }
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(title).append("\nDuration: ").append(duration).append(" min\n\n");
            for (String move : moves) {
                builder.append("• ").append(move).append(" — 3 sets x 12 reps\n");
            }
            return builder.toString();
        }
    }
}
