package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class InformacoesDaPartidaActivity extends Activity implements View.OnClickListener {

    public static final int PERMISSION_REQUEST_CODE = 123;

    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    EditText edtJogadorPretas;
    EditText edtJogadorBrancas;
    EditText edtkomi;
    Button btnDetectarTabuleiro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_informacoes_da_partida);

        edtJogadorPretas = (EditText) findViewById(R.id.edtJogadorPretas);
        edtJogadorBrancas = (EditText) findViewById(R.id.edtJogadorBrancas);
        edtkomi = (EditText) findViewById(R.id.edtKomi);
        btnDetectarTabuleiro = (Button) findViewById(R.id.btnDetectarTabuleiro);
        btnDetectarTabuleiro.setOnClickListener(this);

        edtJogadorPretas.setText("");
        edtJogadorBrancas.setText("");
        edtkomi.setText("");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnDetectarTabuleiro:
                if (edtJogadorPretas.getText().toString().trim().equals("")) {
                    edtJogadorPretas.requestFocus();
                    return;
                }
                if (edtJogadorBrancas.getText().toString().trim().equals("")) {
                    edtJogadorBrancas.requestFocus();
                    return;
                }
                if (edtkomi.getText().toString().trim().equals("")) {
                    edtkomi.requestFocus();
                    return;
                }

//                savePreferences();
                checkCameraPermission();
                break;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        savePreferences();
        Log.d(TestesActivity.TAG, "onPause");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TestesActivity.TAG, "onStop");
    }

    @Override
    public void onResume() {
        super.onResume();
        restorePreferences();
        Log.d(TestesActivity.TAG, "onResume");
    }

    @Override
    public void onRestart() {
        super.onRestart();
        Log.d(TestesActivity.TAG, "onRestart");
    }

    private void savePreferences() {
        preferences = getPreferences(Context.MODE_PRIVATE);
        editor = preferences.edit();
        editor.putString(getString(R.string.preferences_jogador_pretas), edtJogadorPretas.getText().toString());
        editor.putString(getString(R.string.preferences_jogador_brancas), edtJogadorBrancas.getText().toString());
        editor.putString(getString(R.string.preferences_komi), edtkomi.getText().toString());
        editor.commit();
    }

    private void restorePreferences() {
        preferences = getPreferences(Context.MODE_PRIVATE);
        edtJogadorPretas.setText(preferences.getString(getString(R.string.preferences_jogador_pretas), ""));
        edtJogadorBrancas.setText(preferences.getString(getString(R.string.preferences_jogador_brancas), ""));
        edtkomi.setText(preferences.getString(getString(R.string.preferences_komi), ""));
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(InformacoesDaPartidaActivity.this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(InformacoesDaPartidaActivity.this, new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        } else iniciarActivityDetectarTabuleiro();
    }

    private void iniciarActivityDetectarTabuleiro() {
        String jogadorDePretas = edtJogadorPretas.getText().toString();
        String jogadorDeBrancas = edtJogadorBrancas.getText().toString();
        String komi = edtkomi.getText().toString();

        Intent i = new Intent(this, DetectarTabuleiroActivity.class);
        i.putExtra("jogadorDePretas", jogadorDePretas);
        i.putExtra("jogadorDeBrancas", jogadorDeBrancas);
        i.putExtra("komi", komi);
        startActivity(i);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarActivityDetectarTabuleiro();
            } else {
                Toast.makeText(InformacoesDaPartidaActivity.this, getResources().getString(R.string.toast_permissao_camera), Toast.LENGTH_SHORT).show();
            }
        }
    }

}
