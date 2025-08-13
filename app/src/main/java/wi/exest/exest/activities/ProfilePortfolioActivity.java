package wi.exest.exest.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import wi.exest.exest.databinding.ActivityProfilePortfolioBinding;
import wi.exest.exest.SessionManager;
import android.util.Patterns;

public class ProfilePortfolioActivity extends AppCompatActivity {
    private ActivityProfilePortfolioBinding binding;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfilePortfolioBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadPortfolio();
        binding.savePortfolioButton.setOnClickListener(v -> savePortfolio());
    }
    private void loadPortfolio() {
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId();
        if (userId == null) return;
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String url = snapshot.child("portfolioUrl").getValue(String.class);
                binding.portfolioEditText.setText(url != null ? url : "");
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }
    private void savePortfolio() {
        String url = binding.portfolioEditText.getText().toString().trim();
        if (!TextUtils.isEmpty(url) && !Patterns.WEB_URL.matcher(url).matches()) {
            binding.portfolioEditText.setError("Введите корректную ссылку (http/https)");
            Toast.makeText(this, "Некорректная ссылка!", Toast.LENGTH_SHORT).show();
            return;
        }
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId();
        if (userId == null) return;
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId).child("portfolioUrl");
        userRef.setValue(url)
            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Сохранено!", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
} 