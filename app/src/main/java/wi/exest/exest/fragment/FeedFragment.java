package wi.exest.exest.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
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
import wi.exest.exest.databinding.FragmentFeedBinding;
import wi.exest.exest.databinding.PostItemBinding;
import wi.exest.exest.models.Post;
import wi.exest.exest.utils.ErrorReporter;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.util.Base64;
import android.widget.ImageView;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import wi.exest.exest.activities.CreatePostActivity;
import com.google.android.material.appbar.MaterialToolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.appcompat.widget.SearchView;
import wi.exest.exest.R;
import com.google.firebase.database.ServerValue;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.firebase.auth.FirebaseAuth;
import wi.exest.exest.SessionManager;
import android.content.Context;
import android.content.Intent;
import wi.exest.exest.activities.UserProfileActivity;
import androidx.core.content.FileProvider;

public class FeedFragment extends Fragment {
    private FragmentFeedBinding binding;
    private FeedAdapter feedAdapter;
    private List<Post> postList = new ArrayList<>();
    private DatabaseReference postsRef;
    private String currentSearch = "";
    private String currentFilter = "recent"; // recent, popular

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFeedBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        postsRef = FirebaseDatabase.getInstance().getReference("posts");
        feedAdapter = new FeedAdapter(postList);
        binding.feedRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.feedRecyclerView.setAdapter(feedAdapter);
        loadPosts();
        binding.addPostFab.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), CreatePostActivity.class));
        });
        
        MaterialToolbar toolbar = binding.feedToolbar;
        toolbar.setTitle(getString(R.string.app_name));
        toolbar.setOnMenuItemClickListener(item -> onToolbarMenuItemClick(item));
    }

    private boolean onToolbarMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            showSearchDialog();
            return true;
        }
        if (item.getItemId() == R.id.action_filter) {
            showFilterDialog();
            return true;
        }
        return false;
    }

    private void showSearchDialog() {
        final android.widget.EditText input = new android.widget.EditText(requireContext());
        input.setHint("Поиск по постам...");
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("Поиск")
            .setView(input)
            .setPositiveButton("Искать", (dialog, which) -> {
                currentSearch = input.getText().toString().trim();
                filterAndShowPosts();
            })
            .setNegativeButton("Отмена", null)
            .show();
    }

    private void showFilterDialog() {
        String[] filters = {"По актуальности", "По популярности"};
        new android.app.AlertDialog.Builder(requireContext())
            .setTitle("Сортировка")
            .setItems(filters, (dialog, which) -> {
                if (which == 0) currentFilter = "recent";
                else currentFilter = "popular";
                filterAndShowPosts();
            })
            .show();
    }

    private void filterAndShowPosts() {
        List<Post> filtered = new ArrayList<>();
        for (Post post : postList) {
            if (TextUtils.isEmpty(currentSearch) ||
                (post.title != null && post.title.toLowerCase().contains(currentSearch.toLowerCase())) ||
                (post.description != null && post.description.toLowerCase().contains(currentSearch.toLowerCase()))) {
                filtered.add(post);
            }
        }
        if (currentFilter.equals("popular")) {
            filtered.sort((a, b) -> Integer.compare(
                (b.reactions != null ? sumReactions(b.reactions) : 0) + b.commentsCount,
                (a.reactions != null ? sumReactions(a.reactions) : 0) + a.commentsCount));
        } else {
            filtered.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));
        }
        feedAdapter.setData(filtered);
    }

    private void loadPosts() {
        postsRef.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Post post = dataSnapshot.getValue(Post.class);
                    if (post != null) {
                        postList.add(0, post); // новые сверху
                    }
                }
                filterAndShowPosts();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                ErrorReporter.logError(requireContext(), "Ошибка загрузки постов", "FeedFragment", error.toException());
            }
        });
    }

    private int sumReactions(java.util.Map<String, java.util.Map<String, Boolean>> map) {
        int sum = 0;
        if (map == null) return 0;
        for (java.util.Map<String, Boolean> users : map.values()) {
            if (users != null) sum += users.size();
        }
        return sum;
    }

    // --- FeedAdapter ---
    public static class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.PostViewHolder> {
        private List<Post> posts;
        public FeedAdapter(List<Post> posts) { this.posts = posts; }
        public void setData(List<Post> newPosts) {
            this.posts = newPosts;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            PostItemBinding binding = PostItemBinding.inflate(inflater, parent, false);
            return new PostViewHolder(binding);
        }

        @Override
        public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
            holder.bind(posts.get(position));
        }

        @Override
        public int getItemCount() { return posts.size(); }

        static class PostViewHolder extends RecyclerView.ViewHolder {
            private final PostItemBinding binding;
            private Post currentPost;
            public PostViewHolder(PostItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
            public void bind(Post post) {
                this.currentPost = post;
                // Username
                binding.postUsername.setText(post.username);
                // Аватарка
                binding.postAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                if (post.userId != null) {
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(post.userId);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String avatarBase64 = snapshot.child("avatarBase64").getValue(String.class);
                            if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                                byte[] decodedBytes = Base64.decode(avatarBase64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                binding.postAvatar.setImageBitmap(bitmap);
                            } else {
                                binding.postAvatar.setImageResource(android.R.drawable.sym_def_app_icon);
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                    });
                }
                // Клик по username — открыть профиль
                binding.postUsername.setOnClickListener(v -> {
                    if (post.userId != null) {
                        Intent intent = new Intent(binding.getRoot().getContext(), UserProfileActivity.class);
                        intent.putExtra("userId", post.userId);
                        binding.getRoot().getContext().startActivity(intent);
                    }
                });
                // Tag
                String tag = "#" + (post.type != null ? post.type : "пост");
                binding.postTag.setText(tag);
                // Title
                if (!TextUtils.isEmpty(post.title)) {
                    binding.postTitle.setVisibility(View.VISIBLE);
                    binding.postTitle.setText(post.title);
                } else {
                    binding.postTitle.setVisibility(View.GONE);
                }
                // Description
                binding.postDescription.setText(post.description != null ? post.description : "");
                // Advantages (for idea)
                if (!TextUtils.isEmpty(post.advantages)) {
                    binding.postAdvantages.setVisibility(View.VISIBLE);
                    binding.postAdvantages.setText("Преимущества: " + post.advantages);
                } else {
                    binding.postAdvantages.setVisibility(View.GONE);
                }
                // Prepared (for idea)
                if (!TextUtils.isEmpty(post.prepared)) {
                    binding.postPrepared.setVisibility(View.VISIBLE);
                    binding.postPrepared.setText("Подготовлено: " + post.prepared);
                } else {
                    binding.postPrepared.setVisibility(View.GONE);
                }
                // Project status (for project)
                if (!TextUtils.isEmpty(post.projectStatus)) {
                    binding.postProjectStatus.setVisibility(View.VISIBLE);
                    binding.postProjectStatus.setText("Статус: " + post.projectStatus);
                } else {
                    binding.postProjectStatus.setVisibility(View.GONE);
                }
                // Invite (for project)
                if (!TextUtils.isEmpty(post.invite)) {
                    binding.postInvite.setVisibility(View.VISIBLE);
                    binding.postInvite.setText("Приглашение: " + post.invite);
                } else {
                    binding.postInvite.setVisibility(View.GONE);
                }
                // Document (for project)
                if (!TextUtils.isEmpty(post.documentUrl)) {
                    binding.postDocument.setVisibility(View.VISIBLE);
                    binding.postDocument.setText("Документ: " + post.documentUrl);
                } else {
                    binding.postDocument.setVisibility(View.GONE);
                }
                // Timestamp
                if (post.timestamp > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                    String date = sdf.format(new Date(post.timestamp));
                    binding.postTimestamp.setText(date);
                } else {
                    binding.postTimestamp.setText("");
                }
                // Images
                if (post.imageBase64List != null && !post.imageBase64List.isEmpty()) {
                    binding.postImagesRecyclerView.setVisibility(View.VISIBLE);
                    binding.postImagesRecyclerView.setLayoutManager(new LinearLayoutManager(binding.getRoot().getContext(), LinearLayoutManager.HORIZONTAL, false));
                    binding.postImagesRecyclerView.setAdapter(new ImagesAdapter(post.imageBase64List));
                } else {
                    binding.postImagesRecyclerView.setVisibility(View.GONE);
                }
                // Document (PDF)
                if (post.documentBase64 != null && !post.documentBase64.isEmpty()) {
                    binding.postDocument.setVisibility(View.VISIBLE);
                    binding.postDocument.setText("Открыть документ (PDF)");
                    binding.postDocument.setOnClickListener(v -> openPdf(post.documentBase64, binding.getRoot().getContext()));
                } else {
                    binding.postDocument.setVisibility(View.GONE);
                }
                // Реакции
                String myId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "anon";
                int like = post.reactions != null && post.reactions.containsKey("like") ? post.reactions.get("like").size() : 0;
                int fire = post.reactions != null && post.reactions.containsKey("fire") ? post.reactions.get("fire").size() : 0;
                int wow = post.reactions != null && post.reactions.containsKey("wow") ? post.reactions.get("wow").size() : 0;
                binding.reactionLike.setText("👍 " + like);
                binding.reactionFire.setText("🔥 " + fire);
                binding.reactionWow.setText("😮 " + wow);
                binding.reactionLike.setOnClickListener(v -> addReaction("like", myId));
                binding.reactionFire.setOnClickListener(v -> addReaction("fire", myId));
                binding.reactionWow.setOnClickListener(v -> addReaction("wow", myId));
                // Комментарии
                int comments = post.commentsCount;
                binding.commentsButton.setText("Комментарии (" + comments + ")");
                binding.commentsButton.setOnClickListener(v -> openCommentsScreen(post));
            }
            private void openPdf(String base64, Context context) {
                try {
                    byte[] pdfBytes = Base64.decode(base64, Base64.DEFAULT);
                    File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "temp_post.pdf");
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(pdfBytes);
                    }
                    Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "application/pdf");
                    intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(intent);
                } catch (IOException e) {
                    ErrorReporter.showErrorWithReport(context, "Ошибка при открытии PDF документа", "FeedFragment", e);
                }
            }
            private void addReaction(String type, String userId) {
                if (currentPost == null || currentPost.postId == null) return;
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("posts").child(currentPost.postId).child("reactions").child(type).child(userId);
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            ref.setValue(true); // Ставим реакцию
                        }
                        // Если уже есть — ничего не делаем (или можно снять реакцию: ref.removeValue())
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            private void showCommentsDialog(Post post) {
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(binding.getRoot().getContext());
                builder.setTitle("Комментарии");
                ScrollView scrollView = new ScrollView(binding.getRoot().getContext());
                LinearLayout layout = new LinearLayout(binding.getRoot().getContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                scrollView.addView(layout);
                // Показываем последние 10 комментариев
                java.util.List<Post.Comment> comments = new java.util.ArrayList<>();
                if (post.comments != null) comments.addAll(post.comments.values());
                comments.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
                int start = Math.max(0, comments.size() - 10);
                for (int i = start; i < comments.size(); i++) {
                    Post.Comment c = comments.get(i);
                    TextView tv = new TextView(binding.getRoot().getContext());
                    String date = new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(new java.util.Date(c.timestamp));
                    tv.setText(c.username + ": " + c.text + "\n" + date);
                    tv.setPadding(0, 8, 0, 8);
                    layout.addView(tv);
                }
                // Поле для нового комментария
                final EditText input = new EditText(binding.getRoot().getContext());
                input.setHint("Ваш комментарий...");
                layout.addView(input);
                builder.setView(scrollView);
                builder.setPositiveButton("Отправить", (dialog, which) -> {
                    String text = input.getText().toString().trim();
                    if (!text.isEmpty()) addComment(post, text);
                });
                builder.setNegativeButton("Закрыть", null);
                builder.show();
            }
            private void addComment(Post post, String text) {
                String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "anon";
                String username = "Аноним";
                // Можно получить username из SessionManager, если нужно
                Post.Comment comment = new Post.Comment();
                comment.commentId = java.util.UUID.randomUUID().toString();
                comment.userId = userId;
                comment.username = username;
                comment.text = text;
                comment.timestamp = System.currentTimeMillis();
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("posts").child(post.postId).child("comments").child(comment.commentId);
                ref.setValue(comment);
                // Увеличиваем счетчик комментариев
                DatabaseReference countRef = FirebaseDatabase.getInstance().getReference("posts").child(post.postId).child("commentsCount");
                countRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
                    @NonNull
                    @Override
                    public com.google.firebase.database.Transaction.Result doTransaction(@NonNull com.google.firebase.database.MutableData currentData) {
                        Integer value = currentData.getValue(Integer.class);
                        if (value == null) value = 0;
                        currentData.setValue(value + 1);
                        return com.google.firebase.database.Transaction.success(currentData);
                    }
                    @Override
                    public void onComplete(DatabaseError error, boolean committed, DataSnapshot currentData) {}
                });
            }
            private void openCommentsScreen(Post post) {
                Intent intent = new Intent(binding.getRoot().getContext(), wi.exest.exest.activities.CommentsActivity.class);
                intent.putExtra("postId", post.postId);
                binding.getRoot().getContext().startActivity(intent);
            }
        }
        // --- ImagesAdapter для base64 изображений ---
        static class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.ImageViewHolder> {
            private final List<String> base64List;
            ImagesAdapter(List<String> base64List) { this.base64List = base64List; }
            @NonNull
            @Override
            public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                ImageView imageView = new ImageView(parent.getContext());
                imageView.setLayoutParams(new ViewGroup.LayoutParams(180, 180));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setPadding(4,4,4,4);
                return new ImageViewHolder(imageView);
            }
            @Override
            public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
                String base64 = base64List.get(position);
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                holder.imageView.setImageBitmap(bitmap);
                
                // Добавляем обработчик клика для полноэкранного просмотра
                holder.imageView.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(v.getContext(), wi.exest.exest.activities.ImageViewerActivity.class);
                        intent.putExtra("image_base64", base64);
                        v.getContext().startActivity(intent);
                    } catch (Exception e) {
                        ErrorReporter.showErrorWithReport(v.getContext(), 
                            "Ошибка открытия изображения", "FeedFragment", e);
                    }
                });
            }
            @Override
            public int getItemCount() { return base64List.size(); }
            static class ImageViewHolder extends RecyclerView.ViewHolder {
                ImageView imageView;
                public ImageViewHolder(@NonNull View itemView) {
                    super(itemView);
                    imageView = (ImageView) itemView;
                }
            }
        }
    }
}