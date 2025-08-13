package wi.exest.exest.models;

import com.google.firebase.database.IgnoreExtraProperties;

import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class User {

    public String userId;

    public String username;
    public String usernameLowercase;
    public String email;
    public String avatarBase64;
    public long createdAt;
    public List<String> skills; // навыки (теги)
    public List<String> interests; // интересы (теги)
    public String about; // о себе
    public String portfolioUrl; // ссылка на портфолио
    public Map<String, UserProject> projects; // проекты пользователя

    // Обязательный конструктор по умолчанию для Firebase
    public User() {
    }

    // Конструктор с явным указанием параметров
    public User(String username, String email) {
        this.username = username;
        this.usernameLowercase = username != null ? username.toLowerCase() : " ";
        this.email = email;
        this.avatarBase64 = "";
        this.createdAt = System.currentTimeMillis();
    }
}