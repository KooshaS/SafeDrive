package com.example.mindwavedrowsy;

import java.io.File;
import java.io.FileWriter;
import java.sql.Timestamp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.thinkgear.TGDevice;
import com.neurosky.thinkgear.TGEegPower;

public class EEGActivity extends Activity {
    
    private static final String TAG = "HelloEEG";

    BluetoothAdapter            bluetoothAdapter;
    TGDevice                    device;

    final boolean               rawEnabled = true;

    ScrollView                  sv;
    TextView                    tv;
    Button                      b;
    
    MarkovModel mm = new MarkovModel();

    java.util.Date date = new java.util.Date();
    long startTime = 0;
    long endTime = System.currentTimeMillis();
    long totalTime;
           
    @Override
    public void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        
        sv = (ScrollView)findViewById( R.id.scrollView1 );
        tv = (TextView)findViewById( R.id.textView1 );
        tv.setText( "" );
        tv.append( "Android version: " + Integer.valueOf(android.os.Build.VERSION.SDK) + "\n" );
        
        // Check if Bluetooth is available on the Android device
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if( bluetoothAdapter == null ) {            

        	// Alert user that Bluetooth is not available
        	Toast.makeText( this, "Bluetooth not available", Toast.LENGTH_LONG ).show();
        	//finish();
        	return;

        } else {
            
        	// Create the TGDevice 	
        	device = new TGDevice(bluetoothAdapter, handler);
        } 

        tv.append("NeuroSky: " + TGDevice.version + " " + TGDevice.build_title);
        tv.append("\n" );
   
    } 
	/* end onCreate() */
    

    
    public static boolean createDirIfNotExists(String path) {
        boolean ret = true;

        File file = new File(Environment.getExternalStorageDirectory(), path);
        if (!file.exists()) {
            if (!file.mkdirs()) {
                Log.e("TravellerLog :: ", "Problem creating Image folder");
                ret = false;
            }
        }
        return ret;
    }
     
    public void createFile() {
        FileWriter fWriter;
        try {
             
        	 String s1 = "/sdcard/HumaNet/"; 
        	 String s2 = String.valueOf(System.nanoTime());
        	 String s3 = ".txt";
        	 String s = s1 + s2 + s3;
        	 fWriter = new FileWriter(s);
        	
             fWriter.write(tv.getText().toString());
             fWriter.flush();
             fWriter.close();
         } catch(Exception e) {
                  e.printStackTrace();
         }
    }
    
    
    
  //Turn off app when touch return button of phone
    @Override
    public boolean onKeyDown(int keyCode,KeyEvent event)
    {
    	if(keyCode==KeyEvent.KEYCODE_BACK&&event.getRepeatCount()==0)
    	{
    		createDirIfNotExists("HumaNet");
    		createFile();
    		device.close();	
    		this.finish();
    		return true;
    	}
    	return super.onKeyDown(keyCode, event);
    }
    
    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        //if (!bluetoothAdapter.isEnabled()) {
          //  Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            //startActivityForResult(enableIntent, 1);
        //}
    }

    @Override
    public void onPause() {
    	// device.close();
        super.onPause();
    }
    
    @Override
    public void onStop() {
        device.close();
        super.onStop();
    }
    
    @Override
    public void onDestroy() {
    	//device.close();
        super.onDestroy();
    }
       
    /**
     * Handles messages from TGDevice
     */
    final Handler handler = new Handler() {  	  	
        @Override
        public void handleMessage( Message msg ) {

            switch( msg.what ) {
                case TGDevice.MSG_STATE_CHANGE:
    
                    switch( msg.arg1 ) {
    	                case TGDevice.STATE_IDLE:
    	                    break;
    	                case TGDevice.STATE_CONNECTING:       	
    	                	tv.append( "Connecting...\n" );
    	                	break;	
                        case TGDevice.STATE_CONNECTED:
                            tv.append( "Connected.\n" );
                            device.start();                                                     
                            break;
    	                case TGDevice.STATE_NOT_FOUND:
    	                	tv.append( "Could not connect any of the paired BT devices.  Turn them on and try again.\n" );
    	                	break;
                        case TGDevice.STATE_ERR_NO_DEVICE:
                            tv.append( "No Bluetooth devices paired.  Pair your device and try again.\n" );
                            break;
    	                case TGDevice.STATE_ERR_BT_OFF:
    	                    tv.append( "Bluetooth is off.  Turn on Bluetooth and try again." );
    	                    break;

    	                case TGDevice.STATE_DISCONNECTED:
    	                	tv.append( "Disconnected.\n" );
                    } /* end switch on msg.arg1 */

                    break;
                                       
                case TGDevice.MSG_EEG_POWER:
                	TGEegPower ep = (TGEegPower)msg.obj;
                	Log.v( "HelloEEG", "Alpha: " + ep.lowAlpha );
                	
                	double alpha = ep.lowAlpha + ep.highAlpha;
                	double beta = ep.lowBeta + ep.highBeta;
                	double theta = ep.theta;
                	double ba;
                	double atb;
                	
                	if ((alpha != 0) && (beta != 0)) {
                	               	
                	if (mm.ComputeProbability(alpha, beta, theta) == 0) {
                		               			
                			ba = (beta / alpha);
                    		atb = ((alpha + theta) / beta);
                    		
                    		endTime = System.currentTimeMillis();
                    		totalTime = endTime - startTime;
                			                		
                    if ((alpha < 50000) && (beta < 50000) && (theta < 50000)) {		
                    	tv.append(alpha + " , " + beta + " , " + theta + " , " + ba + " , " + atb + " , " + "AWAKE");
                		tv.append(" , latency: " + totalTime + " , time: " + new Timestamp(date.getTime()) + "\n");
                    }
                		
                			startTime = System.currentTimeMillis();
                		                		
                	} else if (mm.ComputeProbability(alpha, beta, theta) == 1){
                		                			
                			ba = (beta / alpha);
                    		atb = ((alpha + theta) / beta);
                    		
                    		endTime = System.currentTimeMillis();
                    		totalTime = endTime - startTime;
                    		                    		
                    if ((alpha < 50000) && (beta < 50000) && (theta < 50000)) {		
                		tv.append(alpha + " , " + beta + " , " + theta + " , " + ba + " , " + atb + " , " + "DROWSY");
                		tv.append(" , latency: " + totalTime + " , time: " + new Timestamp(date.getTime()) + "\n");
                    }
                		
            			startTime = System.currentTimeMillis();
            			                		
                	} else {
                		tv.append( "Processing...\n");	
                	}
                	}
                	
                	break;    
                                                
                default:
                	break;
                	
        	} /* end switch on msg.what */
            
        	sv.fullScroll( View.FOCUS_DOWN );
        	
        } /* end handleMessage() */
        
    }; /* end Handler */
    
    /**
     * This method is called when the user clicks on the "Connect" button.
     * 
     * @param view
     */
    public void doStuff(View view) {
    	if( device.getState() != TGDevice.STATE_CONNECTING && device.getState() != TGDevice.STATE_CONNECTED ) {
    	    
    		device.connect( rawEnabled );
    		   		
    	}
    	
    } /* end doStuff() */
    
} /* end HelloEEGActivity() */
