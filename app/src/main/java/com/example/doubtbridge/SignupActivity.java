package com.example.doubtbridge;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    TextInputEditText nameInput, emailInput, passwordInput;
    Spinner roleSpinner;
    MaterialButton signupBtn;
    TextView loginText;

    FirebaseAuth auth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        roleSpinner = findViewById(R.id.roleSpinner);
        signupBtn = findViewById(R.id.signupBtn);
        loginText = findViewById(R.id.loginText);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        String[] roles = {"Student", "Teacher"};
        roleSpinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, roles));

        signupBtn.setOnClickListener(v -> registerUser());
        loginText.setOnClickListener(v -> finish());
    }

    private void registerUser() {
        String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
        String email = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";
        String password = passwordInput.getText() != null ? passwordInput.getText().toString().trim() : "";
        String role = roleSpinner.getSelectedItem().toString().toLowerCase();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        signupBtn.setEnabled(false);
        signupBtn.setText("Creating account...");

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener(result -> {
                String uid = auth.getCurrentUser().getUid();
                Map<String, Object> user = new HashMap<>();
                user.put("name", name);
                user.put("email", email);
                user.put("role", role);
                user.put("uid", uid);
                user.put("createdAt", System.currentTimeMillis());

                db.collection("users").document(uid).set(user)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Account created! Please login.", Toast.LENGTH_SHORT).show();
                        auth.signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    });
            })
            .addOnFailureListener(e -> {
                signupBtn.setEnabled(true);
                signupBtn.setText("Sign Up");
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}
