/**
 * 
 */
package net.babyoilbonanza.ioio.remote.control;

/**
 * @author rbw
 *  TODO  - can this be neater!?
 */
public interface MyGuiUpdates
{
    
    public static enum DIR {
        FWD, STOP, BKWD
    }

    boolean APPEND_TEXT      = true;
    String  CMD_YAW_PROGRESS = "yaw_progress";
    String  CMD_DIRECTION    = "direction";



    void setInfoText(final String str);
    void appendInfoText(final String str);
    
    int getPulseWidth();
    DIR getDirection();
    void setDirection(final DIR dir);
    void setSeekBarProgress(final int progress);

}
