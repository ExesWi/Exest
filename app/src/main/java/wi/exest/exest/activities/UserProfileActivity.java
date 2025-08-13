package wi.exest.exest.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.List;
import wi.exest.exest.models.User;
import android.util.Base64;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.widget.Button;
import android.content.Intent;
import wi.exest.exest.SessionManager;
import wi.exest.exest.databinding.ActivityUserProfileBinding;
import android.view.View;
import wi.exest.exest.utils.ErrorReporter;

public class UserProfileActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityUserProfileBinding binding = ActivityUserProfileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle("Профиль пользователя");
        String userId = getIntent().getStringExtra("userId");
        if (userId == null || userId.trim().isEmpty()) {
            android.widget.Toast.makeText(this, "Ошибка: не передан userId", android.widget.Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child(userId);
        try {
            ref.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    User user = snapshot.getValue(User.class);
                    if (user == null) return;
                    // Аватар
                    if (user.avatarBase64 != null && !user.avatarBase64.isEmpty()) {
                        try {
                            byte[] decodedBytes = Base64.decode(user.avatarBase64, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            binding.avatarView.setImageBitmap(bitmap);
                        } catch (Exception e) {
                            binding.avatarView.setImageResource(android.R.drawable.sym_def_app_icon);
                        }
                    } else {
                        binding.avatarView.setImageResource(android.R.drawable.sym_def_app_icon);
                    }
                    // Имя/никнейм
                    binding.usernameView.setText(user.username != null ? user.username : "Пользователь");
                    // Навыки
                    binding.skillsGroup.removeAllViews();
                    if (user.skills != null && !user.skills.isEmpty()) {
                        addChips(binding.skillsGroup, user.skills);
                    } else {
                        addNoneChip(binding.skillsGroup);
                    }
                    // Интересы
                    binding.interestsGroup.removeAllViews();
                    if (user.interests != null && !user.interests.isEmpty()) {
                        addChips(binding.interestsGroup, user.interests);
                    } else {
                        addNoneChip(binding.interestsGroup);
                    }
                    // О себе
                    binding.aboutView.setText(user.about != null && !user.about.trim().isEmpty() ? user.about : "отсутствует");
                    // Портфолио
                    binding.portfolioView.setText(user.portfolioUrl != null && !user.portfolioUrl.trim().isEmpty() ? user.portfolioUrl : "отсутствует");
                    // Кнопка для чата
                    SessionManager session = new SessionManager(UserProfileActivity.this);
                    String myId = session.getUserId();
                    if (myId != null && !myId.equals(userId)) {
                        binding.chatBtn.setVisibility(View.VISIBLE);
                        binding.chatBtn.setOnClickListener(v -> {
                            Intent intent = new Intent(UserProfileActivity.this, wi.exest.exest.activities.ChatActivity.class);
                            intent.putExtra("toUserId", userId);
                            startActivity(intent);
                        });
                    } else {
                        binding.chatBtn.setVisibility(View.GONE);
                    }
                }
                @Override
                public void onCancelled(DatabaseError error) {}
            });
        } catch (Exception e) {
            ErrorReporter.showErrorWithReport(this, "Ошибка при загрузке профиля пользователя", "UserProfileActivity", e);
        }
    }
    private void addChips(ChipGroup group, List<String> tags) {
        if (tags == null) return;
        for (String tag : tags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(false);
            group.addView(chip);
        }
    }
    private void addNoneChip(ChipGroup group) {
        Chip chip = new Chip(this);
        chip.setText("отсутствует");
        chip.setCheckable(false);
        chip.setEnabled(false);
        group.addView(chip);
    }
} 