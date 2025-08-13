package wi.exest.exest.activities;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import wi.exest.exest.databinding.ActivityProfileInfoBinding;
import wi.exest.exest.models.User;
import wi.exest.exest.SessionManager;
import com.google.android.material.chip.Chip;
import android.widget.TextView;
import java.util.List;
import android.app.AlertDialog;
import android.widget.EditText;
import android.text.TextUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ProfileInfoActivity extends AppCompatActivity {
    private ActivityProfileInfoBinding binding;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProfileInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadUserInfo();
        binding.editSkillsButton.setOnClickListener(v -> showSkillsDialog());
        binding.editInterestsButton.setOnClickListener(v -> showInterestsDialog());
        binding.editAboutButton.setOnClickListener(v -> showEditAboutDialog());
    }
    private void loadUserInfo() {
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId();
        if (userId == null) return;
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user == null) return;
                setChips(binding.skillsChipGroup, user.skills);
                setChips(binding.interestsChipGroup, user.interests);
                binding.aboutText.setText(user.about != null ? user.about : "");
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }
    private void setChips(com.google.android.material.chip.ChipGroup group, List<String> tags) {
        group.removeAllViews();
        if (tags == null) return;
        for (String tag : tags) {
            Chip chip = new Chip(this);
            chip.setText(tag);
            chip.setCheckable(false);
            group.addView(chip);
        }
    }
    private void showSkillsDialog() {
        String[] allSkills = {"Программирование", "Дизайн", "Маркетинг", "Менеджмент", "Аналитика", "Тестирование", "Копирайтинг"};
        Set<String> selected = new HashSet<>();
        for (int i = 0; i < binding.skillsChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) binding.skillsChipGroup.getChildAt(i);
            selected.add(chip.getText().toString());
        }
        boolean[] checked = new boolean[allSkills.length];
        for (int i = 0; i < allSkills.length; i++) checked[i] = selected.contains(allSkills[i]);
        new AlertDialog.Builder(this)
            .setTitle("Выберите навыки")
            .setMultiChoiceItems(allSkills, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
            .setPositiveButton("OK", (dialog, which) -> {
                Set<String> result = new HashSet<>();
                for (int i = 0; i < allSkills.length; i++) if (checked[i]) result.add(allSkills[i]);
                saveSkills(result);
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    private void showInterestsDialog() {
        String[] allInterests = {"Стартапы", "Экология", "Мобильная разработка", "AI", "Финансы", "Образование", "ЗОЖ"};
        Set<String> selected = new HashSet<>();
        for (int i = 0; i < binding.interestsChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) binding.interestsChipGroup.getChildAt(i);
            selected.add(chip.getText().toString());
        }
        boolean[] checked = new boolean[allInterests.length];
        for (int i = 0; i < allInterests.length; i++) checked[i] = selected.contains(allInterests[i]);
        new AlertDialog.Builder(this)
            .setTitle("Выберите интересы")
            .setMultiChoiceItems(allInterests, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
            .setPositiveButton("OK", (dialog, which) -> {
                Set<String> result = new HashSet<>();
                for (int i = 0; i < allInterests.length; i++) if (checked[i]) result.add(allInterests[i]);
                saveInterests(result);
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    private void showEditAboutDialog() {
        final EditText input = new EditText(this);
        input.setText(binding.aboutText.getText());
        new AlertDialog.Builder(this)
            .setTitle("О себе")
            .setView(input)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                String about = input.getText().toString().trim();
                saveAbout(about);
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
    private void saveSkills(Set<String> skills) {
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId();
        if (userId == null) return;
        FirebaseDatabase.getInstance().getReference("users").child(userId).child("skills").setValue(new ArrayList<>(skills));
        loadUserInfo();
    }
    private void saveInterests(Set<String> interests) {
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId();
        if (userId == null) return;
        FirebaseDatabase.getInstance().getReference("users").child(userId).child("interests").setValue(new ArrayList<>(interests));
        loadUserInfo();
    }
    private void saveAbout(String about) {
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId();
        if (userId == null) return;
        FirebaseDatabase.getInstance().getReference("users").child(userId).child("about").setValue(about);
        loadUserInfo();
    }
} 