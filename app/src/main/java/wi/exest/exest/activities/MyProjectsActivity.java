package wi.exest.exest.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import wi.exest.exest.SessionManager;
import wi.exest.exest.databinding.ActivityMyProjectsBinding;
import wi.exest.exest.databinding.ItemMyProjectBinding;
import wi.exest.exest.models.UserProject;
import wi.exest.exest.utils.ErrorReporter;

public class MyProjectsActivity extends AppCompatActivity {
    
    private ActivityMyProjectsBinding binding;
    private ProjectsAdapter adapter;
    private List<UserProject> myProjects = new ArrayList<>();
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMyProjectsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupToolbar();
        setupRecyclerView();
        setupFab();
        loadMyProjects();
    }
    
    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }
    
    private void setupRecyclerView() {
        binding.projectsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProjectsAdapter();
        binding.projectsRecyclerView.setAdapter(adapter);
    }
    
    private void setupFab() {
        binding.fabAddProject.setOnClickListener(v -> {
            showCreateProjectDialog();
        });
    }
    
    private void showCreateProjectDialog() {
        // Создаем диалог для создания проекта
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Создать новый проект");
        
        // Создаем layout для диалога
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        scrollView.addView(layout);
        
        // Поля для ввода
        android.widget.EditText titleInput = new android.widget.EditText(this);
        titleInput.setHint("Название проекта");
        layout.addView(titleInput);
        
        android.widget.EditText descriptionInput = new android.widget.EditText(this);
        descriptionInput.setHint("Описание проекта");
        descriptionInput.setMinLines(3);
        layout.addView(descriptionInput);
        
        // Спиннер для типа проекта
        android.widget.Spinner typeSpinner = new android.widget.Spinner(this);
        String[] types = {"Веб-сайт", "Приложение", "Дизайн", "Другое"};
        android.widget.ArrayAdapter<String> typeAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);
        layout.addView(typeSpinner);
        
        // Спиннер для статуса
        android.widget.Spinner statusSpinner = new android.widget.Spinner(this);
        String[] statuses = {"Планирование", "Разработка", "Тестирование", "Завершен"};
        android.widget.ArrayAdapter<String> statusAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);
        layout.addView(statusSpinner);
        
        // Поле для ссылки на результат
        android.widget.EditText resultUrlInput = new android.widget.EditText(this);
        resultUrlInput.setHint("Ссылка на результат (опционально)");
        layout.addView(resultUrlInput);
        
        // Спиннер для типа результата
        android.widget.Spinner resultTypeSpinner = new android.widget.Spinner(this);
        String[] resultTypes = {"Веб-сайт", "Скачать", "Предварительный просмотр"};
        android.widget.ArrayAdapter<String> resultTypeAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, resultTypes);
        resultTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resultTypeSpinner.setAdapter(resultTypeAdapter);
        layout.addView(resultTypeSpinner);
        
        builder.setView(scrollView);
        builder.setPositiveButton("Создать", (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            String type = getProjectTypeFromSpinner(typeSpinner.getSelectedItemPosition());
            String stage = getProjectStageFromSpinner(statusSpinner.getSelectedItemPosition());
            String resultUrl = resultUrlInput.getText().toString().trim();
            String resultType = getResultTypeFromSpinner(resultTypeSpinner.getSelectedItemPosition());
            
            if (!title.isEmpty()) {
                createNewProject(title, description, type, stage, resultUrl, resultType);
            } else {
                Toast.makeText(this, "Введите название проекта", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }
    
    private String getProjectTypeFromSpinner(int position) {
        switch (position) {
            case 0: return "website";
            case 1: return "app";
            case 2: return "design";
            case 3: return "other";
            default: return "other";
        }
    }
    
    private String getProjectStageFromSpinner(int position) {
        switch (position) {
            case 0: return "planning";
            case 1: return "development";
            case 2: return "testing";
            case 3: return "completed";
            default: return "planning";
        }
    }

    private String getResultTypeFromSpinner(int position) {
        switch (position) {
            case 0: return "website";
            case 1: return "download";
            case 2: return "preview";
            default: return "preview";
        }
    }
    
    private void createNewProject(String title, String description, String type, String stage) {
        createNewProject(title, description, type, stage, "", "");
    }

    private void createNewProject(String title, String description, String type, String stage, String resultUrl, String resultType) {
        SessionManager session = new SessionManager(this);
        String myId = session.getUserId();
        if (myId == null) return;
        
        UserProject project = new UserProject();
        project.projectId = FirebaseDatabase.getInstance().getReference("users").child(myId).child("projects").push().getKey();
        project.userId = myId;
        project.title = title;
        project.description = description;
        project.type = type;
        project.stage = stage;
        project.resultUrl = resultUrl;
        project.resultType = resultType;
        project.createdAt = System.currentTimeMillis();
        project.updatedAt = System.currentTimeMillis();
        project.isPublic = true;
        
        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("users").child(myId).child("projects").child(project.projectId);
        projectRef.setValue(project)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Проект создан!", Toast.LENGTH_SHORT).show();
                    loadMyProjects(); // Перезагружаем список
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка создания проекта: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void loadMyProjects() {
        SessionManager session = new SessionManager(this);
        String myId = session.getUserId();
        if (myId == null) return;
        
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.projectsRecyclerView.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.GONE);
        
        // Загружаем проекты из профиля пользователя
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(myId).child("projects");
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                binding.progressBar.setVisibility(View.GONE);
                myProjects.clear();
                
                for (DataSnapshot ds : snapshot.getChildren()) {
                    UserProject project = ds.getValue(UserProject.class);
                    if (project != null) {
                        project.projectId = ds.getKey();
                        myProjects.add(project);
                    }
                }
                
                adapter.notifyDataSetChanged();
                updateUI();
            }
            
            @Override 
            public void onCancelled(@NonNull DatabaseError error) {
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(MyProjectsActivity.this, "Ошибка загрузки проектов", Toast.LENGTH_SHORT).show();
                updateUI();
            }
        });
    }
    
    private void updateUI() {
        if (myProjects.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.projectsRecyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.projectsRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private String getStageText(String stage) {
        switch (stage) {
            case "planning": return "Планирование";
            case "development": return "Разработка";
            case "testing": return "Тестирование";
            case "completed": return "Завершен";
            default: return "Неизвестно";
        }
    }
    
    private String getProjectTypeText(String type) {
        switch (type) {
            case "website": return "Веб-сайт";
            case "app": return "Приложение";
            case "design": return "Дизайн";
            case "other": return "Другое";
            default: return "Неизвестно";
        }
    }
    
    private void showEditProjectDialog(UserProject project) {
        // Создаем диалог для редактирования проекта
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Редактировать проект");
        
        // Создаем layout для диалога
        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        scrollView.addView(layout);
        
        // Поля для ввода
        android.widget.EditText titleInput = new android.widget.EditText(this);
        titleInput.setHint("Название проекта");
        titleInput.setText(project.title != null ? project.title : "");
        layout.addView(titleInput);
        
        android.widget.EditText descriptionInput = new android.widget.EditText(this);
        descriptionInput.setHint("Описание проекта");
        descriptionInput.setMinLines(3);
        descriptionInput.setText(project.description != null ? project.description : "");
        layout.addView(descriptionInput);
        
        // Спиннер для типа проекта
        android.widget.Spinner typeSpinner = new android.widget.Spinner(this);
        String[] types = {"Веб-сайт", "Приложение", "Дизайн", "Другое"};
        android.widget.ArrayAdapter<String> typeAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);
        // Устанавливаем текущий тип
        int typePosition = getTypePositionFromString(project.type);
        typeSpinner.setSelection(typePosition);
        layout.addView(typeSpinner);
        
        // Спиннер для статуса
        android.widget.Spinner statusSpinner = new android.widget.Spinner(this);
        String[] statuses = {"Планирование", "Разработка", "Тестирование", "Завершен"};
        android.widget.ArrayAdapter<String> statusAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(statusAdapter);
        // Устанавливаем текущий статус
        int stagePosition = getStagePositionFromString(project.stage);
        statusSpinner.setSelection(stagePosition);
        layout.addView(statusSpinner);
        
        // Поле для ссылки на результат
        android.widget.EditText resultUrlInput = new android.widget.EditText(this);
        resultUrlInput.setHint("Ссылка на результат (опционально)");
        resultUrlInput.setText(project.resultUrl != null ? project.resultUrl : "");
        layout.addView(resultUrlInput);
        
        // Спиннер для типа результата
        android.widget.Spinner resultTypeSpinner = new android.widget.Spinner(this);
        String[] resultTypes = {"Веб-сайт", "Скачать", "Предварительный просмотр"};
        android.widget.ArrayAdapter<String> resultTypeAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_spinner_item, resultTypes);
        resultTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        resultTypeSpinner.setAdapter(resultTypeAdapter);
        // Устанавливаем текущий тип результата
        int resultTypePosition = getResultTypePositionFromString(project.resultType);
        resultTypeSpinner.setSelection(resultTypePosition);
        layout.addView(resultTypeSpinner);
        
        builder.setView(scrollView);
        builder.setPositiveButton("Сохранить", (dialog, which) -> {
            String title = titleInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            String type = getProjectTypeFromSpinner(typeSpinner.getSelectedItemPosition());
            String stage = getProjectStageFromSpinner(statusSpinner.getSelectedItemPosition());
            String resultUrl = resultUrlInput.getText().toString().trim();
            String resultType = getResultTypeFromSpinner(resultTypeSpinner.getSelectedItemPosition());
            
            if (!title.isEmpty()) {
                updateProject(project, title, description, type, stage, resultUrl, resultType);
            } else {
                Toast.makeText(this, "Введите название проекта", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Отмена", null);
        builder.setNeutralButton("Удалить", (dialog, which) -> {
            showDeleteConfirmationDialog(project);
        });
        builder.show();
    }
    
    private int getTypePositionFromString(String type) {
        if (type == null) return 0;
        switch (type) {
            case "website": return 0;
            case "app": return 1;
            case "design": return 2;
            case "other": return 3;
            default: return 0;
        }
    }
    
    private int getStagePositionFromString(String stage) {
        if (stage == null) return 0;
        switch (stage) {
            case "planning": return 0;
            case "development": return 1;
            case "testing": return 2;
            case "completed": return 3;
            default: return 0;
        }
    }
    
    private int getResultTypePositionFromString(String resultType) {
        if (resultType == null) return 0;
        switch (resultType) {
            case "website": return 0;
            case "download": return 1;
            case "preview": return 2;
            default: return 0;
        }
    }
    
    private void updateProject(UserProject project, String title, String description, String type, String stage, String resultUrl, String resultType) {
        SessionManager session = new SessionManager(this);
        String myId = session.getUserId();
        if (myId == null || project.projectId == null) return;
        
        project.title = title;
        project.description = description;
        project.type = type;
        project.stage = stage;
        project.resultUrl = resultUrl;
        project.resultType = resultType;
        project.updatedAt = System.currentTimeMillis();
        
        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("users").child(myId).child("projects").child(project.projectId);
        projectRef.setValue(project)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Проект обновлен!", Toast.LENGTH_SHORT).show();
                    loadMyProjects(); // Перезагружаем список
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка обновления проекта: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showDeleteConfirmationDialog(UserProject project) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Подтверждение удаления");
        builder.setMessage("Вы уверены, что хотите удалить проект: " + project.title + "?");
        builder.setPositiveButton("Да", (dialog, which) -> {
            deleteProject(project);
        });
        builder.setNegativeButton("Нет", null);
        builder.show();
    }

    private void deleteProject(UserProject project) {
        SessionManager session = new SessionManager(this);
        String myId = session.getUserId();
        if (myId == null || project.projectId == null) return;

        DatabaseReference projectRef = FirebaseDatabase.getInstance().getReference("users").child(myId).child("projects").child(project.projectId);
        projectRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Проект удален!", Toast.LENGTH_SHORT).show();
                    loadMyProjects(); // Перезагружаем список
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Ошибка удаления проекта: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    
    private class ProjectsAdapter extends RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder> {
        
        @NonNull
        @Override
        public ProjectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemMyProjectBinding itemBinding = ItemMyProjectBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
            return new ProjectViewHolder(itemBinding);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ProjectViewHolder holder, int position) {
            holder.bind(myProjects.get(position));
        }
        
        @Override
        public int getItemCount() {
            return myProjects.size();
        }
        
        class ProjectViewHolder extends RecyclerView.ViewHolder {
            private final ItemMyProjectBinding itemBinding;
            
            ProjectViewHolder(@NonNull ItemMyProjectBinding binding) {
                super(binding.getRoot());
                this.itemBinding = binding;
            }
            
            void bind(UserProject project) {
                // Заголовок
                itemBinding.projectTitle.setText(project.title != null ? project.title : "Без названия");
                
                // Описание
                if (project.description != null && !project.description.isEmpty()) {
                    itemBinding.projectDescription.setText(project.description);
                    itemBinding.projectDescription.setVisibility(View.VISIBLE);
                } else {
                    itemBinding.projectDescription.setVisibility(View.GONE);
                }
                
                // Тип проекта
                if (project.type != null && !project.type.isEmpty()) {
                    itemBinding.projectType.setText(getProjectTypeText(project.type));
                    itemBinding.projectType.setVisibility(View.VISIBLE);
                } else {
                    itemBinding.projectType.setVisibility(View.GONE);
                }
                
                // Статус проекта
                if (project.stage != null && !project.stage.isEmpty()) {
                    itemBinding.projectStatus.setText(getStageText(project.stage));
                    itemBinding.projectStatus.setVisibility(View.VISIBLE);
                } else {
                    itemBinding.projectStatus.setVisibility(View.GONE);
                }
                
                // Дата создания
                if (project.createdAt > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    String date = sdf.format(new Date(project.createdAt));
                    itemBinding.projectDate.setText(date);
                } else {
                    itemBinding.projectDate.setText("Неизвестно");
                }
                
                // Результат проекта
                if (project.resultUrl != null && !project.resultUrl.isEmpty()) {
                    itemBinding.resultButton.setVisibility(View.VISIBLE);
                    if ("website".equals(project.resultType)) {
                        itemBinding.resultButton.setText("Перейти на сайт");
                    } else if ("download".equals(project.resultType)) {
                        itemBinding.resultButton.setText("Скачать");
                    } else {
                        itemBinding.resultButton.setText("Посмотреть");
                    }
                    
                    itemBinding.resultButton.setOnClickListener(v -> {
                        try {
                            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(project.resultUrl));
                            startActivity(intent);
                        } catch (Exception e) {
                            ErrorReporter.showErrorWithReport(MyProjectsActivity.this, "Ошибка открытия ссылки", "MyProjectsActivity", e);
                        }
                    });
                } else {
                    itemBinding.resultButton.setVisibility(View.GONE);
                }
                

                if (project.imageBase64List != null && !project.imageBase64List.isEmpty()) {
                    try {
                        String firstImage = project.imageBase64List.get(0);
                        byte[] decodedBytes = Base64.decode(firstImage, Base64.DEFAULT);
                        Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                        if (bitmap != null) {
                            itemBinding.projectImage.setImageBitmap(bitmap);
                        }
                    } catch (Exception e) {
                        // Оставляем дефолтную иконку
                    }
                }
                
                // Клик по проекту
                itemView.setOnClickListener(v -> {
                    // TODO: Открыть детальный просмотр проекта
                    Toast.makeText(MyProjectsActivity.this, "Открыть проект: " + project.title, Toast.LENGTH_SHORT).show();
                });
                
                // Длительное нажатие для редактирования
                itemView.setOnLongClickListener(v -> {
                    showEditProjectDialog(project);
                    return true;
                });
            }
        }
    }
} 