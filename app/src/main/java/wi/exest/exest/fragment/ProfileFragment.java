package wi.exest.exest.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import wi.exest.exest.R;
import wi.exest.exest.SessionManager;
import wi.exest.exest.activities.LoginActivity;
import wi.exest.exest.databinding.FragmentProfileBinding;
import wi.exest.exest.models.User;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Button;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.ArrayList;
import android.util.Patterns;
import com.google.android.material.appbar.MaterialToolbar;
import wi.exest.exest.activities.SettingsActivity;
import wi.exest.exest.activities.UserProfileActivity;
import wi.exest.exest.activities.MyProjectsActivity;
import wi.exest.exest.utils.ErrorReporter;


public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private FirebaseDatabase database;
    private SessionManager sessionManager;
    private Uri currentPhotoUri;

    // Launchers для обработки результатов
    private ActivityResultLauncher<Intent> takePictureLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    private static final String[] SKILLS_PRESET = {"Программирование", "Дизайн", "Маркетинг", "Менеджмент", "Аналитика", "Тестирование", "Копирайтинг"};
    private static final String[] INTERESTS_PRESET = {"Стартапы", "Экология", "Мобильная разработка", "AI", "Финансы", "Образование", "ЗОЖ"};

    private User currentUser;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = FirebaseDatabase.getInstance();
        sessionManager = new SessionManager(requireActivity());

        // Инициализация ActivityResultLauncher
        initLaunchers();
    }

    private void initLaunchers() {
        // Launcher для запроса разрешения на чтение хранилища
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImagePicker();
                    } else {
                        Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );

        // Launcher для выбора изображения из галереи
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                        Uri selectedImage = result.getData().getData();
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                    requireActivity().getContentResolver(),
                                    selectedImage
                            );
                            setNewAvatar(bitmap);
                        } catch (IOException e) {
                            ErrorReporter.showErrorWithReport(requireContext(), "Ошибка загрузки изображения из галереи", "ProfileFragment", e);
                        }
                    }
                }
        );

        // Launcher для съемки фото
        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == getActivity().RESULT_OK) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                                    requireActivity().getContentResolver(),
                                    currentPhotoUri
                            );
                            setNewAvatar(bitmap);
                        } catch (IOException e) {
                            ErrorReporter.showErrorWithReport(requireContext(), "Ошибка загрузки изображения с камеры", "ProfileFragment", e);
                        }
                    }
                }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loadUserData();

        binding.profileImage.setOnClickListener(v -> showImageSourceDialog());
        binding.logoutButton.setOnClickListener(v -> logoutUser());

        // Переходы по разделам
        binding.sectionInfo.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), wi.exest.exest.activities.ProfileInfoActivity.class));
        });
        binding.sectionPortfolio.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), wi.exest.exest.activities.ProfilePortfolioActivity.class));
        });
        binding.sectionSettings.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), wi.exest.exest.activities.SettingsActivity.class));
        });
        binding.sectionMyProjects.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), wi.exest.exest.activities.MyProjectsActivity.class));
        });

        // Обработка меню Toolbar
    }

    private void showImageSourceDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Choose source")
                .setItems(new CharSequence[]{"Gallery", "Camera"}, (dialog, which) -> {
                    switch (which) {
                        case 0: // Gallery
                            checkStoragePermission();
                            break;
                        case 1: // Camera
                            dispatchTakePictureIntent();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkStoragePermission() {
        String permission;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES;
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE;
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            requestPermissionLauncher.launch(permission);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = createImageFile();
            if (photoFile != null) {
                currentPhotoUri = FileProvider.getUriForFile(
                        requireContext(),
                        requireContext().getPackageName() + ".fileprovider",
                        photoFile
                );
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                takePictureLauncher.launch(takePictureIntent);
            }
        }
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        try {
            return File.createTempFile(imageFileName, ".jpg", storageDir);
        } catch (IOException e) {
            ErrorReporter.showErrorWithReport(requireContext(), "Ошибка создания файла изображения", "ProfileFragment", e);
            return null;
        }
    }

    private void loadUserData() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.progressBar.setVisibility(View.VISIBLE);
        DatabaseReference userRef = database.getReference("users").child(userId);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressBar.setVisibility(View.GONE);
                if (snapshot.exists()) {
                    User user = snapshot.getValue(User.class);
                    currentUser = user;
                    if (user != null) {
                        // Логирование загруженных данных
                        Log.d("ProfileDebug", "Loaded user: " + user.username + ", " + user.email);
                        binding.usernameText.setText(user.username); // Под аватаркой
                        binding.userEmail.setText(user.email); // Email
                        // Аватарка
                        if (user.avatarBase64 != null && !user.avatarBase64.isEmpty()) {
                            try {
                                byte[] decodedBytes = Base64.decode(user.avatarBase64, Base64.DEFAULT);
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                if (bitmap != null) {
                                    binding.profileImage.setImageBitmap(bitmap);
                                } else {
                                    binding.profileImage.setImageResource(R.drawable.ic_default_avatar);
                                }
                            } catch (Exception e) {
                                ErrorReporter.logError(requireContext(), "Ошибка загрузки аватара пользователя", "ProfileFragment", e);
                                binding.profileImage.setImageResource(R.drawable.ic_default_avatar);
                            }
                        } else {
                            binding.profileImage.setImageResource(R.drawable.ic_default_avatar);
                        }
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Failed to load data", Toast.LENGTH_SHORT).show();
                Log.e("ProfileError", "DB load failed", error.toException());
            }
        });
    }

    private void setNewAvatar(Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(requireContext(), "Invalid image", Toast.LENGTH_SHORT).show();
            return;
        }

        // Compress image
        int maxSize = 1024;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float ratio = (float) width / (float) height;

        if (width > height && width > maxSize) {
            width = maxSize;
            height = (int) (width / ratio);
        } else if (height > width && height > maxSize) {
            height = maxSize;
            width = (int) (height * ratio);
        } else if (width == height && width > maxSize) {
            width = maxSize;
            height = maxSize;
        }

        Bitmap compressedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);

        try {
            // Convert to base64
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            compressedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);

            // Save to database
            String userId = sessionManager.getUserId();
            if (userId != null) {
                binding.progressBar.setVisibility(View.VISIBLE);

                DatabaseReference userRef = database.getReference("users").child(userId).child("avatarBase64");
                userRef.setValue(base64Image)
                        .addOnSuccessListener(aVoid -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.profileImage.setImageBitmap(compressedBitmap);
                            Toast.makeText(requireContext(), "Avatar updated", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            binding.progressBar.setVisibility(View.GONE);
                            Toast.makeText(requireContext(), "Save error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("ProfileFragment", "Error saving avatar", e);
                        });
            }
        } catch (Exception e) {
            binding.progressBar.setVisibility(View.GONE);
            ErrorReporter.showErrorWithReport(requireContext(), "Ошибка обработки изображения", "ProfileFragment", e);
        }
    }

    private void logoutUser() {
        FirebaseAuth.getInstance().signOut();
        sessionManager.logout();
        startActivity(new Intent(requireActivity(), LoginActivity.class));
        requireActivity().finishAffinity();
    }

    private void showSimpleDialog(String title, String message) {
        new AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show();
    }
}