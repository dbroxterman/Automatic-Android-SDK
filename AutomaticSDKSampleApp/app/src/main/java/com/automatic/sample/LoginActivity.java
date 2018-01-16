package com.automatic.sample;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableInt;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Toast;
import com.automatic.android.sdk.Automatic;
import com.automatic.android.sdk.AutomaticLoginCallbacks;
import com.automatic.android.sdk.ELMSocket;
import com.automatic.android.sdk.ObdCommands;
import com.automatic.android.sdk.SdkError;
import com.automatic.android.sdk.ServiceBindingCallback;
import com.automatic.android.sdk.events.AutomaticCoreAppQueryListener;
import com.automatic.net.events.BaseEvent;
import com.automatic.net.events.ConnectedAdapterInfo;
import com.automatic.sample.databinding.ActivityLoginBinding;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class LoginActivity extends AppCompatActivity {

    public static final String TAG = "LoginActivity";

    private EditText mElmCommand;
    private ELMSocket mElmSocket;
    private BluetoothSocket mBluetoothSocket;
    private CommandThread mCommandThread;
    public ArrayAdapter<String> arrayAdapter;
    private boolean mRpmState = false;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private final ObservableBoolean mElmConnected = new ObservableBoolean(false);
    private final ObservableBoolean mClientHandleSocketIo = new ObservableBoolean(false);
    private final ObservableInt mRealTimeRpm = new ObservableInt();

    private AutomaticLoginCallbacks mLoginCallbacks = new AutomaticLoginCallbacks() {
        @Override
        public void onLoginSuccess() {
            Log.d("AutomaticLoginCallbacks", "Got login success callback!");
            Toast.makeText(LoginActivity.this, "login success", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onLoginFailure(SdkError error) {
            Toast.makeText(LoginActivity.this, error.mMessage, Toast.LENGTH_LONG).show();
            Log.d("AutomaticLoginCallbacks", "Got login failure callback!");
        }
    };

    private ELMSocket.ElmSocketCallback mElmSocketCallback = new ELMSocket.ElmSocketCallback() {
        @Override
        public void onSocketAuthenticated(ELMSocket socket) {
            Log.v(TAG, "ELM Socket connected! " + socket.getSocket().getRemoteDevice().getAddress());
            mElmConnected.set(true);
            // elmSocket to communicate with Automatic adapter
            mElmSocket = socket;
            mElmSocket.setCommandCallback(mCommandCallback);
        }

        @Override
        public void onFailure(final SdkError error) {
            Log.e(TAG, error.toString());
            mElmConnected.set(false);
            mClientHandleSocketIo.set(false);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(LoginActivity.this, error.mMessage, Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onDisconnected(SdkError error) {
            Log.i(TAG, error.toString());
            mElmConnected.set(false);
            mClientHandleSocketIo.set(false);
        }
    };

    private ELMSocket.CommandCallback mCommandCallback = new ELMSocket.CommandCallback() {
        @Override
        public void onResponse(String response) {
            Log.i(TAG, "RX: " + response);
            addToList(response);

            if (mRpmState) {
                // first 4 letters are the echoed obd command
                String command = response.substring(0, 4);
                response = response.substring(4);

                if (ObdCommands.RPM.equalsIgnoreCase(command)) {
                    // parse the scaled (0-100) rpm out of the response
                    int rpm = parseRpm(response);
                    // set progress meter
                    mRealTimeRpm.set(rpm);
                    // send another request!
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendCommand(ObdCommands.RPM);
                        }
                    }, 100);
                }
            }
        }
    };

    private AutomaticCoreAppQueryListener mAutomaticCoreAppQueryListener = new AutomaticCoreAppQueryListener() {
        @Override
        public void onResponse(BaseEvent baseEvent) {
            if (baseEvent instanceof ConnectedAdapterInfo) {
                final ConnectedAdapterInfo connectedAdapterInfo = (ConnectedAdapterInfo) baseEvent;
                Log.d(TAG, connectedAdapterInfo.toString());
                if (connectedAdapterInfo.connected && (connectedAdapterInfo.mac != null) && !connectedAdapterInfo.mac.isEmpty()) {
                    // save adapter mac
                    getSharedPreferences("MAC_LAST", MODE_PRIVATE).edit().putString("LAST_MAC", connectedAdapterInfo.mac).apply();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(LoginActivity.this, String.format("Storing mac, %s", connectedAdapterInfo.mac), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityLoginBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_login);

        binding.setElmConnected(mElmConnected);
        binding.setClientHandleSocketIo(mClientHandleSocketIo);
        binding.setRealTimeRpm(mRealTimeRpm);

        mElmCommand = binding.elmCommand;
        mElmCommand.setFilters(new InputFilter[] { new InputFilter.AllCaps() });

        binding.elmStreamResponseList.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        binding.elmStreamResponseList.setAdapter(arrayAdapter);

        binding.sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mElmCommand.getText().length() > 0) {
                    sendCommand(mElmCommand.getText().toString());
                }
            }
        });

        binding.connectElmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // get last-connected MAC
                String mac = getSharedPreferences("MAC_LAST", MODE_PRIVATE).getString("LAST_MAC", null);
                if (mac != null) {
                    connectElmStreaming(mac);
                } else {
                    Toast.makeText(LoginActivity.this, "No stored MAC available.", Toast.LENGTH_LONG).show();
                }
            }
        });

        // press this button to get raw BT socket from a ElmSocket and handle io in the client
        binding.socketIoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mClientHandleSocketIo.get()) {
                    if (mElmSocket != null) {
                        mBluetoothSocket = mElmSocket.getSocket();
                        mClientHandleSocketIo.set(Automatic.get().transferSocketIoToClient());
                        if (mClientHandleSocketIo.get()) {
                            mCommandThread = new CommandThread(mBluetoothSocket);
                            mCommandThread.start();
                        }
                    }
                }
            }
        });

        // present a list of BT bonded devices
        binding.findMacButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findMacFromList();
            }
        });

        // ask core app for connected adapter info(e.g. mac address)
        binding.getAdapterMacButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Automatic.get().setAutomaticCoreAppQueryListener(mAutomaticCoreAppQueryListener);
                if (!Automatic.get().isServiceAuthenticated()) {
                    Automatic.get().bindService(new ServiceBindingCallback() {
                        @Override
                        public void onBindingResponse(boolean success, SdkError sdkError) {
                            Log.i(TAG, "onBindingResponse success, " + success);
                        }
                    });
                    Toast.makeText(LoginActivity.this, "Binding to Automatic Core app, try again later...", Toast.LENGTH_LONG).show();
                } else {
                    try {
                        Automatic.get().queryConnectedAdapterInfo();
                    } catch (RemoteException e) {
                        Log.e(TAG, "queryConnectedAdapterInfo error", e);
                    }
                }
            }
        });

        Automatic.get().addLoginButton(binding.automaticLoginButton, this, mLoginCallbacks);
    }

    private void connectElmStreaming(String btMacAddress) {
        if (!mElmConnected.get() && Automatic.get().isLoggedIn()) {
            Automatic.get().connectElmStreaming(btMacAddress, mElmSocketCallback);
        }
    }

    private void findMacFromList() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        final ArrayAdapter<String> btArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceBTName = device.getName();
                String deviceBTMac = device.getAddress();
                btArrayAdapter.add(deviceBTName + "\n" + deviceBTMac);
            }
        }

        new AlertDialog.Builder(this).setAdapter(btArrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String mac = btArrayAdapter.getItem(which).split("\n")[1];
                getSharedPreferences("MAC_LAST", MODE_PRIVATE).edit().putString("LAST_MAC", mac).apply();
                Toast.makeText(LoginActivity.this, String.format("Storing mac, %s", mac), Toast.LENGTH_LONG).show();
            }
        }).setTitle("Choose current Adapter:").show();
    }

    private void sendCommand(String command) {
        if (mElmSocket != null) {
            if (!mClientHandleSocketIo.get()) {
                mElmSocket.sendCommand(command);
            } else {
                mCommandThread.write(command + "\r");
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_trip_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        // Abort if not logged in
        if (!Automatic.get().isLoggedIn()) {
            Toast.makeText(this, "Must log in first.", Toast.LENGTH_LONG).show();
            return true;
        }

        int id = item.getItemId();
        switch (id) {
            case R.id.action_logout:
                Automatic.get().logout();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return true;
            case R.id.action_trips:
                startActivity(new Intent(this, TripViewerActivity.class));
                break;
            case R.id.action_service_binding:
                startActivity(new Intent(this, ServiceBindingActivity.class));
                break;
            case R.id.action_send_elm_command:
                sendCommand(ObdCommands.IGNITION_STATUS); // VIN
                sendCommand(ObdCommands.VIN); // ignition status
                break;
            case R.id.action_rpm_toggle:
                mRpmState = !mRpmState;
                sendCommand(ObdCommands.RPM);
                if (!mRpmState) {
                    mRealTimeRpm.set(0);
                }
                break;
            case R.id.action_disconnect_elm:
                mRpmState = false;
                Automatic.get().disconnectElm();
                break;
            default:
        }

        return super.onOptionsItemSelected(item);
    }

    private int parseRpm(String rpmResponse) {
        String[] rpmSplit = rpmResponse.split(" ");
        if (rpmSplit.length < 4) {
            return 0;
        }
        String hexRpm = rpmSplit[2] + rpmSplit[3];

        int bigRpm = (int) Long.parseLong(hexRpm, 16);
        int scaledRpm = (int) (bigRpm / 655.35);
        // Log.d(TAG, "Rpm:  hex: " + hexRpm + ", real: " + bigRpm + ", scaled: " + scaledRpm);
        return scaledRpm;
    }

    private void addToList(final String data) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (arrayAdapter != null) {
                    arrayAdapter.add(data);
                }
            }
        });
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions. Demos
     * how to handle raw BT socket in the client.
     */
    public class CommandThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public CommandThread(BluetoothSocket socket) {
            Log.d(TAG, "create CommandThread");
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            mmSocket = socket;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.d(TAG, "BEGIN mCommandThread");
            byte[] buffer = new byte[1024];
            int bytes;
            String stringBuffer = "";
            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    String readIn = new String(buffer, 0, bytes);
                    stringBuffer += readIn;
                    Log.d(TAG, "RX: " + readIn);

                    if (readIn.contains(">")) {
                        if (mRpmState) {
                            // parse the scaled (0-100) rpm out of the response
                            int rpm = parseRpm(stringBuffer.split("[\\r\\n]+")[1]);
                            // set progress meter
                            mRealTimeRpm.set(rpm);
                            addToList(readIn);
                            // reset stringBuffer
                            stringBuffer = "";
                            // send another request!
                            sendCommand(ObdCommands.RPM);
                        } else {
                            addToList(readIn);
                            stringBuffer = "";
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    Automatic.get().disconnectElm();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                // Log message
                Log.d(TAG, "TX: " + new String(buffer));
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void write(String data) {
            write(data.getBytes());
        }
    }
}
