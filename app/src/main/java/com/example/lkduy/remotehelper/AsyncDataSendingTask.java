package com.example.lkduy.remotehelper;

import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by lkduy on 4/9/2017.
 */
public class AsyncDataSendingTask extends AsyncTask<Object, Void, Boolean> {
    static boolean readyToSend = true;
    Socket sendingSocket = null;
    public AsyncDataSendingTask(Socket s){
        sendingSocket = s;
    }
    @Override
    protected Boolean doInBackground(Object[] params) {
        if (readyToSend == false) {
            return true;
        }
        int msgCode = (int)params[0];
        readyToSend = false;
        try {
            sendingSocket.setKeepAlive(true);
            OutputStream os = sendingSocket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeInt(msgCode);
            switch (msgCode){
                case 9002:
                    //define token messages
                    //9002 = send pointing information
                    int pathID = (int)params[1];
                    float x = (float)params[2];
                    float y = (float)params[3];
                    int eventType = (int)params[4];
                    dos.writeUTF("+Pointing+");
                    dos.writeInt(pathID);
                    dos.writeFloat(x);
                    dos.writeFloat(y);
                    dos.writeInt(eventType);
                    //dos.writeUTF("-Pointing-");
                    dos.flush();
                    break;
                case 9003:
                    String handBitmapData = (String)params[1];
                    dos.writeUTF("+Hand+");
                    dos.writeInt(handBitmapData.getBytes().length);
                    dos.writeUTF("-Hand-");
                    dos.flush();
                    dos.writeBytes(handBitmapData);
                    dos.flush();
                    break;
            }
        }catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
    @Override
    protected void onPostExecute(Boolean result) {

        super.onPostExecute(result);
        readyToSend = true;
    }
}
