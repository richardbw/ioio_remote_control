/**
 * 
 */
package net.babyoilbonanza.ioio.remote.connect;

import static net.babyoilbonanza.ioio.remote.control.MyGuiUpdates.CMD_DIRECTION;
import static net.babyoilbonanza.ioio.remote.control.MyGuiUpdates.CMD_YAW_PROGRESS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;

import net.babyoilbonanza.android.ioio.util.Debug;
import net.babyoilbonanza.ioio.remote.control.MyGuiUpdates.DIR;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @author rbw
 * 
 * some code from http://thinkandroid.wordpress.com/2010/03/27/incorporating-socket-programming-into-your-applications/
 *
 */
@SuppressLint("DefaultLocale")
public class TcpListenThread implements Runnable, ClosableConnection
{
    private static final String          THREAD_NAME  = TcpListenThread.class.getSimpleName();

    private boolean                      isRunning    = true;

    private String                       tag          = THREAD_NAME;

    private InetAddress            ipAddress    = getIPAddress();
    public static int              serverPort   = 8086;
    private ServerSocket           serverSocket = null;

    private final Handler _handler;
    private final int _handler_what;


    public InetAddress getIpAddress()
    {
        return ipAddress;
    }
    public static int getPort()
    {
        return serverPort;
    }




    public TcpListenThread(Handler myHandler, int inboundTcpCmd, final int serverPort)
    {
        //super(THREAD_NAME);
        _handler = myHandler;
        _handler_what = inboundTcpCmd;
        TcpListenThread.serverPort = serverPort;
    }



    @Override
    public void run()
    {
        Debug.logInfo("Listening on: "+ipAddress.getHostAddress()+':'+serverPort);
        
        try
        {
            serverSocket = new ServerSocket(serverPort, 0, ipAddress);

            while ( isRunning )
            {
                // listen for incoming clients
                Socket server = serverSocket.accept();
                Debug.logInfo("***Connected***");

                BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));
                BufferedWriter out = new BufferedWriter(new  OutputStreamWriter(server.getOutputStream()) );
                String line = null;
                out.write("[type ? for help]\nCommand> ");
                out.flush();
                
                while ( (line = in.readLine()) != null )
                {
                    Debug.logTrace("ServerActivity:"+line);
                    if ( ! handleCommand(line.trim(), out) ) break;
                    out.write("Command> ");
                    out.flush();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Debug.logError("Error:"+e.getMessage());
        } 
        finally 
        {
            isRunning = false;
        }
        
        Debug.logInfo("/End listen thread.");        
    }
    
    
    
    
    

    private boolean handleCommand(String line, BufferedWriter out) throws IOException
    {
        if ( line.equals("?") || line.equals("help") )
        {
            out.write("HELP\n");
            out.write("----\n");
            out.write(CMD_YAW_PROGRESS+" [0-1000]      - set yaw (left/right)\n");
            out.write(CMD_DIRECTION+" ["+DIR.FWD+','+DIR.STOP+','+DIR.BKWD+"]  - set direction\n");
            out.write("close              - close socket connection\n\n");
        }
        else if ( line.startsWith(CMD_YAW_PROGRESS) || line.startsWith(CMD_DIRECTION) )
        {
            _handler.sendMessage(Message.obtain(_handler, _handler_what, line));
        }
        else if ( line.startsWith("close") )
        {
            out.write("\nGoodbye\n\n");
            out.flush();
            serverSocket.close();
            return false;
        }
        else
        {
            out.write("\nERROR: input malformed, or not recognised.\n");
        }
        
       return true;
        
    }
    public void closeConnection()
    {
        try
        {
            isRunning = false;
            if ( serverSocket != null) serverSocket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    
    
    
    

    /**
     * return first InetAddress interface, that's not loopback (lo)
     */
    private InetAddress getIPAddress() 
    {
        NetworkInterface intf;
        try
        {
            for ( Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); )
            {
                intf = en.nextElement();
                if ( !intf.isLoopback() )
                {
                    for ( Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); )
                    {
                        InetAddress inetAddr = enumIpAddr.nextElement();
                        if ( inetAddr instanceof Inet4Address ) { return inetAddr; } //we only want IPv4
                    }
                }
            }
        }
        catch (SocketException e)
        {
            Log.e(tag, "ERROR:"+e.getMessage());
            e.printStackTrace();
            Debug.logError(e.getMessage());
        }
        return null;
    }
    
    
    


}






