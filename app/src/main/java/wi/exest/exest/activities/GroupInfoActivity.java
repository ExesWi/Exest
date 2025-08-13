package wi.exest.exest.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import android.util.Base64;
import wi.exest.exest.databinding.ActivityGroupInfoBinding;
import wi.exest.exest.models.GroupChat;
import wi.exest.exest.models.User;
import wi.exest.exest.utils.ErrorReporter;
import wi.exest.exest.SessionManager;
import android.app.AlertDialog;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import wi.exest.exest.adapter.UserSelectAdapter;
import wi.exest.exest.R;

public class GroupInfoActivity extends AppCompatActivity {
    private ActivityGroupInfoBinding binding;
    private String groupId;
    private String myId;
    private String ownerId;
    private List<User> members = new ArrayList<>();
    private MembersAdapter membersAdapter;
    private static final int PICK_IMAGE = 101;
    private String groupAvatarBase64;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupInfoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        groupId = getIntent().getStringExtra("groupId");
        SessionManager session = new SessionManager(this);
        myId = session.getUserId();
        membersAdapter = new MembersAdapter();
        binding.groupInfoMembersRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.groupInfoMembersRecycler.setAdapter(membersAdapter);
        binding.changeAvatarBtn.setOnClickListener(v -> pickImage());
        binding.leaveGroupBtn.setOnClickListener(v -> leaveGroup());
        binding.addMembersBtn.setOnClickListener(v -> showAddMembersDialog());
        loadGroupInfo();
    }

    private void loadGroupInfo() {
        try {
            DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference("groupChats").child(groupId);
            groupRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    String title = snapshot.child("title").getValue(String.class);
                    ownerId = snapshot.child("ownerId").getValue(String.class);
                    groupAvatarBase64 = snapshot.child("avatarBase64").getValue(String.class);
                    List<String> memberIds = snapshot.child("members").getValue(new com.google.firebase.database.GenericTypeIndicator<List<String>>() {});
                    binding.groupInfoTitle.setText(title != null ? title : "Группа");
                    int count = memberIds != null ? memberIds.size() : 0;
                    binding.groupInfoMembersCount.setText("Участников: " + count);
                    if (groupAvatarBase64 != null && !groupAvatarBase64.isEmpty()) {
                        byte[] decodedBytes = Base64.decode(groupAvatarBase64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        binding.groupInfoAvatar.setImageBitmap(bitmap);
                    } else {
                        binding.groupInfoAvatar.setImageResource(wi.exest.exest.R.drawable.ic_default_avatar);
                    }
                    // Только владелец может менять аватарку и добавлять участников
                    boolean isOwner = myId != null && myId.equals(ownerId);
                    binding.changeAvatarBtn.setVisibility(isOwner ? View.VISIBLE : View.GONE);
                    binding.addMembersBtn.setVisibility(isOwner ? View.VISIBLE : View.GONE);
                    // Загружаем участников
                    loadMembers(memberIds);
                }
                @Override public void onCancelled(DatabaseError error) {}
            });
        } catch (Exception e) {
            ErrorReporter.showErrorWithReport(this, "Ошибка при управлении участниками группы", "GroupInfoActivity", e);
        }
    }

    private void loadMembers(List<String> memberIds) {
        members.clear();
        if (memberIds == null) {
            membersAdapter.notifyDataSetChanged();
            return;
        }
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User u = ds.getValue(User.class);
                    if (u != null && memberIds.contains(u.userId)) members.add(u);
                }
                membersAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                binding.groupInfoAvatar.setImageBitmap(bitmap);
                // Сохраняем в Firebase
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                String base64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                FirebaseDatabase.getInstance().getReference("groupChats").child(groupId).child("avatarBase64").setValue(base64);
            } catch (Exception e) {
                Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void leaveGroup() {
        new AlertDialog.Builder(this)
            .setTitle("Выйти из группы?")
            .setMessage("Вы уверены, что хотите покинуть группу?")
            .setPositiveButton("Выйти", (d, w) -> {
                DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference("groupChats").child(groupId).child("members");
                groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        List<String> members = snapshot.getValue(new com.google.firebase.database.GenericTypeIndicator<List<String>>() {});
                        if (members != null && members.contains(myId)) {
                            members.remove(myId);
                            groupRef.setValue(members);
                            Toast.makeText(GroupInfoActivity.this, "Вы вышли из группы", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showAddMembersDialog() {
        // Загружаем всех пользователей, кроме уже участников
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<User> allUsers = new ArrayList<>();
                List<String> currentIds = new ArrayList<>();
                for (User u : members) currentIds.add(u.userId);
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User u = ds.getValue(User.class);
                    if (u != null && !currentIds.contains(u.userId)) allUsers.add(u);
                }
                UserSelectAdapter adapter = new UserSelectAdapter(allUsers);
                RecyclerView rv = new RecyclerView(GroupInfoActivity.this);
                rv.setLayoutManager(new LinearLayoutManager(GroupInfoActivity.this));
                rv.setAdapter(adapter);
                new AlertDialog.Builder(GroupInfoActivity.this)
                    .setTitle("Добавить участников")
                    .setView(rv)
                    .setPositiveButton("Добавить", (d, w) -> {
                        List<User> selected = adapter.getSelectedUsers();
                        if (!selected.isEmpty()) {
                            List<String> newMembers = new ArrayList<>();
                            for (User u : members) newMembers.add(u.userId);
                            for (User u : selected) newMembers.add(u.userId);
                            FirebaseDatabase.getInstance().getReference("groupChats").child(groupId).child("members").setValue(newMembers)
                                .addOnSuccessListener(unused -> Toast.makeText(GroupInfoActivity.this, "Участники добавлены", Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e -> Toast.makeText(GroupInfoActivity.this, "Ошибка", Toast.LENGTH_SHORT).show());
                        }
                    })
                    .setNegativeButton("Отмена", (d, w) -> d.dismiss())
                    .show();
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {
        @NonNull
        @Override
        public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_member, parent, false);
            return new MemberViewHolder(view);
        }
        @Override
        public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
            holder.bind(members.get(position));
        }
        @Override
        public int getItemCount() { return members.size(); }
        class MemberViewHolder extends RecyclerView.ViewHolder {
            private final ImageView avatarView;
            private final TextView nameView;
            private final TextView prefixView;
            private User user;
            public MemberViewHolder(View itemView) {
                super(itemView);
                avatarView = itemView.findViewById(R.id.memberAvatar);
                nameView = itemView.findViewById(R.id.memberName);
                prefixView = itemView.findViewById(R.id.memberPrefix);
                itemView.setOnClickListener(v -> {
                    if (user != null) {
                        Intent intent = new Intent(itemView.getContext(), wi.exest.exest.activities.UserProfileActivity.class);
                        intent.putExtra("userId", user.userId);
                        itemView.getContext().startActivity(intent);
                    }
                });
                itemView.setOnLongClickListener(v -> {
                    if (user != null) showMemberActionsDialog(user);
                    return true;
                });
            }
            public void bind(User user) {
                this.user = user;
                nameView.setText(user.username != null ? user.username : user.userId);
                // Префикс/роль (запрашиваем отдельно)
                DatabaseReference prefixRef = FirebaseDatabase.getInstance().getReference("groupPrefixes").child(groupId).child(user.userId);
                prefixRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String prefix = snapshot.getValue(String.class);
                        if (prefix != null && !prefix.isEmpty()) {
                            prefixView.setText(prefix);
                            prefixView.setVisibility(View.VISIBLE);
                        } else {
                            prefixView.setVisibility(View.GONE);
                        }
                    }
                    @Override public void onCancelled(DatabaseError error) {}
                });
                // Аватарка
                if (user.avatarBase64 != null && !user.avatarBase64.isEmpty()) {
                    try {
                        byte[] decodedBytes = Base64.decode(user.avatarBase64, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        avatarView.setImageBitmap(bitmap);
                    } catch (Exception e) {
                        avatarView.setImageResource(wi.exest.exest.R.drawable.ic_default_avatar);
                    }
                } else {
                    avatarView.setImageResource(wi.exest.exest.R.drawable.ic_default_avatar);
                }
            }
        }
    }

    private void showMemberActionsDialog(User user) {
        String[] actions = {"Удалить из группы", "Повысить (назначить права)", "Назначить префикс"};
        new AlertDialog.Builder(this)
            .setTitle(user.username != null ? user.username : user.userId)
            .setItems(actions, (dialog, which) -> {
                switch (which) {
                    case 0: // Удалить
                        removeMember(user);
                        break;
                    case 1: // Повысить
                        showPromoteDialog(user);
                        break;
                    case 2: // Префикс
                        showPrefixDialog(user);
                        break;
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void removeMember(User user) {
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference("groupChats").child(groupId).child("members");
        groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<String> members = snapshot.getValue(new com.google.firebase.database.GenericTypeIndicator<List<String>>() {});
                if (members != null && members.contains(user.userId)) {
                    members.remove(user.userId);
                    groupRef.setValue(members);
                    Toast.makeText(GroupInfoActivity.this, "Пользователь удалён", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void showPromoteDialog(User user) {
        String[] rights = {"Добавлять участников", "Удалять участников"};
        boolean[] checked = new boolean[rights.length];
        // Здесь можно загрузить текущие права пользователя, если они хранятся
        new AlertDialog.Builder(this)
            .setTitle("Назначить права")
            .setMultiChoiceItems(rights, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                // Сохраняем права пользователя в группе (например, в groupRoles/{groupId}/{userId})
                List<String> granted = new ArrayList<>();
                for (int i = 0; i < rights.length; i++) if (checked[i]) granted.add(rights[i]);
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("groupRoles").child(groupId).child(user.userId);
                ref.setValue(granted)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Права обновлены", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showPrefixDialog(User user) {
        final android.widget.EditText input = new android.widget.EditText(this);
        // Загружаем текущий префикс
        DatabaseReference prefixRef = FirebaseDatabase.getInstance().getReference("groupPrefixes").child(groupId).child(user.userId);
        prefixRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String prefix = snapshot.getValue(String.class);
                input.setText(prefix != null ? prefix : "");
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
        new AlertDialog.Builder(this)
            .setTitle("Префикс пользователя")
            .setView(input)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                String prefix = input.getText().toString().trim();
                prefixRef.setValue(prefix)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Префикс обновлён", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
} 