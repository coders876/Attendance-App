package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Main2Activity extends AppCompatActivity {

    private Button button;

    // refrence to the database,name-roll-no and passcode
    private DatabaseReference ref = FirebaseDatabase.getInstance().getReference();
    private DatabaseReference refclass,name_roll_noref,passcoderef,present_studentref,imeinoref,bssidref,headcountref,illegalref,ssidref;
    private EditText editPasscode,editRoll_No,classno;
    private TextView error_message;
    private Button submit;
    private long headcount;
    private String realpasscode,tempIMEINumber,tempBSSIDno,user_name,bssid_no,imeino,realrollno,checkatten,tempssid,ssidno;
    private Map<String,Object > userUpdates = new HashMap<>();
    private Map<String,Object > imeiUpdates = new HashMap<>();
    private Intent returnIntent = new Intent();
    private List<String> list;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        //defining diffrent fields
        submit = (Button) findViewById(R.id.submit);
        editPasscode = (EditText) findViewById(R.id.editPasscode);
        editRoll_No = (EditText) findViewById(R.id.editRoll_No);
        classno = (EditText) findViewById(R.id.classno);
        error_message = (TextView) findViewById(R.id.error_message);

        //getting user_name from previous activity
        user_name = getIntent().getExtras().getString("user_name");

        //on clicking submit
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Boolean tt = check();

                if(check())
                {
                    userUpdates.put(user_name,realrollno);
                    present_studentref.updateChildren(userUpdates);
                    illegalref.child(user_name).removeValue();
                    present_studentref.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            headcount = dataSnapshot.getChildrenCount() - 1;
                            headcountref.setValue(Long.toString(headcount));
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {

                        }
                    });
                    returnIntent.putExtra("result","Thank You for attendance");
                    setResult(Activity.RESULT_OK,returnIntent);
                    finish();
                }
            }
        });


        //getting user name
        final String user_name =  getIntent().getExtras().getString("user_name");

        //getting permission for imei no
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);

        } else {
            getimeinumber();
        }

    }



    private boolean check(){
        String temprollno = editRoll_No.getText().toString();
        String temppasscode = editPasscode.getText().toString();
        String tempclassno = classno.getText().toString();

        if(!connect_wifi()) return false;
        if(!getimeinumber()) return false;

        refclass =  ref.child(tempclassno);
        name_roll_noref = refclass.child("NAMEROLLNO");
        imeinoref = refclass.child("IMEINUMBER");
        bssidref = refclass.child("BSSID_NO");
        passcoderef = refclass.child("PASSCODE");
        present_studentref = refclass.child("PRESENT_STUDENT");
        headcountref = refclass.child("HEADCOUNT");
        illegalref = refclass.child("ILLEGAL_ATTENDANCE");
        ssidref = refclass.child("SSID_NO");

        //getting data from databaseref
        ssidref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ssidno = dataSnapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error_message.setText("unable to connect server\n");
            }
        });

        passcoderef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot){
                realpasscode = dataSnapshot.getValue(String.class);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error_message.setText("unable to connect server\n");
            }
        });

        name_roll_noref.child(user_name).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                realrollno = dataSnapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error_message.setText("unable to connect server\n");
            }
        });

        imeinoref.child(user_name).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                imeino = dataSnapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error_message.setText("unable to connect server\n");
            }
        });

        bssidref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                bssid_no = dataSnapshot.getValue(String.class);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                error_message.setText("unable to connect server\n");
            }
        });

        if(realpasscode==null || realrollno==null || bssid_no==null || tempBSSIDno==null || tempIMEINumber==null || ssidno==null)
        {
            error_message.setText("please try again\n");
            return false;
        }

        if(!(temprollno.equals(realrollno))) {error_message.setText("roll no doesn't match");return false;}
        if(!temppasscode.equals(realpasscode)) {putting_proxy("pass_code doesn't match");return false;}

        int tempssidlen = tempssid.length();
        tempssid = tempssid.substring(1,tempssidlen-1);
        if(!ssidno.equals(tempssid)) {error_message.setText("please connect to correct wifi");return false;}

        //getting first of bssids
        String tempBSSIDno12 = tempBSSIDno.substring(0,11),bssid_no12 = bssid_no.substring(0,11);
        if(!tempBSSIDno12.equals(bssid_no12)) {putting_proxy("wifi doesn't match");return false;}

        //cheking imeino
        if(imeino == null)
        {
            imeinoref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Map<String, String> td = (HashMap<String,String>) dataSnapshot.getValue();
                    list = new ArrayList<String>(td.values());
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                }
            });
            if(list != null)
            {
                for(String imeitemp : list)
                {
                    if(imeitemp.equals("0")) continue;
                    else if(imeitemp.equals(tempIMEINumber)) {
                        putting_proxy("imei number matched with previous list");
                        return false;
                    }
                }
                imeiUpdates.put(user_name , tempIMEINumber);
                imeinoref.updateChildren(imeiUpdates);
                return true;
            }
            else
            {
                return false;
            }
        }
        else
        {
            if(!tempIMEINumber.equals(imeino)) {putting_proxy("imei number does't match");return false;}
        }
        return  true;
    }

    private boolean connect_wifi(){

        WifiManager wifiManager = (WifiManager)this.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo;
        wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            tempBSSIDno = wifiInfo.getBSSID();
            tempssid = wifiInfo.getSSID();
            if(tempBSSIDno==null) return false;
            else return true;
        }
        else
        {
            error_message.append("Not able to connect\n");
            return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,int[] grantResults)
    {
        // Check which request we're responding to
        if (requestCode == 1)
        {
            if (grantResults.length>0&&grantResults[0]== PackageManager.PERMISSION_GRANTED)
            {
                getimeinumber();
            }
        }
    }

    private boolean getimeinumber() {
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        tempIMEINumber = tm.getDeviceId(0);
        if(tempIMEINumber==null)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    private void putting_proxy(String S){
        userUpdates.put(user_name,S);
        illegalref.updateChildren(userUpdates);
        error_message.setText("don't put proxy\n");
    }
}



