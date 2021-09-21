package dev.brickwedde.curacaomanagement;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private TaskListAdapter taskListAdapter;

    public static class TaskHolder {
        JSONObject task;
        TextView taskName;
    }

    private class TaskListAdapter extends BaseAdapter {
        private Context context;
        private List<JSONObject> taskList;

        public TaskListAdapter(Context context, List<JSONObject> taskList) {
            this.context = context;
            this.taskList = taskList;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            TaskHolder holder = null;
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(R.layout.tasklist_row, parent, false);

            holder = new TaskHolder();
            holder.task = taskList.get(position);
            holder.taskName = (TextView) row.findViewById(R.id.taskName);

            row.setTag(holder);
            try {
                holder.taskName.setText(holder.task.getString("taskname"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if (position % 2 == 0) {
                row.setBackgroundColor(Color.rgb(213, 229, 241));
            } else {
                row.setBackgroundColor(Color.rgb(255, 255, 255));
            }

            return row;
        }

        @Override
        public int getCount() {
            return taskList.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }
    }

    MyBluetoothService mbs = null;
    public void connectBt() {
        if (mbs == null) {
            mbs = new MyBluetoothService(this, new Handler());
        } else {
            Toast.makeText(this,"Already connecting/ed", Toast.LENGTH_LONG).show();
        }
    }

    private class MyHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MyBluetoothService.MessageConstants.CONNECTFAILED:
                case MyBluetoothService.MessageConstants.DISCONNECTED:
                    mbs = null;
                    break;
                case MyBluetoothService.MessageConstants.CONNECTED:
                    mbs.write("z\r".getBytes());
                    break;
            }
        }
    }

    MyHandler mHandler = new MyHandler();

    public static List<JSONObject> TASK_LIST = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mbs = new MyBluetoothService(MainActivity.this, mHandler);
            }
        });
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home)
                .setDrawerLayout(drawer)
                .build();
/*
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
*/
        for(int i=0;i<=10;i++) {
            try {
                JSONObject task = new JSONObject("{\"taskname\":\"Test" + i + "\"}");
                TASK_LIST.add(task);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        taskListAdapter = new TaskListAdapter(this, TASK_LIST);
        ListView listView;
        listView = (ListView) findViewById(R.id.tasklist);
        listView.setAdapter(taskListAdapter);

        if (!MainApplication.getApi().hasSessionKey(this)) {
            CcApi.gotoLogin(this);
        } else {
            Handler h = new Handler();
            MainApplication.getApi().call(h, new CcApi.Callback() {
                public void then(JSONObject o, JSONArray a) throws Exception {
                    fetchTasks();
                }
                public void catchy(Exception e, int status, String content) {
                    Log.e("y", "" + status + ":" + content);
                    Toast.makeText(MainActivity.this, "Checksession failed " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                }
            }, "checksessionkey");
        }
    }

    public void fetchTasks() {
        Handler h = new Handler();
        MainApplication.getApi().call(h, new CcApi.Callback() {
            public void then(JSONObject o, JSONArray a) throws Exception {
                TASK_LIST.clear();
                for(int i = 0; i < a.length(); i++) {
                    JSONObject task = (JSONObject) a.get(i);
                    TASK_LIST.add(task);
                }
                taskListAdapter.notifyDataSetChanged();
            }
            public void catchy(Exception e, int status, String content) {
                Log.e("y", "" + status + ":" + content);
                Toast.makeText(MainActivity.this, "Fetching tasks failed " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }, "listObjects", "customerprojecttask");
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

/*
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
 */
}