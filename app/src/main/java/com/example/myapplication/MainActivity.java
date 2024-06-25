package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.example.myapplication.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView txtInterfaces;
    private TextView txtPleaseWait;
    private Button btnGetIP;
    private ExecutorService executor;
    private boolean done;   // flag set when the HTTP POST and response are completed
    private boolean resultBool; // the "nat" result in the response

    // Used to load the 'myapplication' library on application startup.
    static {
        System.loadLibrary("myapplication");
    }

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        txtInterfaces = binding.txtInterfaces;
        btnGetIP = binding.ipAddressBtn;
        txtPleaseWait = binding.txtWait;

        txtPleaseWait.setVisibility(View.INVISIBLE);

        btnGetIP.setOnClickListener(this);
    }

    /**
     * A native method that is implemented by the 'myapplication' native library,
     * which is packaged with this application.
     */
    public native String[] getIPAdddresses();

    /**
     * @param iFace
     *      The interface information returned by getIPAdddresses()
     * @return
     *      The IP address for the given interface
     */
    private String parseIPAddress(String iFace)
    {
        String ip = new String();
        String[] strs = iFace.split("/");
        if (strs.length > 2) {
            ip = strs[2];
        }

        return ip;
    }

    /**
     * @param iFace
     *      The interface information returned by getIPAdddresses()
     * @return
     *      The interface type (4, or 6); otherwise 0
     */
    private int parseType(String iFace)
    {
        int type = 0;
        String[] strs = iFace.split("/");
        if (strs.length > 0) {
            if (strs[0].equals("v4")) {
                type = 4;
            } else if (strs[0] .equals("v6")) {
                type = 6;
            }
        }
        return type;
    }

    public void onClick(View v) {
        if (v == btnGetIP) {
            String v4 = new String();
            String v6 = new String();
            String txt = new String();

            // get the interfaces and show them in the TextView
            String[] addrs = getIPAdddresses();
            for (int i = 0; i < addrs.length; ++i) {
                txt += addrs[i] + "\n";
                int type = parseType(addrs[i]);
                String addr = parseIPAddress(addrs[i]);
                switch (type) {
                    case 4:
                        v4 = addr;
                        break;
                    case 6:
                        v6 = "2001:" + addr;
                        break;
                }
            }
            txtInterfaces.setText(txt);

            String theIP = !v6.isEmpty() ? v6 : v4;

            executor = Executors.newFixedThreadPool(1);

            // result alert dialog
            android.app.AlertDialog.Builder dlgAlert = new android.app.AlertDialog.Builder(this);
            dlgAlert.setTitle("Response");
            dlgAlert.setPositiveButton("OK", null);
            dlgAlert.setCancelable(true);

            done = false;

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject json = new JSONObject();
                        json.put("address", theIP);

                        Log.d("POST", "Sending JSON: " + json.toString());

                        URL obj = new URL("https://s7om3fdgbt7lcvqdnxitjmtiim0uczux.lambda-url.us-east-2.on.aws/");
                        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                        con.setRequestMethod("POST");
                        con.setRequestProperty("Content-Type", "application/json");
                        con.setDoOutput(true);

                        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                        wr.writeBytes(json.toString());
                        wr.flush();
                        wr.close();

                        int responseCode = con.getResponseCode();
                        Log.d("POST", "Response Code: " + responseCode);

                        if (responseCode == 200) {
                            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                            String inputLine;
                            StringBuilder response = new StringBuilder();

                            while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                            }
                            Log.d("RESP", response.toString());
                            in.close();

                            JSONObject respJson = new JSONObject(response.toString());
                            resultBool = respJson.getBoolean("nat");

                            if (resultBool) {
                                // TODO: show a red error icon
                                dlgAlert.setIcon(android.R.drawable.ic_dialog_alert);
                                dlgAlert.setMessage("Failed");
                            } else {
                                // TODO: show a green success
                                dlgAlert.setIcon(android.R.drawable.ic_dialog_info);
                                dlgAlert.setMessage(theIP);
                            }
                        } else {
                            dlgAlert.setIcon(android.R.drawable.ic_dialog_alert);
                            dlgAlert.setMessage("Error: " + responseCode);
                        }

                        done = true;
                    } catch (Exception ex) {
                        System.out.println(ex.toString());
                    }
                }
            });

            String err = "Timed out";
            try {
                for (int i = 0; i < 10 && !done; ++i) {
                    Thread.sleep(500);
                    if (i >= 6) {
                        txtPleaseWait.setVisibility(View.VISIBLE);
                    }
                }
            } catch(Exception ex){
                    err = executor.toString();
            }
            if (!done) {
                // TODO: show a red error icon
                dlgAlert.setIcon(android.R.drawable.ic_dialog_alert);
                dlgAlert.setMessage(err);
            }
            txtPleaseWait.setVisibility(View.INVISIBLE);
            dlgAlert.create().show();
        }
    }
}