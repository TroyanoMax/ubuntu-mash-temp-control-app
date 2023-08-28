package com.ubuntu.mashtempapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.*;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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

//    private Button button_inc;
//    private Button button_dec;

    private ProgressBar progress_temp;

    // Este es el código que identifica la solicitud de permiso de Bluetooth
    private static final int BT_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TextView para mostar la temperatura
        temperatureTextView = findViewById(R.id.progress_temp_text);
        // Carga la fuente personalizada desde la carpeta assets/fonts
        Typeface customFont = Typeface.createFromAsset(getAssets(), "fonts/gothanthin.otf");

        // Aplica la fuente al TextView
        temperatureTextView.setTypeface(customFont);

        progress_temp =  findViewById(R.id.progress_temp);
        progress_temp.setMax(10000);

        progress_temp.setProgress(2536);
        temperatureTextView.setText("25.36°");

//        button_inc = findViewById(R.id.button_inc);
//        button_dec = findViewById(R.id.button_dec);
//
//        button_inc.setOnClickListener(v -> {
//            if(progr <= 100){
//                progr += 1;
//                updateProgressBar();
//            }
//        });
//
//        button_dec.setOnClickListener(v -> {
//            if(progr >= 0){
//                progr -= 1;
//                updateProgressBar();
//            }
//        });

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
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                android.Manifest.permission.BLUETOOTH_CONNECT
                        }, BT_PERMISSION_CODE);
            } else {
                // Tienes el permiso, puedes continuar con tus operaciones relacionadas con Bluetooth.

                // Código relacionado con Bluetooth aquí...

                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                bluetoothSocket.connect();
                inputStream = bluetoothSocket.getInputStream();

                temperatureTextView.setText(R.string.text_temp);

                // Inicia el hilo para recibir datos
                new Thread(new ReceiveDataThread()).start();

            }
        } catch (IOException | SecurityException e) {
            Log.e("Error: {}", e.getMessage());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();

        if(itemId == R.id.nav_item1){

            Toast.makeText(this, R.string.item_1, Toast.LENGTH_SHORT).show();

            return true;

        } else if (itemId == R.id.nav_item2){

            Toast.makeText(this, R.string.item_2, Toast.LENGTH_SHORT).show();

            return true;

        } else if (itemId == R.id.nav_item3){

            showAboutDialog("Versión 0.0.1-SNAPSHOT");
            Toast.makeText(this, R.string.item_3, Toast.LENGTH_SHORT).show();

            return true;

        } else {
            return super.onOptionsItemSelected(item);
        }

    }

    private void showAboutDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ubuntu Mash Control")
                .setMessage(message)
                .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Acción a realizar al hacer clic en el botón "Aceptar"
                        dialog.dismiss();
                    }
                })
                .setCancelable(false) // No se puede cancelar tocando fuera del diálogo
                .show();
    }

    private class ReceiveDataThread implements Runnable {
        @Override
        public void run() {

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            boolean continueReading = true;

            while (continueReading) {
                try {
                    String data = reader.readLine();
                    if (data == null) {
                        continueReading = false; // Termina el bucle cuando no hay más datos
                    } else {
                        // Actualiza el TextView en el hilo principal
                        handler.obtainMessage(0, data).sendToTarget();
                    }
                } catch (IOException e) {
                    Log.e("Error", e.getMessage());
                    break;
                }
            }

        }
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == 0) {

                String receivedData = (String) msg.obj;

                updateProgressBar(receivedData);

            }
        }

        @SuppressLint("SetTextI18n")
        private void updateProgressBar(String str){

            double progr = Double.parseDouble(str) * 100;
            int aux = (int) progr;

            progress_temp.setProgress(2536);
            temperatureTextView.setText("25.36");

        }

    };

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
            Log.e("Error {}", e.getMessage());
        }
    }

}
