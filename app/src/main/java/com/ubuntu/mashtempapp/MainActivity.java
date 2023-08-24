package com.ubuntu.mashtempapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private TextView temperatureTextView;

    // Este es el código que identifica la solicitud de permiso de Bluetooth
    private static final int BT_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        temperatureTextView = findViewById(R.id.temp);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // El dispositivo no admite Bluetooth
            return;
        }

        // Cambia la dirección MAC con la dirección de tu HC-05
        String hc05Address = "98:D3:31:30:99:35";
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(hc05Address);

        // UUID predeterminado para comunicación, serie (SPP) Bluetooth
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

        try {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // No tienes el permiso, así que necesitas solicitarlo.
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH_CONNECT}, BT_PERMISSION_CODE);
            } else {
                // Tienes el permiso, puedes continuar con tus operaciones relacionadas con Bluetooth.

                // Código relacionado con Bluetooth aquí...

                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                bluetoothSocket.connect();
                inputStream = bluetoothSocket.getInputStream();

                temperatureTextView.setText("texto nuevo");

                // Inicia el hilo para recibir datos
                new Thread(new ReceiveDataThread()).start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SecurityException se) {
            se.printStackTrace();
        }
    }

    private class ReceiveDataThread implements Runnable {
        @Override
        public void run() {
//            byte[] buffer = new byte[1024];
//            int bytes;

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            while (true) {
                try {
//                    bytes = inputStream.read(buffer);
//                    String data = new String(buffer, 0, bytes);

                    String data = reader.readLine();

                    // Actualiza el TextView en el hilo principal
                    handler.obtainMessage(0, data).sendToTarget();

                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        }
    }

    private final Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message msg) {
            if (msg.what == 0) {
                String receivedData = (String) msg.obj;
                temperatureTextView.setText(receivedData);
            }
            return true;
        }
    });

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
