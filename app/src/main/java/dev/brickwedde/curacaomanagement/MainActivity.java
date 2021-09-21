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
import android.widget.ImageButton;
import android.widget.ImageView;
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
        ImageView taskTimetrackingButton;
        TextView taskName;

        JSONObject task;
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
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(R.layout.tasklist_row, parent, false);

            final TaskHolder holder = new TaskHolder();
            holder.task = taskList.get(position);
            holder.taskName = (TextView) row.findViewById(R.id.taskName);
            holder.taskTimetrackingButton = (ImageView) row.findViewById(R.id.taskTimetrackingButton);

            row.setTag(holder);
            try {
                holder.taskName.setText(holder.task.getString("taskname"));
                boolean bRunning = false;
                JSONObject time = null;
                JSONArray times = null;
                if (holder.task.has("times")) {
                    times = holder.task.getJSONArray("times");
                    for(int i = 0; i < times.length(); i++) {
                        time = times.getJSONObject(i);
                        Object stoptimeObj = time.has("stoptime") ? time.get("stoptime") : null;
                        if (stoptimeObj == null) {
                            bRunning = true;
                            break;
                        }
                        if (stoptimeObj instanceof Number && ((Number)stoptimeObj).equals(0)) {
                            bRunning = true;
                            break;
                        }
                    }
                }
                final boolean bRunningFinal = bRunning;
                final JSONObject timeFinal = time;
                final JSONArray timesFinal = times;
                if (bRunning) {
                    holder.taskTimetrackingButton.setImageResource(android.R.drawable.ic_media_pause);
                } else {
                    holder.taskTimetrackingButton.setImageResource(android.R.drawable.ic_media_play);
                }

                holder.taskTimetrackingButton.setClickable(true);
                holder.taskTimetrackingButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (bRunningFinal) {
                            try {
                                timeFinal.put("stoptime", System.currentTimeMillis());
                            } catch (JSONException e) {
                            }
                        } else {
                            try {
                                JSONObject newTime = new JSONObject();
                                newTime.put("starttime", System.currentTimeMillis());
                                if (timesFinal != null) {
                                    timesFinal.put(newTime);
                                } else {
                                    JSONArray newTimes = new JSONArray();
                                    newTimes.put(newTime);
                                    holder.task.put("times", newTimes);
                                }
                            } catch (JSONException e) {
                            }
                        }

                        updateTask(holder.task);
                    }
                });
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

    public void updateTask(JSONObject task) {
        Handler h = new Handler();
        MainApplication.getApi().call(h, new CcApi.Callback() {
            public void then(JSONObject o, JSONArray a) throws Exception {
                fetchTasks();
            }
            public void catchy(Exception e, int status, String content) {
                Log.e("y", "" + status + ":" + content);
                Toast.makeText(MainActivity.this, "Updating task failed " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
            }
        }, "updateObject", "customerprojecttask", task);
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