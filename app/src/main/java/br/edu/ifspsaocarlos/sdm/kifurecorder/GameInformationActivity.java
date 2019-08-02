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

import java.util.ArrayList;
import java.util.List;

public class GameInformationActivity extends Activity implements View.OnClickListener {

    public static final int PERMISSION_REQUEST_CODE = 123;

    EditText edtBlackPlayer;
    EditText edtWhitePlayer;
    EditText edtkomi;
    Button btnDetectBoard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_information);

        edtBlackPlayer = (EditText) findViewById(R.id.edtJogadorPretas);
        edtWhitePlayer = (EditText) findViewById(R.id.edtJogadorBrancas);
        edtkomi = (EditText) findViewById(R.id.edtKomi);
        btnDetectBoard = (Button) findViewById(R.id.btnDetectarTabuleiro);
        btnDetectBoard.setOnClickListener(this);

        edtBlackPlayer.setText("");
        edtWhitePlayer.setText("");
        edtkomi.setText("");
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnDetectarTabuleiro:
                if (edtBlackPlayer.getText().toString().trim().equals("")) {
                    edtBlackPlayer.requestFocus();
                    return;
                }
                if (edtWhitePlayer.getText().toString().trim().equals("")) {
                    edtWhitePlayer.requestFocus();
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
        List<String> neededPermissions = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(GameInformationActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.CAMERA);
        }
        if (ContextCompat.checkSelfPermission(GameInformationActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            neededPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (!neededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(GameInformationActivity.this, neededPermissions.toArray(new String[neededPermissions.size()]), PERMISSION_REQUEST_CODE);
        } else {
            startDetectBoardAcitivity();
        }
    }

    private void startDetectBoardAcitivity() {
        String blackPlayer = edtBlackPlayer.getText().toString();
        String whitePlayer = edtWhitePlayer.getText().toString();
        String komi = edtkomi.getText().toString();

        Intent i = new Intent(this, DetectBoardActivity.class);
        i.putExtra("blackPlayer", blackPlayer);
        i.putExtra("whitePlayer", whitePlayer);
        i.putExtra("komi", komi);
        startActivity(i);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDetectBoardAcitivity();
            } else {
                Toast.makeText(GameInformationActivity.this, getResources().getString(R.string.toast_permissao_camera), Toast.LENGTH_SHORT).show();
            }
        }
    }

}
