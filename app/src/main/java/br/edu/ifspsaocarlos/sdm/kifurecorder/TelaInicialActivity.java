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
        setContentView(R.layout.activity_start_screen);

        TextView lblVersao = (TextView) findViewById(R.id.lblVersion);
        Button btnIniciarRegistro = (Button) findViewById(R.id.btnStartRecord);
        Button btnInstrucoes = (Button) findViewById(R.id.btnInstructions);
        Button btnCreditos = (Button) findViewById(R.id.btnCredits);
        lblVersao.setText("v" + BuildConfig.VERSION_NAME);
        btnIniciarRegistro.setOnClickListener(this);
        btnInstrucoes.setOnClickListener(this);
        btnCreditos.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent i;
        switch (v.getId()) {
            case R.id.btnStartRecord:
                i = new Intent(this, GameInformationActivity.class);
                startActivity(i);
                break;
            case R.id.btnInstructions:
                i = new Intent(this, InstructionsActivity.class);
                startActivity(i);
                break;
            case R.id.btnCredits:
                i = new Intent(this, CreditsActivity.class);
                startActivity(i);
                break;
        }
    }

}
