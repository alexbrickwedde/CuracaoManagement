package dev.brickwedde.curacaomanagement;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MyBluetoothService {
    private static final String TAG = "MY_APP_DEBUG_TAG";
    private static final UUID BT_SERIAL_DEVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private Handler handler;
    private ConnectedThread cthread;
    private String m_deviceName = "";

    public MyBluetoothService(Context context, Handler p_handler, String p_deviceName) {
        handler = p_handler;
        cthread = new ConnectedThread();
        cthread.start();
        m_deviceName = p_deviceName.toLowerCase();
    }

    public interface MessageConstants {
        public static final int MESSAGE_READ = 0;
        public static final int MESSAGE_WRITE = 1;
        public static final int MESSAGE_TOAST = 2;
        public static final int CONNECTED = 3;
        public static final int DISCONNECTED = 4;
        public static final int CONNECTFAILED = 5;
    }

    private BluetoothSocket mmSocket;
    private InputStream mmInStream;
    private OutputStream mmOutStream;
    private byte[] mmBuffer; // mmBuffer store for the stream

    private class ConnectedThread extends Thread {
        public void run() {
            try {
                BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                while(true) {
                    Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                    if (pairedDevices.size() > 0) {
                        for (BluetoothDevice device : pairedDevices) {
                            String deviceName = device.getName();
                            String deviceHardwareAddress = device.getAddress(); // MAC address
                            if (deviceName.toLowerCase().equals(m_deviceName)) {
                                mmSocket = device.createRfcommSocketToServiceRecord( BT_SERIAL_DEVICE_UUID);

                                try {
                                    mmSocket.connect();

                                    mmInStream= mmSocket.getInputStream();
                                    mmOutStream = mmSocket.getOutputStream();

                                    mmBuffer = new byte[1024];
                                    String sCmd = "";

                                    Message cnnctMsg = handler.obtainMessage(MessageConstants.CONNECTED, 0, -1,null);
                                    handler.sendMessage(cnnctMsg);

                                    while (true) {
                                        try {
                                            int len = mmInStream.read(mmBuffer);
                                            String sTemp = new String(mmBuffer, 0, len, Charset.forName("UTF-8"));
                                            sTemp = sTemp.replace('\n', '\r');

                                            sCmd += sTemp;

                                            int i;
                                            do {
                                                i = sCmd.indexOf('\r');
                                                if (i < 0) {
                                                    break;
                                                }
                                                String sCmdLine = sCmd.substring(0, i);
                                                sCmd = sCmd.substring(i + 2);

                                                Message readMsg = handler.obtainMessage(
                                                        MessageConstants.MESSAGE_READ, sCmdLine.length(), -1,
                                                        sCmdLine);
                                                handler.sendMessage(readMsg);
                                            } while (i >= 0);

                                        } catch (IOException e) {
                                            Log.d(TAG, "Input stream was disconnected", e);
                                            Message readMsg = handler.obtainMessage(MessageConstants.DISCONNECTED, 0, -1,null);
                                            handler.sendMessage(readMsg);
                                            break;
                                        }
                                    }

                                }catch( IOException e) {
                                    Log.e(TAG, "Connection failed: ", e);
                                    Message readMsg = handler.obtainMessage(MessageConstants.DISCONNECTED, 0, -1,null);
                                    handler.sendMessage(readMsg);
                                    break;
                                }
                                Message readMsg = handler.obtainMessage(MessageConstants.DISCONNECTED, 0, -1,null);
                                handler.sendMessage(readMsg);
                                break;
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
                Message readMsg = handler.obtainMessage(MessageConstants.CONNECTFAILED, 0, -1,null);
                handler.sendMessage(readMsg);
            }
        }
    }

    public void write(byte[] bytes) {
        try {
            mmOutStream.write(bytes);

            Message writtenMsg = handler.obtainMessage(
                    MessageConstants.MESSAGE_WRITE, -1, -1, mmBuffer);
            handler.sendMessage(writtenMsg);
        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);

            Message writeErrorMsg =
                    handler.obtainMessage(MessageConstants.MESSAGE_TOAST);
            Bundle bundle = new Bundle();
            bundle.putString("toast", "Couldn't send data to the other device");
            writeErrorMsg.setData(bundle);
            handler.sendMessage(writeErrorMsg);
        }
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
}