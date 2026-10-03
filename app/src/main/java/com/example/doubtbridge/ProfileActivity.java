package com.example.doubtbridge;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.doubtbridge.utils.AppConstants;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    MaterialToolbar toolbar;
    TextView profileAvatarBig, profileName, profileRole, tvDoubtsCount, tvBookmarksCount;
    TextInputEditText nameInput, emailInput;
    Spinner deptSpinner, classSpinner;
    MaterialButton saveBtn, btnChangePassword, btnLogout;

    FirebaseAuth auth;
    FirebaseFirestore db;
    String userId;
    String userRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        toolbar = findViewById(R.id.toolbar);
        profileAvatarBig = findViewById(R.id.profileAvatarBig);
        profileName = findViewById(R.id.profileName);
        profileRole = findViewById(R.id.profileRole);
        tvDoubtsCount = findViewById(R.id.tvDoubtsCount);
        tvBookmarksCount = findViewById(R.id.tvBookmarksCount);
        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        deptSpinner = findViewById(R.id.deptSpinner);
        classSpinner = findViewById(R.id.classSpinner);
        saveBtn = findViewById(R.id.saveBtn);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnLogout = findViewById(R.id.btnLogout);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Dept spinner (with "All Departments")
        deptSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, AppConstants.DEPARTMENTS));
        classSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, AppConstants.CLASSES));

        // Sync dept and class if teacher — actually profile can be updated for both
        AdapterView.OnItemSelectedListener listener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                // We don't necessarily NEED to update anything here for profile
                // but if we had a subject preview we would.
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        deptSpinner.setOnItemSelectedListener(listener);
        classSpinner.setOnItemSelectedListener(listener);

        loadProfile();
        loadStats();

        saveBtn.setOnClickListener(v -> saveProfile());
        btnChangePassword.setOnClickListener(v -> changePassword());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadProfile() {
        if (userId.isEmpty()) return;
        if (auth.getCurrentUser() != null) {
            emailInput.setText(auth.getCurrentUser().getEmail());
        }
        db.collection("users").document(userId).get()
            .addOnSuccessListener(doc -> {
                if (!doc.exists()) return;
                String name = doc.getString("name");
                String role = doc.getString("role");
                String dept = doc.getString("department");
                String cls  = doc.getString("className");
                userRole = role;

                nameInput.setText(name);
                profileName.setText(name != null ? name : "User");
                profileRole.setText(role != null ? (role.equals("teacher") ? "Teacher 👩‍🏫" : "Student 🎓") : "Student");
                if (name != null && !name.isEmpty()) {
                    profileAvatarBig.setText(String.valueOf(name.charAt(0)).toUpperCase());
                }

                // Set spinners
                if (dept != null) {
                    for (int i = 0; i < deptSpinner.getCount(); i++) {
                        if (deptSpinner.getItemAtPosition(i).toString().equals(dept)) {
                            deptSpinner.setSelection(i); break;
                        }
                    }
                }
                if (cls != null) {
                    for (int i = 0; i < classSpinner.getCount(); i++) {
                        if (classSpinner.getItemAtPosition(i).toString().equals(cls)) {
                            classSpinner.setSelection(i); break;
                        }
                    }
                }
            });
    }

    private void loadStats() {
        if (userId.isEmpty()) return;
        // Doubts count
        db.collection("doubts").whereEqualTo("userId", userId).get()
            .addOnSuccessListener(snap -> tvDoubtsCount.setText(String.valueOf(snap.size())));
        // Bookmarks count
        db.collection("bookmarks").whereEqualTo("userId", userId).get()
            .addOnSuccessListener(snap -> tvBookmarksCount.setText(String.valueOf(snap.size())));
    }

    private void saveProfile() {
        String name = nameInput.getText() != null ? nameInput.getText().toString().trim() : "";
        String dept = deptSpinner.getSelectedItem().toString();
        String cls  = classSpinner.getSelectedItem().toString();

        if (name.isEmpty()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        saveBtn.setEnabled(false);
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("department", dept);
        updates.put("className", cls);

        db.collection("users").document(userId).update(updates)
            .addOnSuccessListener(unused -> {
                saveBtn.setEnabled(true);
                Toast.makeText(this, "Profile saved! ✅", Toast.LENGTH_SHORT).show();
                profileName.setText(name);
                profileAvatarBig.setText(String.valueOf(name.charAt(0)).toUpperCase());
            })
            .addOnFailureListener(e -> {
                saveBtn.setEnabled(true);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void changePassword() {
        String email = auth.getCurrentUser() != null ? auth.getCurrentUser().getEmail() : "";
        if (email == null || email.isEmpty()) return;

        new MaterialAlertDialogBuilder(this)
            .setTitle("Change Password")
            .setMessage("A password reset link will be sent to:\n" + email)
            .setPositiveButton("Send", (d, w) -> {
                auth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(v -> Toast.makeText(this,
                            "Reset link sent to " + email, Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e -> Toast.makeText(this,
                            "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void logout() {
        new MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout", (d, w) -> {
                auth.signOut();
                startActivity(new Intent(this, LoginActivity.class));
                finishAffinity();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
}
