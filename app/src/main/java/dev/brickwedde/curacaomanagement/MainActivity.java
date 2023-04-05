package dev.brickwedde.curacaomanagement;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    MyBluetoothService mbs = null;
    private ImageView connStateImage;
    private TextView connStateText;

    public void connectBt(String deviceName) {
        if (mbs != null) {
            mbs.cancel();
        }
        mbs = new MyBluetoothService(this, handler, deviceName);
    }

    private class HandlerCallback implements Handler.Callback {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case MyBluetoothService.MessageConstants.CONNECTFAILED:
                case MyBluetoothService.MessageConstants.DISCONNECTED:
                    mbs = null;
                    editDevicename.setVisibility(View.VISIBLE);
                    btnConnect.setVisibility(View.VISIBLE);
                    btnClear.setVisibility(View.GONE);
                    btnFrischStart.setVisibility(View.GONE);
                    btnFrischStop.setVisibility(View.GONE);
                    btnAbwasserStart.setVisibility(View.GONE);
                    btnAbwasserStop.setVisibility(View.GONE);
                    connStateText.setText("Not connected");
                    connStateImage.setImageResource(android.R.drawable.btn_star_big_off);
                    break;
                case MyBluetoothService.MessageConstants.CONNECTED:
                    switch(msg.arg1) {
                        case 0:
                            editDevicename.setVisibility(View.GONE);
                            btnConnect.setVisibility(View.GONE);
                            btnClear.setVisibility(View.VISIBLE);
                            btnFrischStart.setVisibility(View.VISIBLE);
                            btnFrischStop.setVisibility(View.VISIBLE);
                            btnAbwasserStart.setVisibility(View.VISIBLE);
                            btnAbwasserStop.setVisibility(View.VISIBLE);

                            connStateText.setText("Connected");
                            connStateImage.setImageResource(android.R.drawable.button_onoff_indicator_on);
                            break;
                        case 1:
                            connStateText.setText("Device found");
                            connStateImage.setImageResource(android.R.drawable.btn_star_big_on);
                            break;
                        case 2:
                            connStateText.setText("Connecting Device");
                            connStateImage.setImageResource(android.R.drawable.button_onoff_indicator_off);
                            break;
                        case 3:
                            connStateText.setText("Connected");
                            break;
                    }
                    break;
                case MyBluetoothService.MessageConstants.MESSAGE_READ:
                    String s = (String)msg.obj;
                    if (s.startsWith("z,")) {
                        String[] aS = s.split(",");
                        textFlashcounter.setText(aS[4]);
                        textConnectioncounter.setText(aS[1]);
                        float liter = Integer.parseInt(aS[3]);

                        SharedPreferences sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE);
                        float literfactor = sharedPref.getFloat("literfactor", 0.002f);

                        liter *= literfactor;
                        textLiter.setText("" + liter + " l");
                    }
                    if (s.startsWith("p,")) {
                        String[] aS = s.split(",");
                        int laufzeit = Integer.parseInt(aS[1]);
                        if (laufzeit > 60) {
                            textLaufzeit1.setText("" + (int)(laufzeit / 60) + ":" + (laufzeit % 60) + " m:s");
                        } else {
                            textLaufzeit1.setText("" + laufzeit + " s");
                        }

                        laufzeit = Integer.parseInt(aS[2]);
                        if (laufzeit > 60) {
                            textLaufzeit2.setText("" + (int)(laufzeit / 60) + ":" + (laufzeit % 60) + " m:s");
                        } else {
                            textLaufzeit2.setText("" + laufzeit + " s");
                        }
                    }
                    break;
                case MyBluetoothService.MessageConstants.MESSAGE_WRITE:
                    break;
                case MyBluetoothService.MessageConstants.MESSAGE_TOAST:
                    Toast.makeText(MainActivity.this, msg.getData().getString("toast"), Toast.LENGTH_SHORT).show();
                    break;
            }
            return false;
        }
    }

    EditText editDevicename = null;
    Button btnConnect = null;
    Button btnClear = null;
    Button btnFrischStart = null;
    Button btnFrischStop = null;
    Button btnAbwasserStart = null;
    Button btnAbwasserStop = null;
    TextView textFlashcounter = null;
    TextView textConnectioncounter = null;
    TextView textLiter = null;
    TextView textLaufzeit1 = null;
    TextView textLaufzeit2 = null;
    Handler handler = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler(new HandlerCallback());

        setContentView(R.layout.activity_main);

        editDevicename = findViewById(R.id.editDevicename);
        textFlashcounter = findViewById(R.id.flashcounter);
        textConnectioncounter = findViewById(R.id.connectioncounter);
        textLiter = findViewById(R.id.liter);
        textLaufzeit1 = findViewById(R.id.laufzeit1);
        textLaufzeit2 = findViewById(R.id.laufzeit2);

        btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                String deviceName = editDevicename.getText().toString();
                connectBt(deviceName);
                SharedPreferences sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE);
                sharedPref.edit().putString("devicename", deviceName).commit();
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                String deviceName = editDevicename.getText().toString();
                connectBt(deviceName);
            }
        }, 1000);

        btnClear = findViewById(R.id.btnClear);
        btnClear.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mbs != null) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Zurücksetzen???")
                            .setMessage("Wasserzähler zurücksetzen?")
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    mbs.write("r\r".getBytes());
                                }})
                            .setNegativeButton(android.R.string.no, null)
                            .show();
                }
            }
        });

        btnFrischStart = findViewById(R.id.btnFrischStart);
        btnFrischStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mbs != null) {
                    mbs.write("k1\r".getBytes());
                }
            }
        });

        btnFrischStop = findViewById(R.id.btnFrischStop);
        btnFrischStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mbs != null) {
                    mbs.write("k2\r".getBytes());
                }
            }
        });

        btnAbwasserStart = findViewById(R.id.btnAbwasserStart);
        btnAbwasserStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mbs != null) {
                    mbs.write("k3\r".getBytes());
                }
            }
        });

        connStateText = (TextView)findViewById(R.id.connState);
        connStateImage = (ImageView)findViewById(R.id.connStateImg);

        btnAbwasserStop = findViewById(R.id.btnAbwasserStop);
        btnAbwasserStop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mbs != null) {
                    mbs.write("k4\r".getBytes());
                }
            }
        });

        SharedPreferences sharedPref = getSharedPreferences("settings", Context.MODE_PRIVATE);
        editDevicename.setText(sharedPref.getString("devicename", "wasserzaehler"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mbs != null) {
            mbs.cancel();
            mbs = null;
        }
        this.finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true;
    }
}