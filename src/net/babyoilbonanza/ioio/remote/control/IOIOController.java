/**
 * 
 */
package net.babyoilbonanza.ioio.remote.control;

import static net.babyoilbonanza.ioio.remote.control.RemoteControlActivity.*;

import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.PwmOutput;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.IOIOLooperProvider;
import ioio.lib.util.android.IOIOAndroidApplicationHelper;
import net.babyoilbonanza.android.ioio.util.Debug;
import net.babyoilbonanza.ioio.remote.control.MyGuiUpdates.DIR;
import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;



/**
 * @author rbw
 *
 */
public class IOIOController implements IOIOLooperProvider
{
    
    private IOIOAndroidApplicationHelper ioioHelper;
    private MyGuiUpdates                 myGuiUpdate;
    private Activity                     myGui;


    private SharedPreferences sharedPrefs;

    
    
    public IOIOController(Activity activity)
    {
        myGui = activity;
        myGuiUpdate = (MyGuiUpdates)myGui ;
        
        ioioHelper = new IOIOAndroidApplicationHelper(activity, this);
        ioioHelper.create();  //not sure why this is needed.. @see 'Advanced Use-Cases'/https://github.com/ytai/ioio/wiki/IOIOLib-Basics
        
        
        
        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
        //sharedPrefs.registerOnSharedPreferenceChangeListener(this); TODO

        Debug.logTrace("ioioHelper.create() completed.");
    }
    
    
    

    @Override public IOIOLooper createIOIOLooper(String connectionType, Object extra)
    {
        return new Looper();
    }
    
    public class Looper implements IOIOLooper 
    {
        protected IOIO        ioio_;
        private DigitalOutput led_;
        private PwmOutput     pwmOutput_;
        private DigitalOutput motorE1Output_;
        private DigitalOutput motorI1Output_;
        private DigitalOutput motorI2Output_;
        private DigitalOutput motorE2Output_;
        private DigitalOutput motorI3Output_;
        private DigitalOutput motorI4Output_;

        private DigitalOutput pin16Output_;
        private DigitalOutput pin17Output_;
        private DigitalOutput pin18Output_;


        @Override public void setup(IOIO ioio) throws ConnectionLostException
        {
            Debug.logTrace("Entering Looper.setup()");
            ioio_ = ioio;
            
            try
            {
                Debug.logInfo("Vers:"+
                        "\nHardware       : "+ioio_.getImplVersion(IOIO.VersionType.HARDWARE_VER)     +
                        "\nApp firmware   : "+ioio_.getImplVersion(IOIO.VersionType.APP_FIRMWARE_VER) +
                        "\nIoiolib        : "+ioio_.getImplVersion(IOIO.VersionType.IOIOLIB_VER)      +
                        "\n");
                

                int pwmPin      = Integer.parseInt(sharedPrefs.getString(PREFERENCE_IOIO_PWMPIN, "-1"));      // show as -1
                int freqHz      = Integer.parseInt(sharedPrefs.getString(PREFERENCE_IOIO_FREQ, "-1"));        //   so that we can see it's not set.
                int pinNo_e1    = Integer.parseInt(sharedPrefs.getString(PREFERENCE_MOTOR_E1, "-1"));        //  
                int pinNo_i1    = Integer.parseInt(sharedPrefs.getString(PREFERENCE_MOTOR_I1, "-1"));        //
                int pinNo_i2    = Integer.parseInt(sharedPrefs.getString(PREFERENCE_MOTOR_I2, "-1"));        //
                int pinNo_e2    = Integer.parseInt(sharedPrefs.getString(PREFERENCE_MOTOR_E2, "-1"));        //  
                int pinNo_i3    = Integer.parseInt(sharedPrefs.getString(PREFERENCE_MOTOR_I3, "-1"));        //
                int pinNo_i4    = Integer.parseInt(sharedPrefs.getString(PREFERENCE_MOTOR_I4, "-1"));        //
                
                Debug.logDebug("Opening pins:"+
                    "\nPWM Pin: " +pwmPin       +
                    "\nE1 Pin: "  +pinNo_e1     +
                    "\nI1 Pin: "  +pinNo_i1     +
                    "\nI2 Pin: "  +pinNo_i2     +
                    "\nE2 Pin: "  +pinNo_e2     +
                    "\nI3 Pin: "  +pinNo_i3     +
                    "\nI4 Pin: "  +pinNo_i4     +
                    "\n");
                
                led_            = ioio_.openDigitalOutput(IOIO.LED_PIN, true);
                motorE1Output_  = ioio_.openDigitalOutput(pinNo_e1, true);
                motorI1Output_  = ioio_.openDigitalOutput(pinNo_i1, true);
                motorI2Output_  = ioio_.openDigitalOutput(pinNo_i2, true);
                motorE2Output_  = ioio_.openDigitalOutput(pinNo_e2, true);
                motorI3Output_  = ioio_.openDigitalOutput(pinNo_i3, true);
                motorI4Output_  = ioio_.openDigitalOutput(pinNo_i4, true);

                pin16Output_  = ioio_.openDigitalOutput(16, true);
                pin17Output_  = ioio_.openDigitalOutput(17, true);
                pin18Output_  = ioio_.openDigitalOutput(18, true);

                pwmOutput_      = ioio_.openPwmOutput( pwmPin, freqHz );
                
                //TODO enableUi(true);
            }
            catch (ConnectionLostException e)
            {
                Debug.logError("Error: "+e.getMessage());
                throw e;
            }
            catch (Exception e)
            {
                Debug.logError("Error: "+e.getMessage());
                Debug.bummer(e, myGui);
            }
            Debug.logTrace("/Exiting Looper.setup()");
        }

        @Override public void loop() throws ConnectionLostException
        {
            Debug.logTrace("Entering Looper.loop()");
            try
            {
                Debug.logTrace("-----> write to "+led_);
                led_.write(true);
                
                Debug.logTrace("-----> write to motor");
                writeMotorCmd(myGuiUpdate.getDirection());
                
                
                Debug.logTrace("-----> setPulseWidth"+myGuiUpdate.getPulseWidth());
                pwmOutput_.setPulseWidth( myGuiUpdate.getPulseWidth() );

                pin16Output_.write(true);
                pin17Output_.write(true);
                pin18Output_.write(true);
                
                Thread.sleep( sharedPrefs.getInt("looper_pause", 10) );
            }
            catch (InterruptedException e)
            {
                Debug.logError("InterruptedException: "+e.getMessage());
                ioio_.disconnect();
            }
            catch (ConnectionLostException e)
            {
                Debug.logError("ConnectionLostException: "+e.getMessage());
                //TODO enableUi(false);
                throw e;
            }
            catch (Exception e)
            {
                Debug.logException(e);
                //Debug.bummer(e, myGui);
            }
            Debug.logTrace("/Exiting Looper.loop()");
        }


        /*
            Pin 1 &   Pin2/I1 & Pin7/I2 &  Function
             Pin9      Pin10/I3  Pin15/I4
            High      High      Low       Turn Anti-clockwise (Reverse)
            High      Low       High      Turn clockwise (Forward)
            High      High      High      Stop
            High      Low       Low       Stop
         */
        private void writeMotorCmd(DIR direction) throws ConnectionLostException
        {
            motorE1Output_.write(true);//always write high... saves thinking
            motorE2Output_.write(true);//
            
            switch ( direction )
            {
                case FWD:
                    motorI1Output_.write(false);
                    motorI2Output_.write(true);
                    motorI3Output_.write(false);
                    motorI4Output_.write(true);
                    break;
                case STOP:
                    motorI1Output_.write(false);
                    motorI2Output_.write(false);
                    motorI3Output_.write(false);
                    motorI4Output_.write(false);
                    break;
                case BKWD:
                    motorI1Output_.write(true);
                    motorI2Output_.write(false);
                    motorI3Output_.write(true);
                    motorI4Output_.write(false);
                    break;
                default://STOP
                    motorI1Output_.write(false);
                    motorI2Output_.write(false);
                    motorI3Output_.write(false);
                    motorI4Output_.write(false);
                    break;
            }
        }

        @Override public void incompatible() {
            myGuiUpdate.setInfoText("Firmware is incompatible with IOIOLib");
        }
        
        @Override public void disconnected(){
            //myGuiUpdate.setInfoText("IOIO Disconnected.", APPEND_TEXT);
            Debug.logInfo("Looper.disconnected()");
        }

        
    }//Looper





    public void onStart()
    {
        ioioHelper.start(); 
        Debug.logInfo("ioioHelper.start()");
    }

    public void onStop()
    {
        Debug.logInfo("ioioHelper.stop()");
        ioioHelper.stop();   
    }
    
    public void onDestroy()
    {
        Debug.logInfo("ioioHelper.destroy()");
        ioioHelper.destroy();
        
    }
}




/*  Orig:
case FWD:
motorI1Output_.write(false);
motorI2Output_.write(true);
motorI3Output_.write(false);
motorI4Output_.write(true);

break;
case BKWD:
motorI1Output_.write(true);
motorI2Output_.write(false);
motorI3Output_.write(true);
motorI4Output_.write(false);
break;
case STOP:
motorI1Output_.write(true);
motorI2Output_.write(true);
motorI3Output_.write(true);
motorI4Output_.write(true);
break;


                    motorI1Output_.write(true);
                    motorI2Output_.write(false);
//                    motorI3Output_.write(false);
//                    motorI4Output_.write(true);
                    
                    break;
                case BKWD:
                    motorI1Output_.write(true);
                    motorI2Output_.write(false);
//                    motorI3Output_.write(true);
//                    motorI4Output_.write(false);
                    break;
                case STOP:
                    motorI1Output_.write(false);
                    motorI2Output_.write(false);
//                    motorI3Output_.write(true);
//                    motorI4Output_.write(true);

xxx
            motorE1Output_.write(true);//always write high... saves thinking
            motorE2Output_.write(true);//
            
            switch ( direction )
            {
                case FWD:
                    motorI1Output_.write(true);
                    motorI2Output_.write(true);
                    motorI3Output_.write(false);
                    motorI4Output_.write(true);
                    break;
                case STOP:
                    motorI1Output_.write(false);
                    motorI2Output_.write(false);
                    motorI3Output_.write(true);
                    motorI4Output_.write(true);
                    break;
                case BKWD:
                    motorI1Output_.write(true);
                    motorI2Output_.write(false);
                    motorI3Output_.write(true);
                    motorI4Output_.write(false);
                    break;
                default://STOP
                    motorI1Output_.write(false);
                    motorI2Output_.write(false);
                    motorI3Output_.write(false);
                    motorI4Output_.write(false);
                    break;
            }
xxx


*
*
*/


