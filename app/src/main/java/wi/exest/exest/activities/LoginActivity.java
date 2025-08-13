package wi.exest.exest.activities;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import wi.exest.exest.MainActivity;
import wi.exest.exest.SessionManager;
import wi.exest.exest.databinding.ActivityLoginBinding;
import wi.exest.exest.models.User;

import wi.exest.exest.utils.ErrorReporter;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        binding.loginButton.setOnClickListener(v -> loginUser());
        binding.registerButton.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
    }

    private boolean validateInput(String email, String password) {
        boolean valid = true;

        // Validate email
        if (email.isEmpty()) {
            binding.emailLayout.setError("Email is required");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError("Valid email required");
            valid = false;
        } else {
            binding.emailLayout.setError(null);
        }

        // Validate password
        if (password.isEmpty()) {
            binding.passwordLayout.setError("Password is required");
            valid = false;
        } else {
            binding.passwordLayout.setError(null);
        }

        return valid;
    }

    private void loginUser() {
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();

        if (validateInput(email, password)) {
            binding.progressBar.setVisibility(View.VISIBLE);

            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        binding.progressBar.setVisibility(View.GONE);

                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                // Load user data from database to get username
                                loadUserData(firebaseUser.getUid(), email);
                            }
                        } else {
                            if (task.getException() != null) {
                                String error = task.getException().getMessage();

                                if (error.contains("invalid login credentials") ||
                                        error.contains("password is invalid") ||
                                        error.contains("wrong password")) {
                                    binding.passwordLayout.setError("Invalid password");
                                } else if (error.contains("no user record") ||
                                        error.contains("user not found")) {
                                    binding.emailLayout.setError("Email not found");
                                } else {
                                    ErrorReporter.showErrorWithReport(LoginActivity.this, "Ошибка входа: " + error, "LoginActivity");
                                }
                            }
                        }
                    });
        }
    }

    private void loadUserData(String userId, String email) {
        DatabaseReference userRef = database.getReference("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        // Save session with username
                        SessionManager session = new SessionManager(LoginActivity.this);
                        session.createLoginSession(userId, email, user.username);

                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        ErrorReporter.showErrorWithReport(LoginActivity.this, "Данные пользователя повреждены", "LoginActivity");
                    }
                } else {
                    ErrorReporter.showErrorWithReport(LoginActivity.this, "Данные пользователя не найдены", "LoginActivity");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                ErrorReporter.showErrorWithReport(LoginActivity.this, "Не удалось загрузить данные пользователя: " + error.getMessage(), "LoginActivity");
            }
        });
    }
}