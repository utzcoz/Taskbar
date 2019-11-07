/* Copyright 2016 Braden Farmer
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.farmerbb.taskbar.activity;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.farmerbb.taskbar.R;
import com.farmerbb.taskbar.util.IconPack;
import com.farmerbb.taskbar.util.IconPackManager;
import com.farmerbb.taskbar.util.U;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IconPackActivity extends AppCompatActivity {

    private AppListGenerator appListGenerator;
    private ProgressBar progressBar;
    private RecyclerView appList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.tb_select_app);
        setFinishOnTouchOutside(false);
        setTitle(R.string.tb_icon_pack);

        progressBar = findViewById(R.id.progress_bar);
        appList = findViewById(R.id.list);

        appListGenerator = new AppListGenerator();
        appListGenerator.execute();
    }

    @Override
    public void finish() {
        if(appListGenerator != null && appListGenerator.getStatus() == AsyncTask.Status.RUNNING)
            appListGenerator.cancel(true);

        super.finish();
    }

    private void openIconPackActivity(String packageName) {
        Intent intent = new Intent("org.adw.launcher.THEMES");
        intent.setPackage(packageName);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) { /* Gracefully fail */ }
    }

    private class AppListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<IconPack> entries = new ArrayList<>();

        AppListAdapter(List<IconPack> list) {
            entries.addAll(list);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            View view = LayoutInflater.from(IconPackActivity.this).inflate(R.layout.tb_desktop_icon_row, viewGroup, false);
            return new RecyclerView.ViewHolder(view) {};
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {
            final IconPack entry = entries.get(i);
            assert entry != null;

            TextView textView = viewHolder.itemView.findViewById(R.id.name);
            textView.setText(entry.getName());

            PackageManager pm = getPackageManager();
            ImageView imageView = viewHolder.itemView.findViewById(R.id.icon);

            if(entry.getPackageName().equals(getPackageName())) {
                imageView.setImageDrawable(null);
            } else {
                try {
                    imageView.setImageDrawable(pm.getApplicationIcon(entry.getPackageName()));
                } catch (PackageManager.NameNotFoundException e) {
                    imageView.setImageDrawable(pm.getDefaultActivityIcon());
                }
            }

            LinearLayout layout = viewHolder.itemView.findViewById(R.id.entry);
            layout.setOnClickListener(view -> {
                SharedPreferences pref = U.getSharedPreferences(IconPackActivity.this);
                pref.edit().putString("icon_pack", entry.getPackageName()).apply();
                setResult(RESULT_OK);
                finish();
            });

            layout.setOnLongClickListener(v -> {
                openIconPackActivity(entry.getPackageName());
                return false;
            });

            layout.setOnGenericMotionListener((view, motionEvent) -> {
                if(motionEvent.getAction() == MotionEvent.ACTION_BUTTON_PRESS
                        && motionEvent.getButtonState() == MotionEvent.BUTTON_SECONDARY) {
                    openIconPackActivity(entry.getPackageName());
                }
                return false;
            });
        }

        @Override
        public int getItemCount() {
            return entries.size();
        }
    }

    private final class AppListGenerator extends AsyncTask<Void, Void, AppListAdapter> {
        @Override
        protected AppListAdapter doInBackground(Void... params) {
            List<IconPack> list = IconPackManager.getInstance().getAvailableIconPacks(IconPackActivity.this);
            if(list.isEmpty())
                return null;
            else {
                List<IconPack> finalList = new ArrayList<>();
                IconPack dummyIconPack = new IconPack();
                dummyIconPack.setPackageName(getPackageName());
                dummyIconPack.setName(getString(R.string.tb_icon_pack_none));

                Collections.sort(list, (ip1, ip2) -> Collator.getInstance().compare(ip1.getName(), ip2.getName()));

                finalList.add(dummyIconPack);
                finalList.addAll(list);

                return new AppListAdapter(finalList);
            }
        }

        @Override
        protected void onPostExecute(AppListAdapter adapter) {
            if(adapter == null) {
                U.showToast(IconPackActivity.this, R.string.tb_no_icon_packs_installed);
                setResult(RESULT_CANCELED);
                finish();
            } else {
                progressBar.setVisibility(View.GONE);
                appList.setHasFixedSize(true);
                appList.setLayoutManager(new LinearLayoutManager(IconPackActivity.this));
                appList.setAdapter(adapter);
                setFinishOnTouchOutside(true);
            }
        }
    }
}