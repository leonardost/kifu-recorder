package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.app.Activity;
import android.content.Intent;
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

                // TODO: Remover esta activity da pilha de activities

                break;
        }
    }



/*    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_informacoes_da_partida, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }*/
}
