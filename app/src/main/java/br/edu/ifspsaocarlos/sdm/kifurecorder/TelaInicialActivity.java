package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class TelaInicialActivity extends Activity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tela_inicial);

        Button btnIniciarRegistro = (Button) findViewById(R.id.btnIniciarRegistro);
        Button btnTestes = (Button) findViewById(R.id.btnTestes);
        btnIniciarRegistro.setOnClickListener(this);
        btnTestes.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent i;
        switch (v.getId()) {
            case R.id.btnIniciarRegistro:
                i = new Intent(this, InformacoesDaPartidaActivity.class);
                startActivity(i);
                break;
            case R.id.btnTestes:
                i = new Intent(this, TestesActivity.class);
                startActivity(i);
                break;
        }
    }

}
