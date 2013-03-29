package net.babyoilbonanza.ioio.remote.control;

import java.text.SimpleDateFormat;
import java.util.Date;

import net.babyoilbonanza.android.ioio.util.Debug;
import net.babyoilbonanza.android.ioio.util.DebugOutput;
import net.babyoilbonanza.android.ioio.util.TailLogThread;
import net.babyoilbonanza.ioio.remote.connect.TcpConnectClient;
import net.babyoilbonanza.ioio.remote.connect.TcpListenThread;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class RemoteControlActivity extends Activity implements SensorEventListener,  OnSharedPreferenceChangeListener, MyGuiUpdates, DebugOutput
{


    private static final double TILT_Y_THRESHOLD = 0.2;
    private static final String APP_VERSION      = "0,1beta";

    public static final String  PREFERENCE_MODE           = "preference_mode";
    public static final String  PREFERENCE_IOIO_PWMPIN    = "preference_ioio_pwmpin";
    public static final String  PREFERENCE_IOIO_FREQ      = "preference_ioio_freq";
    public static final String  PREFERENCE_USE_ACCEL      = "preference_use_accelerometer";
    public static final String  PREFERENCE_LOGLEVEL       = "preference_loglevel";
    public static final String  PREFERENCE_LOGCAT_LINES   = "preference_logcat_lines";
    public static final String  PREFERENCE_MOTOR_E1       = "preference_ioio_motorpin_e1";
    public static final String  PREFERENCE_MOTOR_I1       = "preference_ioio_motorpin_i1";
    public static final String  PREFERENCE_MOTOR_I2       = "preference_ioio_motorpin_i2";
    public static final String  PREFERENCE_MOTOR_E2       = "preference_ioio_motorpin_e2";
    public static final String  PREFERENCE_MOTOR_I3       = "preference_ioio_motorpin_i3";
    public static final String  PREFERENCE_MOTOR_I4       = "preference_ioio_motorpin_i4";
    public static final String  PREFERENCE_LISTENPORT     = "preference_listenport";
    public static final String  PREFERENCE_REMOTEHOST     = "preference_remote_host";
    public static final String  PREFERENCE_FLIP_DIRECTION = "preference_flip_direction";


    private EditText            ioio_info;
    private SeekBar             seekBar_;
    private SensorManager       sensorManager;
    private ToggleButton        connect_toggleBtn;
    private RadioGroup          radioGroupDir;
    private TextView            static_info;

    private SharedPreferences   sharedPrefs;

    IOIOController              ioioController;

    final RemoteControlActivity myGui                   = this;
    RemoteControlHandler        myHandler               = new RemoteControlHandler(myGui);

    private MenuItem            menu_connect_toggle;
    private TcpListenThread     connectionThread        = null;
    private TcpConnectClient    tcpConnectionClient     = null;

    
    
    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.remote_control);
        
        PreferenceManager.setDefaultValues(this, R.xml.ioio_remote_control_preference, false);
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        
        setTitle("Application Mode: "+sharedPrefs.getString(PREFERENCE_MODE, "[ERR: Not set]"));

        
        ioio_info           = (EditText)        findViewById(R.id.ioio_info);
        static_info         = (TextView)        findViewById(R.id.static_info);
        seekBar_            = (SeekBar)         findViewById(R.id.seekBar_);
        connect_toggleBtn   = (ToggleButton)    findViewById(R.id.connect_toggleBtn);
        radioGroupDir       = (RadioGroup)      findViewById(R.id.radioGroupDir);
        sensorManager       = (SensorManager)   getSystemService(SENSOR_SERVICE);
        

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_UI);
        
        seekBar_.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
            {
                setStaticInfoText();
                if (tcpConnectionClient != null) tcpConnectionClient.sendStringToSocket(CMD_YAW_PROGRESS+':'+progress);  
            }

            @Override public void onStopTrackingTouch(SeekBar seekBar)  { }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
        });
        
        radioGroupDir.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                if (tcpConnectionClient != null) tcpConnectionClient.sendStringToSocket(CMD_DIRECTION+':'+getDirection());
            }
        });
        
        
        ioio_info.setOnLongClickListener(new OnLongClickListener() {
            @Override public boolean onLongClick(View v)
            {
                ioio_info.setText("");
                Toast.makeText(myGui, "Cleared log text.", Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        
        
        connect_toggleBtn.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v)
            {
                if ( connect_toggleBtn.isChecked() ) {
                    ioioController = new IOIOController(myGui);  //XXX
                    ioioController.onStart();
                }
                else
                    ioioController.onStop();
            }
        });
        
        setStaticInfoText();
        showPinout();
        
        Debug.DEBUG_LEVEL =  Integer.parseInt(sharedPrefs.getString(PREFERENCE_LOGLEVEL, "3"));
        Debug.debugOut = myGui;
    }
    
    
    
    @Override public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.remote_control, menu);
        
        menu_connect_toggle = menu.findItem(R.id.menu_connect_toggle);
        setMenuConnectToggle();
        
        return true;
    }
    
    
    
    private void setMenuConnectToggle()
    {
        String prefMode = sharedPrefs.getString(PREFERENCE_MODE, "[ERR: Not set]");
        
        menu_connect_toggle.setEnabled(true);
        if ( prefMode.equals(getResources().getString(R.string.mode_listen_for_remote)) )
        {
            menu_connect_toggle.setIcon(R.drawable.antenna);
            menu_connect_toggle.setTitle(
                menu_connect_toggle.isChecked()?
                    R.string.mode_listening_stop : 
                    R.string.mode_listening_start
                );
        }
        else if ( prefMode.equals(getResources().getString(R.string.mode_connect_to_local)) )
        {
            menu_connect_toggle.setIcon(R.drawable.old_phone);
            menu_connect_toggle.setTitle(
                    menu_connect_toggle.isChecked()?
                        R.string.mode_connect_stop : 
                        R.string.mode_connect_start
                    );
        }
    //Local mode:
        else {
            menu_connect_toggle.setIcon(R.drawable.ic_menu_close_clear_cancel);
            menu_connect_toggle.setEnabled(false);
            menu_connect_toggle.setTitle("n/a");
        }
    }
    

    @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
    {
        Toast.makeText(this, "Changed Pref Key: "+key, Toast.LENGTH_SHORT).show();
        
        if ( key.equals(PREFERENCE_MODE) )
        {
            setTitle("Application Mode: "+sharedPrefs.getString(PREFERENCE_MODE, "[ERR: Not set]"));
            setMenuConnectToggle();
        }
        else if ( key.equals(PREFERENCE_LOGLEVEL) )
        {
            Debug.DEBUG_LEVEL =  Integer.parseInt(sharedPrefs.getString(PREFERENCE_LOGLEVEL, "3"));
        }
    }


    
    @Override protected void onDestroy() {
        if ( ioioController !=  null )
        {
            ioioController.onDestroy();
        }
        super.onDestroy();
    }

    
    
    @Override public boolean onOptionsItemSelected(MenuItem item)
    {
        switch ( item.getItemId() )
        {
            case R.id.menu_settings:
                startActivity(new Intent(this, RemoteCtrlPreferenceActivity.class));
                break;
            case R.id.menu_connect_toggle:
                processConnectivity();
                break;
            case R.id.menu_show_pinout:
                showPinout();
                break;
            case R.id.menu_dump_logcat:
                (new TailLogThread(
                        myHandler, RemoteControlHandler.NEW_LOGLINE, 
                        "*", 
                        Integer.parseInt(sharedPrefs.getString(PREFERENCE_LOGCAT_LINES, "30"))
                    )).start();
                break;
                
            case R.id.menu_about:
                aboutDlg();
                break;
        }
        return false;
    }
    
    


    private void processConnectivity()
    {
        String prefMode = sharedPrefs.getString(PREFERENCE_MODE, "[ERR: Not set]");
        
        if ( prefMode.equals(getResources().getString(R.string.mode_listen_for_remote)) )
        {
            if ( ! menu_connect_toggle.isChecked() ) {
                setInfoText("Starting to listen");
                connectionThread = new TcpListenThread(
                      myHandler, RemoteControlHandler.INBOUND_TCP_CMD, 
                      Integer.parseInt(sharedPrefs.getString(PREFERENCE_LISTENPORT, "[ERR: Not set]"))
                      ) ;
                new Thread(connectionThread).start();
            } else {
                setInfoText("Stopping to listen", APPEND_TEXT);
                connectionThread.closeConnection();
                connectionThread = null;
            }
        }
        else if ( prefMode.equals(getResources().getString(R.string.mode_connect_to_local)) )
        {
            if ( ! menu_connect_toggle.isChecked() ) {
                setInfoText("Starting to connect");
                tcpConnectionClient = new TcpConnectClient(
                      myHandler, RemoteControlHandler.OUTBOUND_TCP_CMD,
                      sharedPrefs.getString(PREFERENCE_REMOTEHOST, "[ERR: Not set]")
                      );
            } else {
                setInfoText("Stopping to connect", APPEND_TEXT);
                tcpConnectionClient.closeConnection();
                tcpConnectionClient = null;
            }
        }
        menu_connect_toggle.setChecked(!menu_connect_toggle.isChecked());

        setMenuConnectToggle();
        
    }





    private void aboutDlg()
    {
        String verString = "Version: "+APP_VERSION;
        
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(),0);
            verString += "/"+packageInfo.versionCode+"-" + packageInfo.versionName;
        }
        catch (NameNotFoundException e)  { Log.e("RBW>", e.getMessage()); e.printStackTrace(); }

        new AlertDialog.Builder(this)
            .setIcon(R.drawable.wile_e_coyote_error)
            .setTitle("About")
            .setMessage("(l) 2012 Richard Barnes-Webb\n"+verString+"\nAndroid API ver: "+android.os.Build.VERSION.SDK_INT)
            .setNeutralButton("Okay", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface dialog, int which)  { dialog.dismiss();  } })
            .show();
    }

    


    private void showPinout() {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        
        setInfoText("IOIO board Pin configuration> \n"+
                " *  Digital Pin1 (E1):"+sharedPrefs.getString(PREFERENCE_MOTOR_E1, "-1")+ "\n"+
                " *  Digital Pin2 (I1):"+sharedPrefs.getString(PREFERENCE_MOTOR_I1, "-1")+ "\n"+
                " *  Digital Pin7 (I2):"+sharedPrefs.getString(PREFERENCE_MOTOR_I2, "-1")+ "\n"+
                " *  Digital Pin9 (E2):"+sharedPrefs.getString(PREFERENCE_MOTOR_E2, "-1")+ "\n"+
                " *  Digital Pin10(I3):"+sharedPrefs.getString(PREFERENCE_MOTOR_I3, "-1")+ "\n"+
                " *  Digital Pin15(I4):"+sharedPrefs.getString(PREFERENCE_MOTOR_I4, "-1")+ "\n"+
                " *  PWM Contrl Pin:"+sharedPrefs.getString(PREFERENCE_IOIO_PWMPIN, "-1")+ "\n"+
                "\n"+
                "Wifi connected: " + connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnected() + "\n"+
                "Mobile connected: " + connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnected()
            );
    }

    
    public void setInfoText(final String str) {
        setInfoText(str, false);
    }
    
    public void setInfoText(final String str, final boolean append) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                if (append) 
                    ioio_info.append("\n"+str); 
                else  
                    ioio_info.setText(str);
            }
        });
    }
    


    private void setStaticInfoText()
    {
        static_info.setText("PWM Pulse Width: "+getPulseWidth()+ "µs");
    }



    private void setSeekBarProgressGAccel(final double accel)
    {
        int units = (seekBar_.getMax()) / 18; // -10 <-- 0 --> 10 --actually 9.81, but account for round-up.
        setSeekBarProgress((seekBar_.getMax() / 2) + (int) (units * accel));
    }



    public void setSeekBarProgress(final int progress)
    {
        runOnUiThread(new Runnable() {
            @Override public void run() { seekBar_.setProgress(progress); }
        });
    }
    


    
    float _last_accel_mag;
    @Override public void onSensorChanged(SensorEvent event)
    {
        if (!(sharedPrefs.getBoolean(PREFERENCE_USE_ACCEL, false)) ||
             (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) )
            return;
        
        float yaxis_accel = event.values[1];
        
        if( Math.abs(yaxis_accel) > (_last_accel_mag+TILT_Y_THRESHOLD) ) {
            setSeekBarProgressGAccel(yaxis_accel);
        }
        _last_accel_mag = Math.abs(yaxis_accel);
    }
    
    @Override public void onAccuracyChanged(Sensor sensor, int accuracy){}


    
  
    
    @SuppressLint("SimpleDateFormat")
    public static String getTS()
    {
        return (new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss" )).format(new Date());
    }


    @Override public int getPulseWidth()
    {
        int progress = sharedPrefs.getBoolean(PREFERENCE_FLIP_DIRECTION, false) ?
                    seekBar_.getMax() -  seekBar_.getProgress()  
                : 
                    seekBar_.getProgress();
        
        return 500 + progress * 2;     // min 1,000µs, mid-way 1,500µs, max 2,000µs

    }


    @Override public DIR getDirection()
    {
      //courtesy: http://stackoverflow.com/questions/6440259/how-to-get-the-selected-index-of-a-radiogroup-in-android:
        return DIR.values()[  
                radioGroupDir.indexOfChild(
                    findViewById(radioGroupDir.getCheckedRadioButtonId())
                )-1 //rem: togglebutton is in the group too
            ];
    }


    @Override public void appendLogTxt(String s)
    {
        setInfoText(s, APPEND_TEXT);   
    }


    @Override public void appendInfoText(String str)
    {
        appendLogTxt(str);
    }



    @Override public void setDirection(final DIR dir)
    {
        runOnUiThread(new Runnable() {
            @Override public void run()
            {
                //we've got that lurking togglebtn (hence +1)
                ((RadioButton)radioGroupDir.getChildAt(dir.ordinal()+1)).setChecked(true);
            }
        });

    }
    
    
    
}



/****** SCRAP SCRAP SCRAP



//connectionThread = new Thread(new TcpListenThread(
//      myHandler, RemoteControlHandler.INBOUND_TCP_CMD, 
//      Integer.parseInt(sharedPrefs.getString(PREFERENCE_LISTENPORT, "[ERR: Not set]"))
//      ) );
//setMenuProcessListen(
//      R.drawable.antenna, R.string.mode_listening_start, R.string.mode_listening_stop, 
//      connectionThread
//);

  
//  

private void setMenuProcessListen(int iconRef, int startStr, int stopStr, Thread thread)
{
  menu_connect_toggle.setIcon(iconRef);
  menu_connect_toggle.setTitle(menu_connect_toggle.isChecked()?startStr :stopStr );
  if ( ! menu_connect_toggle.isChecked() )
  {
      setInfoText("-- Starting ");
      thread.start();
      menu_connect_toggle.setChecked(true);
  }
  else
  {
      setInfoText("-- Closing listener", true);
      try
      {
          thread.join(10000);
      }
      catch (InterruptedException e)
      {
          Debug.bummer(e, myGui);
          e.printStackTrace();
      }
      menu_connect_toggle.setChecked(false);
      setInfoText("-- Closed listener", true);
  }
  
}

*****/