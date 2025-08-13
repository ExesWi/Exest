package wi.exest.exest.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import wi.exest.exest.SessionManager;
import wi.exest.exest.activities.ChatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import wi.exest.exest.databinding.ItemChatBinding;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.util.Map;
import wi.exest.exest.R;
import wi.exest.exest.adapter.UserSelectAdapter;
import wi.exest.exest.models.User;
import wi.exest.exest.models.GroupChat;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.app.AlertDialog;
import android.widget.EditText;
import android.widget.Toast;
import android.net.Uri;
import android.content.Intent;
import android.provider.MediaStore;
import android.graphics.Bitmap;
import java.io.ByteArrayOutputStream;
import android.app.Activity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import wi.exest.exest.databinding.DialogCreateGroupBinding;
import wi.exest.exest.activities.GroupChatActivity;
import android.os.Handler;
import wi.exest.exest.databinding.FragmentChatBinding;
import wi.exest.exest.utils.ErrorReporter;

public class ChatFragment extends Fragment {
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;
    private List<ChatPreview> chatPreviews = new ArrayList<>();
    private String myId;
    private DialogCreateGroupBinding createGroupDialogBinding;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private String groupAvatarBase64 = null;
    private View emptyStateLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatAdapter = new ChatAdapter(chatPreviews);
        recyclerView.setAdapter(chatAdapter);
        FloatingActionButton fab = view.findViewById(R.id.fabCreateGroup);
        fab.setOnClickListener(v -> showCreateGroupDialog());
        // Регистрация image picker
        pickImageLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                        groupAvatarBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
                        if (createGroupDialogBinding != null) {
                            createGroupDialogBinding.groupAvatar.setImageBitmap(bitmap);
                        }
                    } catch (Exception ignored) {}
                }
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SessionManager session = new SessionManager(requireContext());
        myId = session.getUserId();
        
        // Инициализация views
        recyclerView = view.findViewById(R.id.recyclerView);
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout);
        
        loadChats();
    }

    private void loadChats() {
        chatPreviews.clear();
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference("groupChats");
        
        // Слушаем личные чаты в реальном времени
        chatsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    List<ChatPreview> previews = new ArrayList<>();
                    for (DataSnapshot chatSnap : snapshot.getChildren()) {
                        String chatId = chatSnap.getKey();
                        if (chatId == null || !chatId.contains(myId)) continue;
                        if (chatSnap.child("hiddenFor").child(myId).exists()) continue;
                        DataSnapshot messagesSnap = chatSnap.child("messages");
                        if (!messagesSnap.exists()) continue;
                        DataSnapshot lastMsgSnap = null;
                        long lastTime = 0;
                        for (DataSnapshot msg : messagesSnap.getChildren()) {
                            Long t = msg.child("timestamp").getValue(Long.class);
                            if (t != null && t > lastTime) {
                                lastTime = t;
                                lastMsgSnap = msg;
                            }
                        }
                        if (lastMsgSnap != null) {
                            String fromUsername = lastMsgSnap.child("fromUsername").getValue(String.class);
                            String text = lastMsgSnap.child("text").getValue(String.class);
                            String fromUserId = lastMsgSnap.child("fromUserId").getValue(String.class);
                            String toUserId = lastMsgSnap.child("toUserId").getValue(String.class);
                            String otherId = myId.equals(fromUserId) ? toUserId : fromUserId;
                            previews.add(new ChatPreview(chatId, otherId, fromUsername, text, lastTime, false, null, null));
                        }
                    }
                    // Теперь слушаем групповые чаты отдельно (только один раз)
                    groupRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            try {
                                // Сначала копируем previews из личных чатов
                                List<ChatPreview> allPreviews = new ArrayList<>(previews);
                                for (DataSnapshot groupSnap : snapshot.getChildren()) {
                                    String groupId = groupSnap.getKey();
                                    if (groupId == null) continue;
                                    GenericTypeIndicator<List<String>> t = new GenericTypeIndicator<List<String>>() {};
                                    List<String> members = groupSnap.child("members").getValue(t);
                                    if (members == null || !members.contains(myId)) continue;
                                    String title = groupSnap.child("title").getValue(String.class);
                                    String avatarBase64 = groupSnap.child("avatarBase64").getValue(String.class);
                                    // Ищем последнее сообщение в messages
                                    DataSnapshot messagesSnap = groupSnap.child("messages");
                                    DataSnapshot lastMsgSnap = null;
                                    long lastTime = 0;
                                    String lastMsg = null;
                                    String fromUsername = null;
                                    if (messagesSnap.exists()) {
                                        for (DataSnapshot msg : messagesSnap.getChildren()) {
                                            Long tmsg = msg.child("timestamp").getValue(Long.class);
                                            if (tmsg != null && tmsg > lastTime) {
                                                lastTime = tmsg;
                                                lastMsgSnap = msg;
                                            }
                                        }
                                        if (lastMsgSnap != null) {
                                            lastMsg = lastMsgSnap.child("text").getValue(String.class);
                                            fromUsername = lastMsgSnap.child("fromUsername").getValue(String.class);
                                        }
                                    }
                                    allPreviews.add(new ChatPreview(groupId, null, fromUsername, lastMsg, lastTime, true, avatarBase64, title));
                                }
                                // Сортировка: самые свежие чаты сверху
                                Collections.sort(allPreviews, (a, b) -> Long.compare(b.lastTime, a.lastTime));
                                chatPreviews.clear();
                                chatPreviews.addAll(allPreviews);
                                chatAdapter.notifyDataSetChanged();
                                updateEmptyState();
                            } catch (Exception e) {
                                ErrorReporter.logError(requireContext(), "Ошибка при загрузке групповых чатов", "ChatFragment", e);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                } catch (Exception e) {
                    ErrorReporter.logError(requireContext(), "Ошибка при загрузке личных чатов", "ChatFragment", e);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private static class ChatPreview {
        String chatId;
        String otherId;
        String fromUsername;
        String lastMsg;
        long lastTime;
        boolean isGroup;
        String groupAvatarBase64;
        String groupTitle;
        ChatPreview(String chatId, String otherId, String fromUsername, String lastMsg, long lastTime, boolean isGroup, String groupAvatarBase64, String groupTitle) {
            this.chatId = chatId;
            this.otherId = otherId;
            this.fromUsername = fromUsername;
            this.lastMsg = lastMsg;
            this.lastTime = lastTime;
            this.isGroup = isGroup;
            this.groupAvatarBase64 = groupAvatarBase64;
            this.groupTitle = groupTitle;
        }
    }

    private class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
        private final List<ChatPreview> chats;
        ChatAdapter(List<ChatPreview> chats) { this.chats = chats; }
        @NonNull
        @Override
        public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemChatBinding binding = ItemChatBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new ChatViewHolder(binding);
        }
        @Override
        public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
            holder.bind(chats.get(position));
        }
        @Override
        public int getItemCount() { return chats.size(); }
        class ChatViewHolder extends RecyclerView.ViewHolder {
            private final ItemChatBinding binding;
            public ChatViewHolder(ItemChatBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
            public void bind(ChatPreview preview) {
                if (preview.isGroup) {
                    binding.userName.setText(preview.groupTitle != null ? preview.groupTitle : "Группа");
                    if (preview.lastMsg != null && !preview.lastMsg.isEmpty() && preview.fromUsername != null && !preview.fromUsername.isEmpty()) {
                        binding.lastMessage.setText(preview.fromUsername + ": " + preview.lastMsg);
                    } else if (preview.lastMsg != null && !preview.lastMsg.isEmpty()) {
                        binding.lastMessage.setText(preview.lastMsg);
                    } else {
                        binding.lastMessage.setText("Нет сообщений");
                    }
                    if (preview.groupAvatarBase64 != null && !preview.groupAvatarBase64.isEmpty()) {
                        byte[] decodedBytes = Base64.decode(preview.groupAvatarBase64, Base64.DEFAULT);
                        binding.userAvatar.setImageBitmap(BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length));
                    } else {
                        binding.userAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                    }
                    binding.userName.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_myplaces, 0, 0, 0);
                    itemView.setOnClickListener(v -> {
                        Intent intent = new Intent(requireContext(), GroupChatActivity.class);
                        intent.putExtra("groupId", preview.chatId);
                        startActivity(intent);
                    });
                } else {
                    // Загрузка имени и аватара собеседника
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(preview.otherId);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            Map<String, Object> user = (Map<String, Object>) snapshot.getValue();
                            String name = user != null && user.get("username") != null ? user.get("username").toString() : preview.otherId;
                            binding.userName.setText(name);
                            String avatarBase64 = user != null && user.get("avatarBase64") != null ? user.get("avatarBase64").toString() : null;
                            if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                                byte[] decodedBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
                                binding.userAvatar.setImageBitmap(BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length));
                            } else {
                                binding.userAvatar.setImageResource(R.drawable.ic_default_avatar);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                    if (preview.lastMsg != null && !preview.lastMsg.isEmpty() && preview.fromUsername != null && !preview.fromUsername.isEmpty()) {
                        binding.lastMessage.setText(preview.fromUsername + ": " + preview.lastMsg);
                    } else if (preview.lastMsg != null && !preview.lastMsg.isEmpty()) {
                        binding.lastMessage.setText(preview.lastMsg);
                    } else {
                        binding.lastMessage.setText("Нет сообщений");
                    }
                    binding.userName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                    itemView.setOnClickListener(v -> {
                        Intent intent = new Intent(requireContext(), ChatActivity.class);
                        intent.putExtra("toUserId", preview.otherId);
                        startActivity(intent);
                    });
                }
                itemView.setOnLongClickListener(v -> {
                    showDeleteChatDialog(preview);
                    return true;
                });
            }
        }
    }

    private void showCreateGroupDialog() {
        createGroupDialogBinding = DialogCreateGroupBinding.inflate(getLayoutInflater());
        // Загрузка всех пользователей
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> users = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    User u = ds.getValue(User.class);
                    if (u != null && u.userId != null && !u.userId.equals(myId)) users.add(u);
                }
                UserSelectAdapter adapter = new UserSelectAdapter(users);
                createGroupDialogBinding.usersRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
                createGroupDialogBinding.usersRecyclerView.setAdapter(adapter);
                createGroupDialogBinding.groupAvatar.setOnClickListener(v -> {
                    Intent pickIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    pickImageLauncher.launch(pickIntent);
                });
                AlertDialog dialog = new AlertDialog.Builder(requireContext())
                        .setTitle("Создать группу")
                        .setView(createGroupDialogBinding.getRoot())
                        .setNegativeButton("Отмена", (d, w) -> d.dismiss())
                        .create();
                createGroupDialogBinding.createGroupBtn.setOnClickListener(v -> {
                    String title = createGroupDialogBinding.groupTitle.getText().toString().trim();
                    String desc = createGroupDialogBinding.groupDescription.getText().toString().trim();
                    List<User> selected = adapter.getSelectedUsers();
                    if (title.isEmpty()) {
                        Toast.makeText(requireContext(), "Введите название группы", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    List<String> memberIds = new ArrayList<>();
                    memberIds.add(myId);
                    for (User u : selected) memberIds.add(u.userId);
                    String groupId = FirebaseDatabase.getInstance().getReference("groupChats").push().getKey();
                    GroupChat group = new GroupChat(groupId, title, desc, groupAvatarBase64, myId, memberIds, System.currentTimeMillis());
                    FirebaseDatabase.getInstance().getReference("groupChats").child(groupId).setValue(group)
                        .addOnSuccessListener(unused -> {
                            Toast.makeText(requireContext(), "Группа создана!", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            // TODO: обновить список чатов
                        })
                        .addOnFailureListener(e -> Toast.makeText(requireContext(), "Ошибка создания группы", Toast.LENGTH_SHORT).show());
                });
                dialog.show();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showDeleteChatDialog(ChatPreview preview) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Удалить чат")
            .setMessage("Выберите действие:")
            .setPositiveButton("Удалить только у меня", (d, w) -> deleteChatForMe(preview))
            .setNegativeButton("Удалить у всех", (d, w) -> deleteChatForAll(preview))
            .setNeutralButton("Отмена", null)
            .show();
    }
    private void deleteChatForMe(ChatPreview preview) {
        if (preview.isGroup) {
            // Для группового чата — удаляем себя из участников
            DatabaseReference groupRef = FirebaseDatabase.getInstance().getReference("groupChats").child(preview.chatId).child("members");
            groupRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<String> members = snapshot.getValue(new GenericTypeIndicator<List<String>>() {});
                    if (members != null && members.contains(myId)) {
                        members.remove(myId);
                        groupRef.setValue(members);
                        Toast.makeText(requireContext(), "Групповой чат удалён у вас", Toast.LENGTH_SHORT).show();
                        new Handler().postDelayed(ChatFragment.this::loadChats, 500);
                    }
                }
                @Override public void onCancelled(@NonNull DatabaseError error) {}
            });
        } else {
            // Для личного чата — удаляем chatId из списка только у себя (например, можно хранить список скрытых чатов)
            // Для простоты: удаляем все сообщения только у себя (можно реализовать скрытие)
            DatabaseReference chatRef = FirebaseDatabase.getInstance().getReference("chats").child(preview.chatId);
            chatRef.child("hiddenFor").child(myId).setValue(true);
            Toast.makeText(requireContext(), "Чат скрыт у вас", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(this::loadChats, 500);
        }
    }
    private void deleteChatForAll(ChatPreview preview) {
        if (preview.isGroup) {
            // Удаляем весь групповой чат
            FirebaseDatabase.getInstance().getReference("groupChats").child(preview.chatId).removeValue();
            Toast.makeText(requireContext(), "Групповой чат удалён у всех", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(this::loadChats, 500);
        } else {
            // Удаляем весь личный чат
            FirebaseDatabase.getInstance().getReference("chats").child(preview.chatId).removeValue();
            Toast.makeText(requireContext(), "Чат удалён у всех", Toast.LENGTH_SHORT).show();
            new Handler().postDelayed(this::loadChats, 500);
        }
    }

    private void updateEmptyState() {
        if (chatPreviews.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateLayout.setVisibility(View.GONE);
        }
    }
} 