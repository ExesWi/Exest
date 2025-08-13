package wi.exest.exest.adapter;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import wi.exest.exest.R;
import wi.exest.exest.models.User;

public class UserSelectAdapter extends RecyclerView.Adapter<UserSelectAdapter.UserViewHolder> {
    private final List<User> userList;
    private final Set<String> selectedIds = new HashSet<>();
    public UserSelectAdapter(List<User> userList) {
        this.userList = userList;
    }
    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_item, parent, false);
        return new UserViewHolder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        holder.bind(user, selectedIds.contains(user.userId));
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedIds.contains(user.userId));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) selectedIds.add(user.userId);
            else selectedIds.remove(user.userId);
        });
        holder.itemView.setOnClickListener(v -> {
            boolean checked = !holder.checkBox.isChecked();
            holder.checkBox.setChecked(checked);
        });
    }
    @Override
    public int getItemCount() { return userList.size(); }
    public List<User> getSelectedUsers() {
        List<User> selected = new ArrayList<>();
        for (User u : userList) if (selectedIds.contains(u.userId)) selected.add(u);
        return selected;
    }
    static class UserViewHolder extends RecyclerView.ViewHolder {
        private final TextView usernameTextView;
        private final ImageView avatarImageView;
        private final CheckBox checkBox;
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameTextView = itemView.findViewById(R.id.user_name);
            avatarImageView = itemView.findViewById(R.id.user_avatar);
            checkBox = new CheckBox(itemView.getContext());
            ((ViewGroup) itemView).addView(checkBox);
        }
        public void bind(User user, boolean checked) {
            usernameTextView.setText(user.username);
            if (user.avatarBase64 != null && !user.avatarBase64.isEmpty()) {
                byte[] decodedBytes = Base64.decode(user.avatarBase64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                avatarImageView.setImageBitmap(bitmap);
            } else {
                avatarImageView.setImageResource(R.drawable.ic_default_avatar);
            }
            checkBox.setChecked(checked);
        }
    }
} 