package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class TelaInicialActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_inicial);

        TextView lblVersao = (TextView) findViewById(R.id.lblVersao);
        Button btnIniciarRegistro = (Button) findViewById(R.id.btnIniciarRegistro);
        Button btnInstrucoes = (Button) findViewById(R.id.btnInstrucoes);
        Button btnCreditos = (Button) findViewById(R.id.btnCreditos);
        lblVersao.setText("v" + BuildConfig.VERSION_NAME);
        btnIniciarRegistro.setOnClickListener(this);
        btnInstrucoes.setOnClickListener(this);
        btnCreditos.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent i;
        switch (v.getId()) {
            case R.id.btnIniciarRegistro:
                i = new Intent(this, InformacoesDaPartidaActivity.class);
                startActivity(i);
                break;
            case R.id.btnInstrucoes:
                i = new Intent(this, InstrucoesActivity.class);
                startActivity(i);
                break;
            case R.id.btnCreditos:
                i = new Intent(this, CreditosActivity.class);
                startActivity(i);
                break;
        }
    }

}
