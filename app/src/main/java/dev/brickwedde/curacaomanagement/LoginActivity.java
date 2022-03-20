package dev.brickwedde.curacaomanagement;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.android.volley.VolleyError;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

public class LoginActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        Button b = this.findViewById(R.id.login_loginbtn);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText host = findViewById(R.id.login_host);
                EditText username = findViewById(R.id.login_username);
                EditText password = findViewById(R.id.login_password);
                Handler h = new Handler();
                MainApplication.changeHost(getApplicationContext(), host.getText().toString());
                MainApplication.getApi().call(h, new CcApi.Callback() {
                    public void then(JSONObject o, JSONArray a) throws Exception {
                        Log.e("y", o.toString());
                        String sessionkey = o.getString("sessionkey");
                        MainApplication.getApi().setSessionKey(sessionkey, LoginActivity.this);

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                    public void catchy(Exception e, int status, String content) {
                        Log.e("y", "" + status + ":" + content);
                        Toast.makeText(LoginActivity.this, "Login failed " + status, Toast.LENGTH_LONG).show();
                    }
                }, "login", username.getText().toString(), password.getText().toString());
            }
        });
    }
}
