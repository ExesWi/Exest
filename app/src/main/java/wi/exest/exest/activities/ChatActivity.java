package wi.exest.exest.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import wi.exest.exest.SessionManager;
import android.view.Gravity;
import androidx.cardview.widget.CardView;
import android.widget.ImageView;
import android.os.Handler;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.ServerValue;
import android.text.Editable;
import android.text.TextWatcher;
import android.graphics.BitmapFactory;
import android.util.Base64;
import androidx.annotation.NonNull;
import android.R;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import wi.exest.exest.databinding.ActivityChatBinding;
import de.hdodenhof.circleimageview.CircleImageView;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import java.util.Map;
import wi.exest.exest.databinding.ItemMessageBinding;
import android.widget.FrameLayout;
import androidx.recyclerview.widget.ItemTouchHelper;

public class ChatActivity extends AppCompatActivity {
    private EditText inputMessage;
    private Button sendButton;
    private String myId;
    private String myName;
    private String toUserId;
    private String chatId;
    private DatabaseReference messagesRef;
    private Handler typingHandler = new Handler();
    private boolean isTyping = false;
    private ActivityChatBinding binding;
    private RecyclerView messagesRecyclerView;
    private MessagesAdapter messagesAdapter;
    private List<Message> messages = new ArrayList<>();
    private Message replyToMessage = null;

    public static class Message {
        public String messageId;
        public String fromUserId;
        public String fromUsername;
        public String toUserId;
        public String text;
        public long timestamp;
        // Для ответа
        public String replyMessageId;
        public String replyText;
        public String replyUsername;
        public Message() {}
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Сначала получаем и проверяем myId и toUserId
        SessionManager session = new SessionManager(this);
        myId = session.getUserId();
        myName = session.getUsername();
        toUserId = getIntent().getStringExtra("toUserId");
        if (myId == null || toUserId == null) {
            finish();
            return;
        }
        // Теперь можно инициализировать binding и обращаться к Firebase
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        messagesRecyclerView = binding.messagesRecyclerView;
        messagesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        messagesAdapter = new MessagesAdapter(messages);
        messagesRecyclerView.setAdapter(messagesAdapter);
        // Удаляем Toolbar
        //binding.chatToolbar.setVisibility(View.GONE);
        // Восстанавливаем биндинг для аватарки, username и статуса
        CircleImageView avatarView = binding.userAvatar;
        TextView usernameView = binding.usernameView;
        TextView statusView = binding.statusView;
        // Загружаем username и аватарку собеседника
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(toUserId);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String name = snapshot.child("username").getValue(String.class);
                String avatarBase64 = snapshot.child("avatarBase64").getValue(String.class);
                usernameView.setText(name != null ? name : "Пользователь");
                if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                    byte[] decodedBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
                    avatarView.setImageBitmap(BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length));
                } else {
                    avatarView.setImageResource(android.R.drawable.sym_def_app_icon);
                }
                // Индикатор онлайн/оффлайн
                Long lastSeen = snapshot.child("lastSeen").getValue(Long.class);
                if (lastSeen != null && System.currentTimeMillis() - lastSeen < 2 * 60 * 1000) {
                    statusView.setText("в сети");
                    statusView.setTextColor(0xFF4CAF50);
                } else {
                    statusView.setText("не в сети");
                    statusView.setTextColor(0xFFB0B0B0);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
        inputMessage = binding.inputMessage;
        sendButton = binding.sendButton;
        chatId = makeChatId(myId, toUserId);
        messagesRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("messages");
        sendButton.setOnClickListener(v -> {
            String text = inputMessage.getText().toString().trim();
            if (!text.isEmpty()) sendMessage(text);
        });
        loadMessages();
        // Индикатор "печатает"
        //inputMessage.addTextChangedListener(new TextWatcher() {
        //    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        //    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
        //        if (!isTyping) {
        //            isTyping = true;
        //            setTypingStatus(true);
        //        }
        //        typingHandler.removeCallbacks(stopTypingRunnable);
        //        typingHandler.postDelayed(stopTypingRunnable, 1500);
        //    }
        //    @Override public void afterTextChanged(Editable s) {}
        //});
        // Слушаем статус "печатает" собеседника
        //DatabaseReference typingRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("typing").child(toUserId);
        //typingRef.addValueEventListener(new ValueEventListener() {
        //    @Override
        //    public void onDataChange(@NonNull DataSnapshot snapshot) {
        //        Boolean typing = snapshot.getValue(Boolean.class);
        //        if (typing != null && typing) {
        //            statusView.setText("печатает...");
        //            statusView.setTextColor(0xFF2196F3);
        //        }
        //    }
        //    @Override public void onCancelled(@NonNull DatabaseError error) {}
        //});
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
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(messagesRecyclerView);
    }

    private String makeChatId(String a, String b) {
        List<String> ids = new ArrayList<>();
        ids.add(a); ids.add(b);
        Collections.sort(ids);
        return ids.get(0) + "_" + ids.get(1);
    }

    private void loadMessages() {
        messages.clear();
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                messages.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Message m = ds.getValue(Message.class);
                    if (m != null) messages.add(m);
                }
                messages.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
                messagesAdapter.notifyDataSetChanged();
                // Автопрокрутка вниз
                if (!messages.isEmpty()) {
                    messagesRecyclerView.scrollToPosition(messages.size() - 1);
                }
            }
            @Override
            public void onCancelled(DatabaseError error) {}
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

    private void sendMessage(String text) {
        Message msg = new Message();
        msg.messageId = UUID.randomUUID().toString();
        msg.fromUserId = myId;
        msg.fromUsername = myName;
        msg.toUserId = toUserId;
        msg.text = text;
        msg.timestamp = System.currentTimeMillis();
        if (replyToMessage != null) {
            msg.replyMessageId = replyToMessage.messageId;
            msg.replyText = replyToMessage.text;
            msg.replyUsername = replyToMessage.fromUsername;
        }
        messagesRef.child(msg.messageId).setValue(msg);
        inputMessage.setText("");
        replyToMessage = null;
        binding.replyPanel.setVisibility(View.GONE);
        // После отправки — скроллим вниз
        messagesRecyclerView.post(() -> {
            if (!messages.isEmpty()) {
                messagesRecyclerView.scrollToPosition(messages.size() - 1);
            }
        });
    }

    private void setTypingStatus(boolean typing) {
        DatabaseReference typingRef = FirebaseDatabase.getInstance().getReference("chats").child(chatId).child("typing").child(myId);
        typingRef.setValue(typing);
    }
    private final Runnable stopTypingRunnable = new Runnable() {
        @Override
        public void run() {
            isTyping = false;
            setTypingStatus(false);
        }
    };

    private class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.MessageViewHolder> {
        private final List<Message> messages;
        MessagesAdapter(List<Message> messages) { this.messages = messages; }
        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemMessageBinding binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new MessageViewHolder(binding);
        }
        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            holder.bind(messages.get(position));
        }
        @Override
        public int getItemCount() { return messages.size(); }
        class MessageViewHolder extends RecyclerView.ViewHolder {
            private final ItemMessageBinding binding;
            public MessageViewHolder(ItemMessageBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
            public void bind(Message m) {
                String date = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(new Date(m.timestamp));
                binding.messageText.setText(m.text);
                binding.messageMeta.setText(m.fromUsername + "  " + date);
                FrameLayout.LayoutParams cardParams = (FrameLayout.LayoutParams) binding.messageCard.getLayoutParams();
                if (m.fromUserId != null && m.fromUserId.equals(myId)) {
                    cardParams.gravity = Gravity.END;
                    binding.messageCard.setCardBackgroundColor(0xFFE0E0E0);
                    binding.messageText.setTextColor(0xFF232323);
                } else {
                    cardParams.gravity = Gravity.START;
                    binding.messageCard.setCardBackgroundColor(0xFFFFFFFF);
                    binding.messageText.setTextColor(0xFF232323);
                }
                binding.messageCard.setLayoutParams(cardParams);
                // Отображение блока ответа
                if (m.replyMessageId != null && m.replyText != null) {
                    binding.replyBlock.setVisibility(View.VISIBLE);
                    binding.replyBlockText.setText(m.replyText);
                    binding.replyBlockUsername.setText(m.replyUsername != null ? m.replyUsername : "Ответ");
                } else {
                    binding.replyBlock.setVisibility(View.GONE);
                }
            }
        }
    }
} 