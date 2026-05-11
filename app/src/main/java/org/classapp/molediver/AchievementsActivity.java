package org.classapp.molediver;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class AchievementsActivity extends AppCompatActivity {

    private static final String[] CATEGORIES = {
        "Depth", "Collection", "Coins", "Upgrades", "Survival", "Sets"
    };

    private TextView tvCompletion;
    private ListView listAchievements;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        tvCompletion    = findViewById(R.id.tvCompletion);
        listAchievements = findViewById(R.id.listAchievements);

        // Catch any achievements earned offline (shop purchases, collection milestones)
        AchievementManager.checkAndUnlockAll(this);

        refreshUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AchievementManager.checkAndUnlockAll(this);
        refreshUi();
    }

    private void refreshUi() {
        int unlocked = AchievementManager.getUnlockedCount(this);
        int total    = AchievementManager.ALL_ACHIEVEMENTS.length;
        int percent  = AchievementManager.getCompletionPercent(this);
        tvCompletion.setText(unlocked + " / " + total + " (" + percent + "%)");

        listAchievements.setAdapter(new AchievementAdapter(this));
    }

    // ── Helper: category accent colour ───────────────────────────────────────

    private static int categoryColor(String cat) {
        switch (cat) {
            case "Depth":      return Color.parseColor("#2196F3"); // blue
            case "Collection": return Color.parseColor("#9C27B0"); // purple
            case "Coins":      return Color.parseColor("#F5C542"); // amber
            case "Upgrades":   return Color.parseColor("#4CAF50"); // green
            case "Survival":   return Color.parseColor("#F44336"); // red
            case "Sets":       return Color.parseColor("#FFD700"); // gold
            default:           return Color.parseColor("#888888");
        }
    }

    // ── List model (flat mix of headers and achievement rows) ─────────────────

    private static final class ListEntry {
        final boolean isHeader;
        // header fields
        final String  headerText;
        final int     headerColor;
        // achievement fields
        final AchievementManager.Achievement achievement;
        final boolean unlocked;

        ListEntry(String header, int color) {
            isHeader    = true;
            headerText  = header;
            headerColor = color;
            achievement = null;
            unlocked    = false;
        }

        ListEntry(AchievementManager.Achievement a, boolean unlocked) {
            isHeader    = false;
            headerText  = null;
            headerColor = 0;
            achievement = a;
            this.unlocked = unlocked;
        }
    }

    private List<ListEntry> buildEntries() {
        List<ListEntry> entries = new ArrayList<>();
        for (String cat : CATEGORIES) {
            entries.add(new ListEntry(cat, categoryColor(cat)));
            for (AchievementManager.Achievement a : AchievementManager.ALL_ACHIEVEMENTS) {
                if (a.category.equals(cat)) {
                    entries.add(new ListEntry(a, PlayerData.isAchievementUnlocked(this, a.id)));
                }
            }
        }
        return entries;
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    private class AchievementAdapter extends BaseAdapter {

        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM   = 1;

        private final LayoutInflater inflater;
        private final List<ListEntry> entries;

        AchievementAdapter(Context ctx) {
            inflater = LayoutInflater.from(ctx);
            entries  = buildEntries();
        }

        @Override public int getCount()              { return entries.size(); }
        @Override public Object getItem(int pos)     { return entries.get(pos); }
        @Override public long getItemId(int pos)     { return pos; }
        @Override public int getViewTypeCount()      { return 2; }

        @Override
        public int getItemViewType(int position) {
            return entries.get(position).isHeader ? TYPE_HEADER : TYPE_ITEM;
        }

        @Override
        public boolean isEnabled(int position) { return false; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ListEntry entry = entries.get(position);

            if (entry.isHeader) {
                return buildHeaderView(entry, convertView, parent);
            } else {
                return buildItemView(entry, convertView, parent);
            }
        }

        private View buildHeaderView(ListEntry entry, View convertView, ViewGroup parent) {
            if (convertView == null || convertView.getTag() == null
                    || !(convertView.getTag() instanceof TextView)) {
                convertView = inflater.inflate(R.layout.item_achievement_header, parent, false);
            }
            TextView tv = convertView.findViewById(R.id.tvHeaderCategory);
            tv.setText(entry.headerText);
            tv.setTextColor(entry.headerColor);
            return convertView;
        }

        private View buildItemView(ListEntry entry, View convertView, ViewGroup parent) {
            ViewHolder vh;
            if (convertView == null || !(convertView.getTag() instanceof ViewHolder)) {
                convertView = inflater.inflate(R.layout.item_achievement, parent, false);
                vh = new ViewHolder(convertView);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }

            AchievementManager.Achievement a = entry.achievement;
            int catColor = categoryColor(a.category);

            if (entry.unlocked) {
                // Bright row
                convertView.setBackgroundColor(Color.parseColor("#0F1F0F"));

                // Trophy icon with green background circle
                vh.tvIcon.setText("★");
                vh.tvIcon.setTextColor(Color.parseColor("#4CAF50"));
                setCircleBg(vh.tvIcon, Color.parseColor("#1A3A1A"), Color.parseColor("#4CAF50"));

                vh.tvName.setTextColor(Color.WHITE);
                vh.tvDesc.setTextColor(Color.parseColor("#AAAAAA"));

                vh.tvCheck.setText("✓");
                vh.tvCheck.setTextColor(Color.parseColor("#4CAF50"));

            } else {
                // Dimmed row
                convertView.setBackgroundColor(Color.parseColor("#080F08"));

                // Lock icon with dark background circle
                vh.tvIcon.setText("●");
                vh.tvIcon.setTextColor(Color.parseColor("#444444"));
                setCircleBg(vh.tvIcon, Color.parseColor("#111111"), Color.parseColor("#2A2A2A"));

                vh.tvName.setTextColor(Color.parseColor("#555555"));
                vh.tvDesc.setTextColor(Color.parseColor("#333333"));

                vh.tvCheck.setText("");
                vh.tvCheck.setTextColor(Color.TRANSPARENT);
            }

            vh.tvName.setText(a.name);
            vh.tvDesc.setText(a.description);

            // Category badge
            vh.tvCategory.setText(a.category.toUpperCase());
            vh.tvCategory.setTextColor(entry.unlocked ? catColor : Color.parseColor("#333333"));

            return convertView;
        }

        private void setCircleBg(TextView tv, int fillColor, int strokeColor) {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(fillColor);
            circle.setStroke(2, strokeColor);
            tv.setBackground(circle);
        }
    }

    private static final class ViewHolder {
        final TextView tvIcon, tvName, tvCategory, tvDesc, tvCheck;

        ViewHolder(View v) {
            tvIcon     = v.findViewById(R.id.tvAchIcon);
            tvName     = v.findViewById(R.id.tvAchName);
            tvCategory = v.findViewById(R.id.tvAchCategory);
            tvDesc     = v.findViewById(R.id.tvAchDesc);
            tvCheck    = v.findViewById(R.id.tvAchCheck);
        }
    }
}
