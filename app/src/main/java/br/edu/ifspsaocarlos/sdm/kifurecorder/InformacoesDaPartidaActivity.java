package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class InformacoesDaPartidaActivity extends Activity implements View.OnClickListener {

    public static final int PERMISSION_REQUEST_CODE = 123;

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

                checkCameraPermission();
                break;
        }
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
