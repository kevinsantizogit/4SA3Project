package ca.kevin.myfitnessapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

import ca.kevin.myfitnessapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private FirebaseFirestore database;
    private FirebaseAuth authentication;
    private static final String TAG = "FirebaseIntegration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        FirebaseApp.initializeApp(this);
        authentication = FirebaseAuth.getInstance();
        database = FirebaseFirestore.getInstance();

        Button btnSignUp = findViewById(R.id.btnRegister);
        Button btnSignIn = findViewById(R.id.btnLogin);


        btnSignUp.setOnClickListener(v -> createNewUser());
        btnSignIn.setOnClickListener(v -> signInUser());


        binding.fab.setOnClickListener(view -> {
            storeUserInfo("Kevin");
            Snackbar.make(view, "User data successfully stored", Snackbar.LENGTH_LONG)
                    .setAction("Action", null)
                    .setAnchorView(R.id.fab)
                    .show();
        });

        fetchUserFromDatabase();
    }

    private void createNewUser() {
        EditText emailInput = findViewById(R.id.editEmail);
        EditText passwordInput = findViewById(R.id.editPassword);
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        authentication.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(MainActivity.this, "Successfully registered!", Toast.LENGTH_SHORT).show();
                    } else {
                        processAuthException(task.getException(), "Registration Error");
                    }
                });
    }

    private void signInUser() {
        EditText emailInput = findViewById(R.id.editEmail);
        EditText passwordInput = findViewById(R.id.editPassword);
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password fields can't be blank", Toast.LENGTH_SHORT).show();
            return;
        }

        authentication.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = authentication.getCurrentUser();
                        Toast.makeText(MainActivity.this, "Welcome, " + user.getEmail(), Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(MainActivity.this, DashboardActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        processAuthException(task.getException(), "Login Issue");
                    }
                });
    }

    private void storeUserInfo(String name) {
        if (name == null || name.trim().isEmpty()) {
            Toast.makeText(this, "Name can't be left blank", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("name", name);

        database.collection("users")
                .add(userData)
                .addOnSuccessListener(documentReference -> Log.d(TAG, "User stored with ID: " + documentReference.getId()))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Failed to store user", e);
                    Toast.makeText(MainActivity.this, "Error storing user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void fetchUserFromDatabase() {
        database.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot document : task.getResult()) {
                            String userName = document.getString("name");
                            Log.d(TAG, "User fetched from DB: " + (userName != null ? userName : "unknown"));
                        }
                    } else {
                        Log.w(TAG, "User retrieval failed", task.getException());
                        Toast.makeText(MainActivity.this, "Unable to fetch user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void processAuthException(Exception exception, String errorContext) {
        if (exception instanceof FirebaseAuthWeakPasswordException) {
            Toast.makeText(this, "Password strength is weak. Use a stronger one", Toast.LENGTH_SHORT).show();
        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
            Toast.makeText(this, "Invalid credentials. Please check your email or password", Toast.LENGTH_SHORT).show();
        } else if (exception instanceof FirebaseAuthInvalidUserException) {
            Toast.makeText(this, "User not found. Please register first", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, errorContext + ": " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
