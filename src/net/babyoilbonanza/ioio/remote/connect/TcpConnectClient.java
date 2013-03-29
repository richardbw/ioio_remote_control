/**
 * 
 */
package net.babyoilbonanza.ioio.remote.connect;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;

import net.babyoilbonanza.android.ioio.util.Debug;
import android.os.Handler;
import android.util.Log;

/**
 * @author rbw
 *
 */
public class TcpConnectClient implements ClosableConnection
{

    private static final int DEFAULT_SERVER_PORT  = 8087;
    private static final int COMMAND_Q_MAX_LENGTH = 100;

    private boolean          isRunning;
    private ClientThread     client;



    public TcpConnectClient(Handler handler, int outboundTcpCmd, String connectionStr)
    {

        client = new ClientThread(connectionStr);

        (new Thread(client)).start();
    }
    

    
    public void sendStringToSocket(String string)
    {
        try
        {
            client.pushString(string);
        }
        catch (InterruptedException e)
        {
            Debug.logException(e);
            Log.e("ClientPush", "C: Error", e);
        }
    }


    public void closeConnection()
    {
        client.closeSocket();
    }
    
    
    //** http://thinkandroid.wordpress.com/2010/03/27/incorporating-socket-programming-into-your-applications/
    public class ClientThread implements Runnable
    {
        
        private String connectionStr;
        public Socket socket = null; 
        
        private ArrayBlockingQueue<String> cmdQ = new ArrayBlockingQueue<String>(COMMAND_Q_MAX_LENGTH); 

        public ClientThread(String connectionStr)
        {
            this.connectionStr = connectionStr;
        }


        public void pushString(String string) throws InterruptedException
        {
            cmdQ.put(string); 
        }


        public void run()
        {
            try
            {
                socket = new Socket(InetAddress.getByName(getHost(connectionStr)), getPort(connectionStr));
                Debug.logInfo("Connected... ["+socket.getInetAddress()+':'+socket.getPort()+"]");
                isRunning = true;
                String cmd;
                while ( isRunning )
                {
                    try
                    {
                        Debug.logTrace("C: Sending command.");
                        PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())),  true);
                        
                        cmd = cmdQ.poll();
                        if ( cmd == null ) continue;  //spin until a command becomes available
                        out.println(cmd);
                    }
                    catch (Exception e)
                    {
                        Debug.logException(e);
                        Log.e("ClientActivity", "S: Error", e);
                    }
                }
                socket.close();
                Debug.logDebug("Client: Closed.");
            }
            catch (Exception e)
            {
                Debug.logException(e);
                Log.e("ClientActivity", "C: Error", e);
                isRunning = false;
            }
        }
        
        
        public void closeSocket()
        {
            try
            {
                isRunning = false;
                if ( socket != null) socket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            
        }
        

        private String getHost(String connectionStr)
        {
            return ( connectionStr.indexOf(':') != -1 ) ?
                connectionStr.split(":")[0]
            :   connectionStr;
        }

        private int getPort(String connectionStr)
        {
            return ( connectionStr.indexOf(':') != -1 ) ?
                Integer.parseInt(connectionStr.split(":")[1])
            :   DEFAULT_SERVER_PORT;
        }
        
    }

    

}
