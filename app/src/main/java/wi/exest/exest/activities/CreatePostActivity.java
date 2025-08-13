package wi.exest.exest.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.RadioButton;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.ArrayList;
import java.util.List;
import wi.exest.exest.databinding.FragmentCreatePostBinding;
import wi.exest.exest.models.Post;
import wi.exest.exest.SessionManager;
import androidx.annotation.NonNull;
import android.view.ViewGroup;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.provider.OpenableColumns;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import android.database.Cursor;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import wi.exest.exest.utils.ErrorReporter;

public class CreatePostActivity extends AppCompatActivity {
    private FragmentCreatePostBinding binding;
    private List<Uri> imageUris = new ArrayList<>();
    private Uri documentUri = null;
    private String documentName = null;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> documentPickerLauncher;
    private ImagesAdapter imagesAdapter;
    private List<String> imageBase64List = new ArrayList<>();
    private String documentBase64 = null;

    private static final String[] PROJECT_TYPES = {"Коммерческий", "Социальный", "Образовательный", "Хобби", "Другое"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = FragmentCreatePostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imagesAdapter = new ImagesAdapter(imageUris);
        binding.imagesRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.imagesRecyclerView.setAdapter(imagesAdapter);

        // --- Project type spinner ---
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, PROJECT_TYPES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.projectTypeSpinner.setAdapter(adapter);
        binding.projectTypeSpinner.setSelection(0);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            imageUris.add(uri);
                            // Преобразуем в base64 и сохраняем
                            String base64 = uriToBase64(uri, "image");
                            if (base64 != null) imageBase64List.add(base64);
                            imagesAdapter.notifyDataSetChanged();
                            binding.imagesRecyclerView.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

        documentPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        documentUri = result.getData().getData();
                        if (documentUri != null) {
                            documentName = getFileName(documentUri);
                            binding.documentNameText.setText(documentName);
                            binding.documentNameText.setVisibility(View.VISIBLE);
                            // Преобразуем в base64 и сохраняем
                            documentBase64 = uriToBase64(documentUri, "pdf");
                        }
                    }
                }
        );

        binding.addImageButton.setOnClickListener(v -> pickImage());
        binding.attachDocumentButton.setOnClickListener(v -> pickDocument());
        binding.publishButton.setOnClickListener(v -> publishPost());

        binding.typeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> updateFieldsVisibility());
        updateFieldsVisibility();
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void pickDocument() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        documentPickerLauncher.launch(intent);
    }

    private String uriToBase64(Uri uri, String type) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (type.equals("image")) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                byte[] bytes = baos.toByteArray();
                return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
            } else if (type.equals("pdf")) {
                byte[] bytes = new byte[inputStream.available()];
                inputStream.read(bytes);
                return android.util.Base64.encodeToString(bytes, android.util.Base64.DEFAULT);
            }
        } catch (Exception e) {
            ErrorReporter.showErrorWithReport(this, "Ошибка при создании поста", "CreatePostActivity", e);
        }
        return null;
    }

    private void updateFieldsVisibility() {
        int checkedId = binding.typeRadioGroup.getCheckedRadioButtonId();
        boolean isIdea = checkedId == binding.radioIdea.getId();
        boolean isProject = checkedId == binding.radioProject.getId();
        binding.advantagesEditText.setVisibility(isIdea ? View.VISIBLE : View.GONE);
        binding.preparedEditText.setVisibility(isIdea ? View.VISIBLE : View.GONE);
        binding.projectStatusEditText.setVisibility(isProject ? View.VISIBLE : View.GONE);
        binding.inviteEditText.setVisibility(isProject ? View.VISIBLE : View.GONE);
        binding.attachDocumentButton.setVisibility(isProject ? View.VISIBLE : View.GONE);
        binding.documentNameText.setVisibility(isProject && documentUri != null ? View.VISIBLE : View.GONE);
        binding.projectTypeLabel.setVisibility(isProject ? View.VISIBLE : View.GONE);
        binding.projectTypeSpinner.setVisibility(isProject ? View.VISIBLE : View.GONE);
    }

    private void publishPost() {
        SessionManager session = new SessionManager(this);
        String userId = session.getUserId();
        String username = session.getUsername();
        if (userId == null || username == null) {
            Toast.makeText(this, "Ошибка авторизации", Toast.LENGTH_SHORT).show();
            return;
        }
        int checkedId = binding.typeRadioGroup.getCheckedRadioButtonId();
        String type = "post";
        if (checkedId == binding.radioIdea.getId()) type = "idea";
        else if (checkedId == binding.radioProject.getId()) type = "project";
        String title = binding.titleEditText.getText().toString().trim();
        String description = binding.descriptionEditText.getText().toString().trim();
        String advantages = binding.advantagesEditText.getText().toString().trim();
        String prepared = binding.preparedEditText.getText().toString().trim();
        String projectStatus = binding.projectStatusEditText.getText().toString().trim();
        String invite = binding.inviteEditText.getText().toString().trim();
        String documentUrl = documentUri != null ? documentUri.toString() : null;
        List<String> imageUrls = new ArrayList<>();
        for (Uri uri : imageUris) imageUrls.add(uri.toString());
        long timestamp = System.currentTimeMillis();
        String projectType = null;
        if (type.equals("project")) {
            projectType = (String) binding.projectTypeSpinner.getSelectedItem();
        }
        Post post = new Post();
        post.userId = userId;
        post.username = username;
        post.type = type;
        post.title = title;
        post.description = description;
        post.advantages = isEmpty(advantages) ? null : advantages;
        post.prepared = isEmpty(prepared) ? null : prepared;
        post.projectStatus = isEmpty(projectStatus) ? null : projectStatus;
        post.invite = isEmpty(invite) ? null : invite;
        post.documentBase64 = documentBase64;
        post.imageBase64List = imageBase64List;
        post.timestamp = timestamp;
        post.projectType = projectType;
        // post.tags = ... // можно добавить позже
        DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference("posts");
        String postId = postsRef.push().getKey();
        post.postId = postId;
        postsRef.child(postId).setValue(post)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Пост опубликован!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Ошибка публикации: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    // --- ImagesAdapter (только для предпросмотра локальных uri) ---
    private static class ImagesAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<ImagesAdapter.ImageViewHolder> {
        private final List<Uri> uris;
        ImagesAdapter(List<Uri> uris) { this.uris = uris; }
        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            android.widget.ImageView imageView = new android.widget.ImageView(parent.getContext());
            imageView.setLayoutParams(new ViewGroup.LayoutParams(120, 120));
            imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(4,4,4,4);
            return new ImageViewHolder(imageView);
        }
        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            holder.imageView.setImageURI(uris.get(position));
        }
        @Override
        public int getItemCount() { return uris.size(); }
        static class ImageViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            android.widget.ImageView imageView;
            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = (android.widget.ImageView) itemView;
            }
        }
    }
} 