package org.classapp.molediver;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CollectionActivity extends AppCompatActivity {

    private Button      btnTabItems, btnTabSets;
    private GridView    gridCollection;
    private ScrollView  scrollSets;
    private LinearLayout containerSets;
    private TextView    tvItemCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnTabItems    = findViewById(R.id.btnTabItems);
        btnTabSets     = findViewById(R.id.btnTabSets);
        gridCollection = findViewById(R.id.gridCollection);
        scrollSets     = findViewById(R.id.scrollSets);
        containerSets  = findViewById(R.id.containerSets);
        tvItemCount    = findViewById(R.id.tvItemCount);

        int collected = 0;
        for (ItemCatalogue.Item item : ItemCatalogue.ALL_ITEMS) {
            if (PlayerData.isItemCollected(this, item.id)) collected++;
        }
        if (collected == 50) {
            tvItemCount.setText("ALL ITEMS COLLECTED — GOLDEN SKIN UNLOCKED!");
            tvItemCount.setTextColor(Color.parseColor("#FFD700"));
        } else {
            tvItemCount.setText(collected + " / 50 COLLECTED");
        }

        gridCollection.setAdapter(new ItemGridAdapter());
        buildSetCards();

        btnTabItems.setOnClickListener(v -> showTab(true));
        btnTabSets.setOnClickListener(v -> showTab(false));

        showTab(true);
    }

    private void showTab(boolean items) {
        gridCollection.setVisibility(items ? View.VISIBLE : View.GONE);
        scrollSets.setVisibility(items ? View.GONE : View.VISIBLE);
        btnTabItems.setTextColor(items ? Color.parseColor("#4CAF50") : Color.parseColor("#888888"));
        btnTabSets.setTextColor(items ? Color.parseColor("#888888") : Color.parseColor("#4CAF50"));
    }

    // ─── Set cards ────────────────────────────────────────────────────────────

    private void buildSetCards() {
        LayoutInflater inflater = LayoutInflater.from(this);
        containerSets.removeAllViews();

        for (SetManager.SetDef set : SetManager.ALL_SETS) {
            View card = inflater.inflate(R.layout.set_card, containerSets, false);

            TextView     tvSetName     = card.findViewById(R.id.tvSetName);
            TextView     tvSetProgress = card.findViewById(R.id.tvSetProgress);
            TextView     tvSetDesc     = card.findViewById(R.id.tvSetDescription);
            LinearLayout containerItems= card.findViewById(R.id.containerItems);
            TextView     tvReward      = card.findViewById(R.id.tvReward);

            int     progress = SetManager.getSetProgress(this, set);
            int     total    = set.itemIds.length;
            boolean done     = progress == total;

            tvSetName.setText(done ? set.name + "  ✓" : set.name);
            tvSetProgress.setText(progress + " / " + total);
            tvSetDesc.setText(set.description);

            if (done) {
                tvReward.setTextColor(Color.parseColor("#F5C542"));
                tvReward.setText("REWARD: " + set.reward);
            } else {
                tvReward.setTextColor(Color.parseColor("#555555"));
                tvReward.setText("Reward: " + set.reward);
            }

            addItemChips(containerItems, set);
            containerSets.addView(card);
        }
    }

    private void addItemChips(LinearLayout container, SetManager.SetDef set) {
        container.removeAllViews();

        // Large sets (e.g. "Collector's Nightmare") — skip individual chips
        if (set.itemIds.length > 12) {
            TextView note = new TextView(this);
            note.setText("Collect all 50 items — see ITEMS tab");
            note.setTextColor(Color.parseColor("#888888"));
            note.setTextSize(9f);
            container.addView(note);
            return;
        }

        // Chips in rows of 4
        final int COLS = 4;
        LinearLayout row = null;
        for (int i = 0; i < set.itemIds.length; i++) {
            if (i % COLS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                );
                rowLp.bottomMargin = dp(4);
                row.setLayoutParams(rowLp);
                container.addView(row);
            }

            int id = set.itemIds[i];
            ItemCatalogue.Item item = ItemCatalogue.getItem(id);
            boolean collected = PlayerData.isItemCollected(this, id);

            TextView chip = new TextView(this);
            LinearLayout.LayoutParams chipLp = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            );
            chipLp.setMarginEnd(dp(3));
            chip.setLayoutParams(chipLp);
            chip.setText(collected ? item.name : "???");
            chip.setTextSize(7.5f);
            chip.setMaxLines(2);
            chip.setEllipsize(TextUtils.TruncateAt.END);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(3), dp(3), dp(3), dp(3));

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dp(3));
            if (collected) {
                int rc = ItemCatalogue.getBorderColor(item.rarity);
                bg.setColor(Color.argb(50, Color.red(rc), Color.green(rc), Color.blue(rc)));
                bg.setStroke(1, rc);
                chip.setTextColor(rc);
            } else {
                bg.setColor(Color.argb(50, 40, 40, 40));
                bg.setStroke(1, Color.parseColor("#333333"));
                chip.setTextColor(Color.parseColor("#444444"));
            }
            chip.setBackground(bg);

            if (row != null) row.addView(chip);
        }
    }

    private int dp(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    // ─── Grid adapter ─────────────────────────────────────────────────────────

    private class ItemGridAdapter extends BaseAdapter {

        private final LayoutInflater inflater = LayoutInflater.from(CollectionActivity.this);

        @Override public int getCount()              { return ItemCatalogue.ALL_ITEMS.length; }
        @Override public Object getItem(int pos)     { return ItemCatalogue.ALL_ITEMS[pos]; }
        @Override public long getItemId(int pos)     { return pos; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vh;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_cell, parent, false);
                vh = new ViewHolder(convertView);
                convertView.setTag(vh);
            } else {
                vh = (ViewHolder) convertView.getTag();
            }

            ItemCatalogue.Item item = ItemCatalogue.ALL_ITEMS[position];
            boolean collected = PlayerData.isItemCollected(CollectionActivity.this, item.id);

            // Root background = rarity border color (or dim gray if undiscovered)
            vh.cellRoot.setBackgroundColor(
                collected ? ItemCatalogue.getBorderColor(item.rarity)
                          : Color.parseColor("#333333")
            );

            vh.imgItem.setImageResource(item.spriteResId);

            if (collected) {
                vh.imgItem.clearColorFilter();
                vh.tvLock.setVisibility(View.GONE);
                vh.tvItemName.setText(item.name);
                vh.tvItemName.setTextColor(Color.parseColor("#E0E0E0"));
            } else {
                // Paint all sprite pixels near-black to create a silhouette
                vh.imgItem.setColorFilter(new PorterDuffColorFilter(
                    Color.argb(220, 20, 20, 20), PorterDuff.Mode.SRC_IN
                ));
                vh.tvLock.setVisibility(View.VISIBLE);
                vh.tvItemName.setText("???");
                vh.tvItemName.setTextColor(Color.parseColor("#444444"));
            }

            return convertView;
        }
    }

    private static final class ViewHolder {
        final LinearLayout cellRoot;
        final ImageView    imgItem;
        final TextView     tvLock;
        final TextView     tvItemName;

        ViewHolder(View v) {
            cellRoot   = (LinearLayout) v.findViewById(R.id.cellRoot);
            imgItem    = v.findViewById(R.id.imgItem);
            tvLock     = v.findViewById(R.id.tvLock);
            tvItemName = v.findViewById(R.id.tvItemName);
        }
    }
}
