package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDeTabuleiro;

public class InformacoesDaPartidaActivity extends Activity implements View.OnClickListener {

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
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnDetectarTabuleiro:
                if (edtJogadorPretas.getText().toString().equals("")) {
                    edtJogadorPretas.requestFocus();
                    return;
                }
                if (edtJogadorBrancas.getText().toString().equals("")) {
                    edtJogadorBrancas.requestFocus();
                    return;
                }
                if (edtkomi.getText().toString().equals("")) {
                    edtkomi.requestFocus();
                    return;
                }
                // TODO: Guardar informações da partida em algum lugar persistente
                Intent i = new Intent(this, DetectarTabuleiroActivity.class);
                startActivity(i);
                break;
        }
    }
}
