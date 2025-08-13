package wi.exest.exest.models;

import com.google.firebase.database.IgnoreExtraProperties;
import java.util.List;

@IgnoreExtraProperties
public class Course {
    
    public String courseId;
    public String title;
    public String description;
    public String category; // programming, design, business, marketing
    public String instructor;
    public String instructorId;
    public String imageBase64;
    public String videoUrl;
    public String duration; // например "2 часа 30 минут"
    public int lessonsCount;
    public String difficulty; // beginner, intermediate, advanced
    public double rating;
    public int studentsCount;
    public boolean isFree;
    public double price;
    public String currency; // USD, EUR, RUB
    public List<String> tags;
    public List<String> requirements;
    public List<String> whatYouWillLearn;
    public long createdAt;
    public long updatedAt;
    public boolean isPublished;
    
    // Обязательный конструктор по умолчанию для Firebase
    public Course() {
    }
    
    // Конструктор с основными параметрами
    public Course(String title, String description, String category, String instructor, String instructorId) {
        this.title = title;
        this.description = description;
        this.category = category;
        this.instructor = instructor;
        this.instructorId = instructorId;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isPublished = false;
        this.isFree = true;
        this.price = 0.0;
        this.currency = "RUB";
        this.rating = 0.0;
        this.studentsCount = 0;
        this.lessonsCount = 0;
        this.difficulty = "beginner";
    }
} 