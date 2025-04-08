package ca.kevin.myfitnessapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import ca.kevin.myfitnessapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        FirebaseApp.initializeApp(this);
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() != null) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        Button registerBtn = findViewById(R.id.btnRegister);
        Button loginBtn = findViewById(R.id.btnLogin);

        registerBtn.setOnClickListener(v -> registerUser());
        loginBtn.setOnClickListener(v -> loginUser());

        fetchAllUsers();
    }

    private void registerUser() {
        EditText emailInput = findViewById(R.id.editEmail);
        EditText passwordInput = findViewById(R.id.editPassword);

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Enter both email and password", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        handleAuthError(task.getException(), "Registration failed");
                    }
                });
    }

    private void loginUser() {
        EditText emailInput = findViewById(R.id.editEmail);
        EditText passwordInput = findViewById(R.id.editPassword);

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password required", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        Toast.makeText(this, "Welcome, " + user.getEmail(), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, DashboardActivity.class));
                        finish();
                    } else {
                        handleAuthError(task.getException(), "Login failed");
                    }
                });
    }

    private void fetchAllUsers() {
        firestore.collection("users")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (DocumentSnapshot doc : task.getResult()) {
                            String name = doc.getString("name");
                            Log.d(TAG, "User found: " + (name != null ? name : "Unnamed"));
                        }
                    } else {
                        Log.e(TAG, "User fetch failed", task.getException());
                        Toast.makeText(this, "Could not retrieve users", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void handleAuthError(Exception e, String context) {
        if (e instanceof FirebaseAuthWeakPasswordException) {
            Toast.makeText(this, "Weak password. Please use a stronger one.", Toast.LENGTH_SHORT).show();
        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
            Toast.makeText(this, "Invalid email or password", Toast.LENGTH_SHORT).show();
        } else if (e instanceof FirebaseAuthInvalidUserException) {
            Toast.makeText(this, "User not found. Please register.", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, context + ": " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
