package dev.brickwedde.curacaomanagement;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.util.HashMap;
import java.util.Map;

public class CcApi {
    private String endpoint;
    private String sessionKey;
    private RequestQueue queue;

    CcApi(String endpoint, Context context) {
        this.endpoint = endpoint;
        queue = Volley.newRequestQueue(context);
    }

    public void setSessionKey(String sessionkey) {
        this.sessionKey = sessionkey;
    }

    public static interface Callback {
        void then(JSONObject o) throws Exception;
        void catchy(Exception e);
    }

    void call(final Handler h, Callback cb, String function, Object ...args) {
        String url = this.endpoint + "/" + function;

        JSONArray a = new JSONArray();
        for(Object o : args) {
            a.put(o);
        }

        StringRequest stringRequest = new StringRequest(Request.Method.POST, url,
            new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        // Display the first 500 characters of the response string.
                        JSONObject o = new JSONObject(response);
                        h.postDelayed(new Runnable() {
                            public void run() {
                                try {
                                    cb.then(o);
                                } catch (Exception e) {
                                    cb.catchy(e);
                                }
                            }
                        }, 0);
                    } catch (Exception e) {
                        h.postDelayed(new Runnable() {
                            public void run() {
                                cb.catchy(e);
                            }
                        }, 0);
                    }
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    h.postDelayed(new Runnable() {
                        public void run() {
                            cb.catchy(error);
                        }
                    }, 0);
                }
            }) {
            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                return a.toString().getBytes();
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> h = new HashMap<>();
                if (sessionKey != null) {
                    h.put("Authorization", "Bearer " + sessionKey);
                }
                return h;
            }
        };
        stringRequest.setShouldCache(false);
        queue.add(stringRequest);
    }
}
