/**
 * 
 */
package net.babyoilbonanza.ioio.remote.control;

import static net.babyoilbonanza.ioio.remote.control.MyGuiUpdates.CMD_DIRECTION;
import static net.babyoilbonanza.ioio.remote.control.MyGuiUpdates.CMD_YAW_PROGRESS;
import net.babyoilbonanza.android.ioio.util.Debug;
import net.babyoilbonanza.ioio.remote.control.MyGuiUpdates.DIR;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @author rbw
 *
 */
public class RemoteControlHandler extends Handler
{

    public static final int NEW_LOGLINE      = 0;
    public static final int INBOUND_TCP_CMD  = 1;
    public static final int OUTBOUND_TCP_CMD = 2;


    private MyGuiUpdates myGui;



    public RemoteControlHandler(MyGuiUpdates myGui)
    {
        this.myGui = myGui;
    }



    @Override public void handleMessage(Message msg)
    {
        switch ( msg.what )
        {
            case NEW_LOGLINE:
                Log.d(Debug.tag, "Appending logs..");
                myGui.appendInfoText(msg.obj + "");
            case INBOUND_TCP_CMD:
                Log.d(Debug.tag, "RemoteControlHandler received: "+msg.obj);
                handleCommand(""+msg.obj);
                break;
            default:
        }
    }

    

    private void handleCommand(String line) {
        //myGui.appendInfoText(line + "");
        
        if ( line.startsWith(CMD_YAW_PROGRESS) )
        {
            myGui.setSeekBarProgress(Integer.parseInt(line.split("\\s*[\\s:]\\s*")[1]));
        }
        else if ( line.startsWith(CMD_DIRECTION) )
        {
            String dirStr = line.split("\\s*[\\s:]\\s*")[1];
            for ( DIR d : DIR.values() )
                if(d.name().equals(dirStr)) myGui.setDirection(d);
        }
        else
        {
            Debug.logError("Command ["+line+"] malformed, or not recognised.");
        }
        
    }
    

}
