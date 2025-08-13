package wi.exest.exest.models;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.List;

@IgnoreExtraProperties
public class UserProject {
    public String projectId;
    public String userId;
    public String title;
    public String description;
    public String type; // "website", "app", "design", "other"
    public String stage; // "planning", "development", "testing", "completed"
    public String resultUrl; // ссылка на сайт или файл
    public String resultType; // "website", "download", "preview"
    public String resultTitle; // название результата
    public String resultDescription; // описание результата
    public List<String> imageBase64List; // скриншоты проекта
    public long createdAt;
    public long updatedAt;
    public boolean isPublic; // публичный ли проект

    public UserProject() {}

    public UserProject(String userId, String title, String description, String type, String stage) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.type = type;
        this.stage = stage;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isPublic = true;
    }
} 