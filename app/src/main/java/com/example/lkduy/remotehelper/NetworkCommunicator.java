package com.example.lkduy.remotehelper;

import android.os.AsyncTask;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by lkduy on 4/8/2017.
 */
public class NetworkCommunicator {
    private String SERVER_IP = "129.16.213.47";
    private int PORT = 4043;
    private Socket comSocket;
    public Socket getSocket(){return comSocket;}
    private ConnectionEstablisedNotifier conEstablishedNotifier;
    public void setConnectionEstablishedNotifier(ConnectionEstablisedNotifier notifier){
        conEstablishedNotifier = notifier;
    }
    public NetworkCommunicator() {

    }
    public void Connect(){
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... unused) {
                // Background Code
                try {
                    comSocket = new Socket(SERVER_IP, PORT);

                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
                return true;
            }
            @Override
            protected void onPostExecute(Boolean result) {

                super.onPostExecute(result);
                if(conEstablishedNotifier != null){
                    conEstablishedNotifier.connectionEstablished(result);
                }
            }
        }.execute();
    }
}
interface ConnectionEstablisedNotifier{
    void connectionEstablished(boolean result);
}