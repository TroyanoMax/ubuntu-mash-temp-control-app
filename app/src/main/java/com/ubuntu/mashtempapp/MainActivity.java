package com.ubuntu.mashtempapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private TextView minNumberTextView;
    private TextView maxNumberTextView;
    private int currentMinValue = 70;
    private int currentMaxValue = 74;

    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private TextView temperatureTextView;
    private MediaPlayer mediaPlayer;
    private Ringtone ringtone;

    private ProgressBar progress_temp;

    private SwitchCompat betAlarmSwitch;
    private SwitchCompat uppAlarmSwitch;

    // Este es el código que identifica la solicitud de permiso de Bluetooth
    private static final int BT_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // No tienes el permiso, así que necesitas solicitarlo.
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_CONNECT
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

        // TextView para mostar la temperatura
        temperatureTextView = findViewById(R.id.progress_temp_text);
        // Carga la fuente personalizada desde la carpeta assets/fonts
        Typeface customFont = Typeface.createFromAsset(getAssets(), "fonts/gothanthin.otf");

        // textos del termometro muestra min y max temp
        TextView minTemp = findViewById(R.id.minTextView);
        TextView maxTemp = findViewById(R.id.maxTextView);

        // Aplica la fuente a los TextView
        temperatureTextView.setTypeface(customFont);
        minTemp.setTypeface(customFont);
        maxTemp.setTypeface(customFont);

        // set minimo y máximo
        progress_temp =  findViewById(R.id.progress_temp);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            progress_temp.setMin(-5500);
        }
        progress_temp.setMax(12500);

        // esto es temporario se setean valores fijos para el termometro
//        progress_temp.setProgress(7853);
//        temperatureTextView.setText("78.53°");

        // botones y texto configura temperatura minima
        minNumberTextView = findViewById(R.id.minTextNumber);
        Button decMinButton = findViewById(R.id.dec_min_button);
        Button incMinButton = findViewById(R.id.inc_min_button);

        decMinButton.setOnClickListener(v -> {
            if (currentMinValue > 0) {
                currentMinValue--;
                updateMinNumberTextView();
            }
        });

        incMinButton.setOnClickListener(v -> {
            currentMinValue++;
            updateMinNumberTextView();
        });

        // botones y texto configura temperatura minima
        maxNumberTextView = findViewById(R.id.maxTextNumber);
        Button decMaxButton = findViewById(R.id.dec_max_button);
        Button incMaxButton = findViewById(R.id.inc_max_button);

        decMaxButton.setOnClickListener(v -> {
            if (currentMaxValue > 0) {
                currentMaxValue--;
                updateMaxNumberTextView();
            }
        });

        incMaxButton.setOnClickListener(v -> {
            currentMaxValue++;
            updateMaxNumberTextView();
        });

        // Switch para alarmas
        betAlarmSwitch = findViewById(R.id.betAlarmSwitch);
        uppAlarmSwitch = findViewById(R.id.uppAlarmSwitch);

        // Configurar la alarma cuando el estado del Switch cambia
        betAlarmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                playAlarmSound(RingtoneManager.TYPE_NOTIFICATION );
            } else {
                cancelAlarmSound();
            }
        });

        // Configurar la alarma cuando el estado del Switch cambia
        uppAlarmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                playAlarmSound(RingtoneManager.TYPE_ALARM);
            } else {
                cancelAlarmSound();
            }
        });

    }

    private void updateMinNumberTextView() {
        String textWithDegree = currentMinValue + "°";
        minNumberTextView.setText(textWithDegree);
    }

    private void updateMaxNumberTextView() {
        String textWithDegree = currentMaxValue + "°";
        maxNumberTextView.setText(textWithDegree);
    }

    private void playAlarmSound(Integer type) {
        Toast.makeText(this, "Alarm ON", Toast.LENGTH_SHORT).show();
        Uri alarmUri = RingtoneManager.getDefaultUri(type);
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        }

        // Reproduce el sonido
        ringtone = RingtoneManager.getRingtone(this, alarmUri);
        ringtone.play();
    }

    private void cancelAlarmSound() {
        Toast.makeText(this, "Alarm Off", Toast.LENGTH_SHORT).show();

        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
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

                double progress = Double.parseDouble(receivedData);

                updateProgressBar((int) progress * 100);

                checkAlarm((int)progress);

            }
        }

        @SuppressLint("SetTextI18n")
        private void updateProgressBar(int value){

            progress_temp.setProgress(value);
            temperatureTextView.setText(value + "°");

        }

        private void checkAlarm(int value){

            if(value > currentMaxValue && uppAlarmSwitch.isChecked()){
                playAlarmSound(RingtoneManager.TYPE_ALARM );
            } else if (value >= currentMinValue && value < currentMaxValue && betAlarmSwitch.isChecked()){
                playAlarmSound(RingtoneManager.TYPE_NOTIFICATION);
            }

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
