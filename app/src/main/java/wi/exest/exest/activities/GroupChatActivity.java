package wi.exest.exest.activities;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.View;
import wi.exest.exest.databinding.ActivityGroupChatBinding;
import wi.exest.exest.models.GroupChat;
import androidx.annotation.NonNull;
import android.view.ViewGroup;
import wi.exest.exest.SessionManager;
import wi.exest.exest.adapter.UserSelectAdapter;
import wi.exest.exest.models.User;
import android.app.AlertDialog;
import wi.exest.exest.databinding.DialogGroupMembersBinding;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.GenericTypeIndicator;
import android.widget.FrameLayout;
import android.view.Gravity;
import de.hdodenhof.circleimageview.CircleImageView;
import android.widget.LinearLayout;
import wi.exest.exest.R;
import android.content.Intent;
import wi.exest.exest.activities.GroupInfoActivity;
import java.util.HashMap;
import androidx.recyclerview.widget.ItemTouchHelper;

public class GroupChatActivity extends AppCompatActivity {
    private ActivityGroupChatBinding binding;
    private String groupId;
    private GroupChat groupChat;
    private List<Message> messages = new ArrayList<>();
    private MessagesAdapter messagesAdapter;
    private DatabaseReference messagesRef;
    private String myId;
    private String myName;
    private HashMap<String, String> userPrefixes = new HashMap<>();
    private Message replyToMessage = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        SessionManager session = new SessionManager(this);
        myId = session.getUserId();
        myName = session.getUsername();
        groupId = getIntent().getStringExtra("groupId");
        if (groupId == null) {
            Toast.makeText(this, "Ошибка: groupId не передан", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        binding.sendButton.setEnabled(false); // Блокируем отправку до загрузки сообщений
        // Переход на экран информации о группе по клику на CardView
        binding.groupHeaderCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, GroupInfoActivity.class);
            intent.putExtra("groupId", groupId);
            startActivity(intent);
        });
        loadGroupInfo();
        binding.messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesAdapter = new MessagesAdapter(messages);
        binding.messagesRecyclerView.setAdapter(messagesAdapter);
        binding.sendButton.setOnClickListener(v -> {
            String text = binding.inputMessage.getText().toString().trim();
            if (!text.isEmpty()) sendMessage(text);
        });
        // Добавляем ItemTouchHelper для свайпа
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int pos = viewHolder.getAdapterPosition();
                if (pos >= 0 && pos < messages.size()) {
                    replyToMessage = messages.get(pos);
                    showReplyPanel();
                }
                messagesAdapter.notifyItemChanged(pos); // сбрасываем свайп
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(binding.messagesRecyclerView);
    }

    private void loadGroupInfo() {
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference("groupChats").child(groupId);
        groupRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                groupChat = snapshot.getValue(GroupChat.class);
                if (groupChat == null) {
                    Toast.makeText(GroupChatActivity.this, "Группа не найдена", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }
                binding.groupTitle.setText(groupChat.title);
                int count = groupChat.members != null ? groupChat.members.size() : 0;
                binding.groupMembersCount.setText("Участников: " + count);
                if (groupChat.avatarBase64 != null && !groupChat.avatarBase64.isEmpty()) {
                    byte[] decodedBytes = Base64.decode(groupChat.avatarBase64, Base64.DEFAULT);
                    binding.groupAvatar.setImageBitmap(BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length));
                } else {
                    binding.groupAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                }
                // Загружаем префиксы всех участников
                loadAllPrefixes();
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void loadAllPrefixes() {
        userPrefixes.clear();
        DatabaseReference prefixRef = FirebaseDatabase.getInstance().getReference("groupPrefixes").child(groupId);
        prefixRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                userPrefixes.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    String userId = ds.getKey();
                    String prefix = ds.getValue(String.class);
                    if (userId != null && prefix != null && !prefix.isEmpty()) {
                        userPrefixes.put(userId, prefix);
                    }
                }
                loadMessages(); // всегда вызываем
            }
            @Override
            public void onCancelled(DatabaseError error) {
                loadMessages(); // обязательно вызываем даже при ошибке
            }
        });
    }

    private void loadMessages() {
        messages.clear();
        messagesRef = FirebaseDatabase.getInstance().getReference("groupChats").child(groupId).child("messages");
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                messages.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Message m = ds.getValue(Message.class);
                    if (m != null) messages.add(m);
                }
                Collections.sort(messages, (a, b) -> Long.compare(a.timestamp, b.timestamp));
                messagesAdapter.notifyDataSetChanged();
                binding.messagesRecyclerView.scrollToPosition(messages.size() - 1);
                binding.sendButton.setEnabled(true); // Разблокируем отправку после загрузки
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void sendMessage(String text) {
        if (messagesRef == null) {
            Toast.makeText(this, "Чат ещё не загружен. Подождите...", Toast.LENGTH_SHORT).show();
            return;
        }
        Message msg = new Message();
        msg.messageId = java.util.UUID.randomUUID().toString();
        msg.fromUserId = myId;
        msg.fromUsername = myName;
        msg.text = text;
        msg.timestamp = System.currentTimeMillis();
        if (replyToMessage != null) {
            msg.replyMessageId = replyToMessage.messageId;
            msg.replyText = replyToMessage.text;
            msg.replyUsername = replyToMessage.fromUsername;
        }
        messagesRef.child(msg.messageId).setValue(msg);
        binding.inputMessage.setText("");
        replyToMessage = null;
        binding.replyPanel.setVisibility(View.GONE);
    }

    public static class Message {
        public String messageId;
        public String fromUserId;
        public String fromUsername;
        public String text;
        public long timestamp;
        // Для ответа
        public String replyMessageId;
        public String replyText;
        public String replyUsername;
        public Message() {}
    }

    private class MessagesAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {
        private final List<Message> messages;
        MessagesAdapter(List<Message> messages) { this.messages = messages; }
        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.view.LayoutInflater inflater = android.view.LayoutInflater.from(parent.getContext());
            wi.exest.exest.databinding.ItemMessageBinding binding = wi.exest.exest.databinding.ItemMessageBinding.inflate(inflater, parent, false);
            return new MessageViewHolder(binding);
        }
        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            holder.bind(messages.get(position));
        }
        @Override
        public int getItemCount() { return messages.size(); }
        class MessageViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            private final wi.exest.exest.databinding.ItemMessageBinding binding;
            public MessageViewHolder(wi.exest.exest.databinding.ItemMessageBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
            public void bind(Message m) {
                String time = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(m.timestamp));
                String prefix = userPrefixes.get(m.fromUserId);
                String meta;
                if (prefix != null && !prefix.isEmpty()) {
                    meta = m.fromUsername + " [" + prefix + "] " + time;
                } else {
                    meta = m.fromUsername + " " + time;
                }
                binding.messageText.setText(m.text);
                binding.messageMeta.setText(meta);
                FrameLayout.LayoutParams cardParams = (FrameLayout.LayoutParams) binding.messageCard.getLayoutParams();
                if (m.fromUserId != null && m.fromUserId.equals(myId)) {
                    cardParams.gravity = Gravity.END;
                    binding.messageCard.setCardBackgroundColor(0xFFE0E0E0);
                    binding.messageText.setTextColor(0xFF232323);
                    binding.messageAvatar.setVisibility(View.GONE);
                } else {
                    cardParams.gravity = Gravity.START;
                    binding.messageCard.setCardBackgroundColor(0xFFFFFFFF);
                    binding.messageText.setTextColor(0xFF232323);
                    binding.messageAvatar.setVisibility(View.VISIBLE);
                    if (m.fromUserId != null) {
                        FirebaseDatabase.getInstance().getReference("users").child(m.fromUserId)
                            .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                                @Override
                                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                                    String avatarBase64 = snapshot.child("avatarBase64").getValue(String.class);
                                    if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                                        byte[] decodedBytes = android.util.Base64.decode(avatarBase64, android.util.Base64.DEFAULT);
                                        binding.messageAvatar.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length));
                                    } else {
                                        binding.messageAvatar.setImageResource(R.drawable.ic_default_avatar);
                                    }
                                }
                                @Override public void onCancelled(com.google.firebase.database.DatabaseError error) {}
                            });
                    }
                }
                binding.messageCard.setLayoutParams(cardParams);
                // Отображение блока ответа
                if (m.replyMessageId != null && m.replyText != null) {
                    binding.replyBlock.setVisibility(View.VISIBLE);
                    binding.replyBlockText.setText(m.replyText);
                    binding.replyBlockUsername.setText(m.replyUsername != null ? m.replyUsername : "Ответ");
                    binding.replyBlock.setOnClickListener(v -> {
                        // Находим позицию исходного сообщения по messageId
                        int targetPos = -1;
                        for (int i = 0; i < messages.size(); i++) {
                            if (messages.get(i).messageId != null && messages.get(i).messageId.equals(m.replyMessageId)) {
                                targetPos = i;
                                break;
                            }
                        }
                        if (targetPos != -1) {
                            final int scrollToPos = targetPos;
                            binding.getRoot().post(() -> {
                                GroupChatActivity.this.binding.messagesRecyclerView.smoothScrollToPosition(scrollToPos);
                            });
                        }
                    });
                } else {
                    binding.replyBlock.setVisibility(View.GONE);
                    binding.replyBlock.setOnClickListener(null);
                }
            }
        }
    }

    private void showMembersDialog() {
        DialogGroupMembersBinding dialogBinding = DialogGroupMembersBinding.inflate(getLayoutInflater());
        // Загружаем участников
        GenericTypeIndicator<List<String>> t = new GenericTypeIndicator<List<String>>() {};
        List<String> memberIds = groupChat.members;
        List<User> members = new ArrayList<>();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User u = ds.getValue(User.class);
                    if (u != null && memberIds.contains(u.userId)) members.add(u);
                }
                UserSelectAdapter adapter = new UserSelectAdapter(members);
                dialogBinding.membersRecyclerView.setLayoutManager(new LinearLayoutManager(GroupChatActivity.this));
                dialogBinding.membersRecyclerView.setAdapter(adapter);
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
        // Кнопка добавления участника только для владельца
        if (myId != null && myId.equals(groupChat.ownerId)) {
            dialogBinding.addMemberBtn.setVisibility(View.VISIBLE);
            dialogBinding.addMemberBtn.setOnClickListener(v -> showAddMembersDialog());
        } else {
            dialogBinding.addMemberBtn.setVisibility(View.GONE);
        }
        new AlertDialog.Builder(this)
                .setTitle("Участники группы")
                .setView(dialogBinding.getRoot())
                .setNegativeButton("Закрыть", (d, w) -> d.dismiss())
                .show();
    }

    private void showAddMembersDialog() {
        // Загружаем всех пользователей, кроме уже участников
        GenericTypeIndicator<List<String>> t = new GenericTypeIndicator<List<String>>() {};
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<User> users = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User u = ds.getValue(User.class);
                    if (u != null && !groupChat.members.contains(u.userId)) users.add(u);
                }
                UserSelectAdapter adapter = new UserSelectAdapter(users);
                RecyclerView rv = new RecyclerView(GroupChatActivity.this);
                rv.setLayoutManager(new LinearLayoutManager(GroupChatActivity.this));
                rv.setAdapter(adapter);
                new AlertDialog.Builder(GroupChatActivity.this)
                        .setTitle("Добавить участников")
                        .setView(rv)
                        .setPositiveButton("Добавить", (d, w) -> {
                            List<User> selected = adapter.getSelectedUsers();
                            if (!selected.isEmpty()) {
                                List<String> newMembers = new ArrayList<>(groupChat.members);
                                for (User u : selected) newMembers.add(u.userId);
                                FirebaseDatabase.getInstance().getReference("groupChats").child(groupId).child("members").setValue(newMembers)
                                    .addOnSuccessListener(unused -> Toast.makeText(GroupChatActivity.this, "Участники добавлены", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(GroupChatActivity.this, "Ошибка", Toast.LENGTH_SHORT).show());
                            }
                        })
                        .setNegativeButton("Отмена", (d, w) -> d.dismiss())
                        .show();
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void showReplyPanel() {
        if (replyToMessage != null) {
            binding.replyPanel.setVisibility(View.VISIBLE);
            binding.replyText.setText(replyToMessage.text);
            binding.replyUsername.setText(replyToMessage.fromUsername);
            binding.replyCancel.setOnClickListener(v -> {
                replyToMessage = null;
                binding.replyPanel.setVisibility(View.GONE);
            });
        }
    }
} 