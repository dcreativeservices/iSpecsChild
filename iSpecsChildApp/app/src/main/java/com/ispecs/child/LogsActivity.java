package com.ispecs.child;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ispecs.child.helper.FileHelper;

import java.util.List;

public class LogsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecyclerViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);
        ActionBar actionBar = getSupportActionBar();

        // showing the back button in action bar
        actionBar.setDisplayHomeAsUpEnabled(true);

        actionBar.setTitle("All logs");


        recyclerView = findViewById(R.id.logs_recycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        List<String> textFileNames = FileHelper.getAllTextFileNames(getApplicationContext());

        adapter = new RecyclerViewAdapter(textFileNames);

        recyclerView.setAdapter(adapter);

        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class mViewHolder extends RecyclerView.ViewHolder{

        TextView itemTextView;

        public mViewHolder(@NonNull View itemView) {
            super(itemView);
            itemTextView = itemView.findViewById(R.id.itemTextView);
        }
    }

    private class RecyclerViewAdapter extends RecyclerView.Adapter<mViewHolder> {

        private List<String> items;

       public  RecyclerViewAdapter(List<String> items) {
            this.items = items;
        }
        @NonNull
        @Override
        public mViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.log_filename_cell, parent, false);
            return new mViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull mViewHolder holder, int position) {
            String filename = items.get(position);
            String fileDisplayName = filename.replace(".txt", "");
            holder.itemTextView.setText(fileDisplayName);

            holder.itemTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    Intent i = new Intent(LogsActivity.this, LogsDataActivity.class);
                    i.putExtra("filename" , filename);
                    startActivity(i);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }
}