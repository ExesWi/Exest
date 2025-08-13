package wi.exest.exest.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import wi.exest.exest.MainActivity;
import wi.exest.exest.R;
import wi.exest.exest.SessionManager;
import wi.exest.exest.databinding.ActivityRegisterBinding;
import wi.exest.exest.models.User;

import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        binding.registerButton.setOnClickListener(v -> registerUser());

        // Переход на логин
        binding.loginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private boolean validateInput(String username, String email, String password, String confirmPassword) {
        boolean valid = true;

        // Валидация username
        if (username.isEmpty()) {
            binding.usernameLayout.setError("Username is required");
            valid = false;
        } else if (username.length() < 3) {
            binding.usernameLayout.setError("Min 3 characters");
            valid = false;
        } else {
            binding.usernameLayout.setError(null);
        }

        // Валидация email
        if (email.isEmpty()) {
            binding.emailLayout.setError("Email is required");
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError("Valid email required");
            valid = false;
        } else {
            binding.emailLayout.setError(null);
        }

        // Валидация пароля
        if (password.isEmpty()) {
            binding.passwordLayout.setError("Password is required");
            valid = false;
        } else if (password.length() < 6) {
            binding.passwordLayout.setError("Min 6 characters");
            valid = false;
        } else {
            binding.passwordLayout.setError(null);
        }

        // Подтверждение пароля
        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordLayout.setError("Confirm password");
            valid = false;
        } else if (!password.equals(confirmPassword)) {
            binding.confirmPasswordLayout.setError("Passwords don't match");
            valid = false;
        } else {
            binding.confirmPasswordLayout.setError(null);
        }

        return valid;
    }

    private void registerUser() {
        // Получаем значения из правильных полей!
        String username = binding.usernameEditText.getText().toString().trim();
        String email = binding.emailEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString().trim();
        String confirmPassword = binding.confirmPasswordEditText.getText().toString().trim();

        // Логирование перед регистрацией
        Log.d("RegisterDebug", "Username: " + username);
        Log.d("RegisterDebug", "Email: " + email);

        if (validateInput(username, email, password, confirmPassword)) {
            binding.progressBar.setVisibility(View.VISIBLE);

            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                saveUserToDatabase(firebaseUser.getUid(), username, email);
                            }
                        } else {
                            binding.progressBar.setVisibility(View.GONE);
                            if (task.getException() != null) {
                                Toast.makeText(RegisterActivity.this,
                                        "Registration failed: " + task.getException().getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    private void saveUserToDatabase(String userId, String username, String email) {
        DatabaseReference usersRef = database.getReference("users");

        // Создаем пользователя с ПРАВИЛЬНЫМИ данными
        User user = new User(username, email);
        user.userId = userId;

        usersRef.child(userId).setValue(user)
                .addOnCompleteListener(task -> {
                    binding.progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        // Логирование успешного сохранения
                        Log.d("RegisterDebug", "User saved to DB: " + user.username + ", " + user.email);

                        SessionManager session = new SessionManager(RegisterActivity.this);
                        session.createLoginSession(userId, email, username);

                        Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                        finish();
                    } else {
                        if (mAuth.getCurrentUser() != null) {
                            mAuth.getCurrentUser().delete();
                        }
                        Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                        Log.e("RegisterError", "DB save failed", task.getException());
                    }
                });
    }
}