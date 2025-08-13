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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import wi.exest.exest.models.Post;
import wi.exest.exest.SessionManager;
import wi.exest.exest.R;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import wi.exest.exest.databinding.ActivityCommentsBinding;
import wi.exest.exest.databinding.ItemCommentBinding;
import androidx.annotation.NonNull;
import android.graphics.BitmapFactory;
import android.util.Base64;
import de.hdodenhof.circleimageview.CircleImageView;
import android.content.Intent;
import wi.exest.exest.activities.UserProfileActivity;

public class CommentsActivity extends AppCompatActivity {
    private ActivityCommentsBinding binding;
    private CommentsAdapter adapter;
    private List<Post.Comment> comments = new ArrayList<>();
    private String postId;
    private String userId;
    private String username;
    private DatabaseReference commentsRef;
    private DatabaseReference countRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCommentsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        postId = getIntent().getStringExtra("postId");
        SessionManager session = new SessionManager(this);
        userId = session.getUserId();
        username = session.getUsername();
        commentsRef = FirebaseDatabase.getInstance().getReference("posts").child(postId).child("comments");
        countRef = FirebaseDatabase.getInstance().getReference("posts").child(postId).child("commentsCount");
        adapter = new CommentsAdapter();
        binding.commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.commentsRecyclerView.setAdapter(adapter);
        binding.sendButton.setOnClickListener(v -> {
            String text = binding.inputComment.getText().toString().trim();
            if (!text.isEmpty()) addComment(text);
        });
        loadComments();
    }

    private void loadComments() {
        commentsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                comments.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Post.Comment c = ds.getValue(Post.Comment.class);
                    if (c != null) comments.add(c);
                }
                comments.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
                adapter.notifyDataSetChanged();
                binding.commentsRecyclerView.scrollToPosition(comments.size() - 1);
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void addComment(String text) {
        Post.Comment comment = new Post.Comment();
        comment.commentId = java.util.UUID.randomUUID().toString();
        comment.userId = userId;
        comment.username = username;
        comment.text = text;
        comment.timestamp = System.currentTimeMillis();
        commentsRef.child(comment.commentId).setValue(comment);
        countRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(com.google.firebase.database.MutableData currentData) {
                Integer value = currentData.getValue(Integer.class);
                if (value == null) value = 0;
                currentData.setValue(value + 1);
                return com.google.firebase.database.Transaction.success(currentData);
            }
            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {}
        });
        binding.inputComment.setText("");
    }

    private void editComment(Post.Comment c, String newText) {
        commentsRef.child(c.commentId).child("text").setValue(newText);
    }

    private void deleteComment(Post.Comment c) {
        commentsRef.child(c.commentId).removeValue();
        countRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(com.google.firebase.database.MutableData currentData) {
                Integer value = currentData.getValue(Integer.class);
                if (value == null || value <= 0) value = 1;
                currentData.setValue(value - 1);
                return com.google.firebase.database.Transaction.success(currentData);
            }
            @Override public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {}
        });
    }

    private class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {
        @NonNull
        @Override
        public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemCommentBinding itemBinding = ItemCommentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new CommentViewHolder(itemBinding);
        }
        @Override
        public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
            holder.bind(comments.get(position));
        }
        @Override
        public int getItemCount() { return comments.size(); }
        class CommentViewHolder extends RecyclerView.ViewHolder {
            private final ItemCommentBinding itemBinding;
            public CommentViewHolder(ItemCommentBinding binding) {
                super(binding.getRoot());
                this.itemBinding = binding;
            }
            public void bind(Post.Comment c) {
                itemBinding.commentUsername.setText(c.username);
                String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(c.timestamp));
                itemBinding.commentDate.setText(date);
                itemBinding.commentText.setText(c.text);
                // Аватарка
                itemBinding.commentAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                if (c.userId != null) {
                    FirebaseDatabase.getInstance().getReference("users").child(c.userId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot snapshot) {
                                String avatarBase64 = snapshot.child("avatarBase64").getValue(String.class);
                                if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                                    byte[] decodedBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
                                    itemBinding.commentAvatar.setImageBitmap(BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length));
                                } else {
                                    itemBinding.commentAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                                }
                            }
                            @Override public void onCancelled(DatabaseError error) {}
                        });
                }
                // Клик по аватарке и username — открыть профиль
                View.OnClickListener openProfile = v -> {
                    if (c.userId != null) {
                        Intent intent = new Intent(itemBinding.getRoot().getContext(), UserProfileActivity.class);
                        intent.putExtra("userId", c.userId);
                        itemBinding.getRoot().getContext().startActivity(intent);
                    }
                };
                itemBinding.commentAvatar.setOnClickListener(openProfile);
                itemBinding.commentUsername.setOnClickListener(openProfile);
                // Кнопки редактировать/удалить только для своих
                itemBinding.commentActions.removeAllViews();
                if (userId != null && userId.equals(c.userId)) {
                    Button editBtn = new Button(itemBinding.getRoot().getContext());
                    editBtn.setText("Редактировать");
                    editBtn.setTextSize(12f);
                    editBtn.setOnClickListener(v -> showEditDialog(c));
                    Button delBtn = new Button(itemBinding.getRoot().getContext());
                    delBtn.setText("Удалить");
                    delBtn.setTextSize(12f);
                    delBtn.setOnClickListener(v -> deleteComment(c));
                    itemBinding.commentActions.addView(editBtn);
                    itemBinding.commentActions.addView(delBtn);
                }
            }
        }
    }

    private void showEditDialog(Post.Comment c) {
        final EditText input = new EditText(this);
        input.setText(c.text);
        new android.app.AlertDialog.Builder(this)
            .setTitle("Редактировать комментарий")
            .setView(input)
            .setPositiveButton("Сохранить", (dialog, which) -> {
                String newText = input.getText().toString().trim();
                if (!newText.isEmpty()) {
                    editComment(c, newText);
                }
            })
            .setNegativeButton("Отмена", null)
            .show();
    }
} 