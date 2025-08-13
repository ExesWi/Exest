package wi.exest.exest.models;

import java.util.List;

public class GroupChat {
    public String groupId;
    public String title;
    public String description;
    public String avatarBase64;
    public String ownerId;
    public List<String> members;
    public long createdAt;

    public GroupChat() {}

    public GroupChat(String groupId, String title, String description, String avatarBase64, String ownerId, List<String> members, long createdAt) {
        this.groupId = groupId;
        this.title = title;
        this.description = description;
        this.avatarBase64 = avatarBase64;
        this.ownerId = ownerId;
        this.members = members;
        this.createdAt = createdAt;
    }
} 