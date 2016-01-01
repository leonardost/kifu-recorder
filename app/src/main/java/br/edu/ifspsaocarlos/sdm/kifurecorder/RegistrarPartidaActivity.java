package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Partida;
import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Tabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.Desenhista;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDePedras;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.TransformadorDeTabuleiro;


public class RegistrarPartidaActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    private String jogadorDePretas;
    private String jogadorDeBrancas;
    private String komi;

    Mat posicaoDoTabuleiroNaImagem;
    int dimensaoDoTabuleiro;
    Point[] cantos;
    MatOfPoint contornoDoTabuleiro;
    DetectorDePedras detectorDePedras = new DetectorDePedras();
    long momentoDaUltimaDeteccaoDeTabuleiro;
    long tempoDesdeUltimaMudancaDeTabuleiro;
    Tabuleiro ultimoTabuleiro;
    Partida partida;

    ImageButton btnVoltarUltimaJogada;
    ImageButton btnRotacionarEsquerda;
    ImageButton btnRotacionarDireita;
    ImageButton btnPausar;
    Button btnFinalizar;

    boolean pausado = false;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(MainActivity.TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registrar_partida);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_registro);
        mOpenCvCameraView.setCvCameraViewListener(this);

        Intent i = getIntent();
        jogadorDePretas = i.getStringExtra("jogadorDePretas");
        jogadorDeBrancas = i.getStringExtra("jogadorDeBrancas");
        komi = i.getStringExtra("komi");
        dimensaoDoTabuleiro = i.getIntExtra("dimensaoDoTabuleiro", -1);
        int[] cantosDoTabuleiro = i.getIntArrayExtra("posicaoDoTabuleiroNaImagem");

        posicaoDoTabuleiroNaImagem = new Mat(4, 1, CvType.CV_32FC2);
        posicaoDoTabuleiroNaImagem.put(0, 0,
                cantosDoTabuleiro[0], cantosDoTabuleiro[1],
                cantosDoTabuleiro[2], cantosDoTabuleiro[3],
                cantosDoTabuleiro[4], cantosDoTabuleiro[5],
                cantosDoTabuleiro[6], cantosDoTabuleiro[7]);

        cantos = new Point[4];
        cantos[0] = new Point(cantosDoTabuleiro[0], cantosDoTabuleiro[1]);
        cantos[1] = new Point(cantosDoTabuleiro[2], cantosDoTabuleiro[3]);
        cantos[2] = new Point(cantosDoTabuleiro[4], cantosDoTabuleiro[5]);
        cantos[3] = new Point(cantosDoTabuleiro[6], cantosDoTabuleiro[7]);
        contornoDoTabuleiro = new MatOfPoint(cantos);

        partida = new Partida(dimensaoDoTabuleiro);
        ultimoTabuleiro = new Tabuleiro(dimensaoDoTabuleiro);
        momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
        tempoDesdeUltimaMudancaDeTabuleiro = 0;

        btnVoltarUltimaJogada = (ImageButton) findViewById(R.id.btnVoltarUltimaJogada);
        btnVoltarUltimaJogada.setOnClickListener(this);
        btnVoltarUltimaJogada.setEnabled(false);
        btnRotacionarEsquerda = (ImageButton) findViewById(R.id.btnRotacionarEsquerda);
        btnRotacionarEsquerda.setOnClickListener(this);
        btnRotacionarDireita = (ImageButton) findViewById(R.id.btnRotacionarDireita);
        btnRotacionarDireita.setOnClickListener(this);
        btnPausar = (ImageButton) findViewById(R.id.btnPausar);
        btnPausar.setOnClickListener(this);
        btnFinalizar = (Button) findViewById(R.id.btnFinalizar);
        btnFinalizar.setOnClickListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {

    }

    public void onCameraViewStopped() {

    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat imagemFonte = inputFrame.rgba();

        Mat tabuleiroOrtogonal = TransformadorDeTabuleiro.transformar(imagemFonte, posicaoDoTabuleiroNaImagem, null);
        detectorDePedras.setImagemDoTabuleiro(tabuleiroOrtogonal);
        detectorDePedras.setDimensaoDoTabuleiro(dimensaoDoTabuleiro);
        tabuleiroOrtogonal.copyTo(imagemFonte.rowRange(0, 500).colRange(0, 500));
        Tabuleiro tabuleiro = detectorDePedras.detectar();

        long tempoLimite = 2000;

        if (!pausado) {

            if (ultimoTabuleiro.equals(tabuleiro)) {
                tempoDesdeUltimaMudancaDeTabuleiro += SystemClock.elapsedRealtime() - momentoDaUltimaDeteccaoDeTabuleiro;
                momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
                if (tempoDesdeUltimaMudancaDeTabuleiro > tempoLimite) {
                    partida.adicionarJogadaSeForValida(tabuleiro);
                    if (partida.numeroDeJogadasFeitas() > 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                btnVoltarUltimaJogada.setEnabled(true);
                            }
                        });
                    }
                }
            } else {
                tempoDesdeUltimaMudancaDeTabuleiro = 0;
                momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
            }

        }

        ultimoTabuleiro = tabuleiro;

        Desenhista.desenharContornoDoTabuleiro(imagemFonte, contornoDoTabuleiro);
        if (pausado) {
            Desenhista.desenharTabuleiro(imagemFonte, tabuleiro, 0, 500, 400);
        }
        else {
            Desenhista.desenharTabuleiro(imagemFonte, partida.ultimoTabuleiro(), 0, 500, 400);
        }

        return imagemFonte;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnVoltarUltimaJogada:
                temCertezaQueDesejaVoltarAUltimaJogada(getString(R.string.btn_voltar_ultima_jogada));
                break;
            case R.id.btnRotacionarEsquerda:
                rotacionar(-1);
                break;
            case R.id.btnRotacionarDireita:
                rotacionar(1);
                break;
            case R.id.btnPausar:
                pausado = !pausado;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (pausado) {
                            btnPausar.setImageResource(R.drawable.play);
                        } else {
                            btnPausar.setImageResource(R.drawable.pause);
                        }
                    }
                });
                break;
            case R.id.btnFinalizar:
                temCertezaQueDesejaFinalizarORegisro();
                break;
        }
    }

    private void temCertezaQueDesejaVoltarAUltimaJogada(String mensagem) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_tem_certeza)
                .setMessage(mensagem)
                .setPositiveButton(R.string.sim, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        partida.voltarUltimaJogada();
                        if (partida.numeroDeJogadasFeitas() == 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    btnVoltarUltimaJogada.setEnabled(false);
                                }
                            });
                        }
                    }
                })
                .setNegativeButton(R.string.nao, null)
                .show();
    }

    private void rotacionar(int direcao) {
        Point[] cantosRotacionados = new Point[4];
        // Anti-horário
        if (direcao == -1) {
            cantosRotacionados[0] = cantos[1];
            cantosRotacionados[1] = cantos[2];
            cantosRotacionados[2] = cantos[3];
            cantosRotacionados[3] = cantos[0];
        }
        // Horário
        else if (direcao == 1) {
            cantosRotacionados[0] = cantos[3];
            cantosRotacionados[1] = cantos[0];
            cantosRotacionados[2] = cantos[1];
            cantosRotacionados[3] = cantos[2];
        }
        contornoDoTabuleiro = new MatOfPoint(cantosRotacionados);
        cantos = cantosRotacionados;

        posicaoDoTabuleiroNaImagem = new Mat(4, 1, CvType.CV_32FC2);
        posicaoDoTabuleiroNaImagem.put(0, 0,
                cantos[0].x, cantos[0].y,
                cantos[1].x, cantos[1].y,
                cantos[2].x, cantos[2].y,
                cantos[3].x, cantos[3].y);

        partida.rotacionar(direcao);
    }

    private void temCertezaQueDesejaFinalizarORegisro() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_tem_certeza)
                .setMessage(getString(R.string.dialog_finalizar_registro))
                .setPositiveButton(R.string.sim, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        salvarArquivoESair();
                    }
                })
                .setNegativeButton(R.string.nao, null)
                .show();
    }

    private void salvarArquivoESair() {
        String nomeArquivo = gerarNomeDeArquivo();
        String conteudoDaPartida = partida.sgf();

        if (isExternalStorageWritable()) {
            File arquivo = getFile(nomeArquivo);
            try {
                FileOutputStream fos = new FileOutputStream(arquivo);
                fos.write(conteudoDaPartida.getBytes());
                fos.flush();
                fos.close();
                Toast.makeText(RegistrarPartidaActivity.this,
                        "Partida salva no arquivo: " + arquivo.getName() + ".",
                        Toast.LENGTH_LONG).show();
                Log.i(MainActivity.TAG, "Partida salva: " + arquivo.getName() + " com conteúdo " + conteudoDaPartida);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                finish();
            }
        }
        else {
            Log.i(MainActivity.TAG, "ERRO: Armazenamento externo não disponível.");
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private File getFile(String filename) {
        File pasta = new File(Environment.getExternalStorageDirectory(), "sgfs_salvos");
        if (!pasta.exists()) {
            if (!pasta.mkdirs()) {
                Toast.makeText(
                        RegistrarPartidaActivity.this,
                        "ERRO: Diretório " + pasta.toString() + " não criado.",
                        Toast.LENGTH_LONG).show();
                Log.i(MainActivity.TAG, "ERRO: Diretório " + pasta.toString() + " não criada.");
            }
        }

        File arquivo = new File(pasta, filename);
        int contador = 1;
        while (arquivo.exists()) {
            String newFilename = filename + "(" + contador + ")";
            arquivo = new File(pasta, newFilename);
            contador++;
        }

        return arquivo;
    }

    private String gerarNomeDeArquivo() {
        StringBuilder string = new StringBuilder();
        // http://stackoverflow.com/questions/10203924/displaying-date-in-a-double-digit-format
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();

        string.append(jogadorDeBrancas)
                .append("-")
                .append(jogadorDePretas)
                .append("_")
                .append(sdf.format(new Date(c.getTimeInMillis())))
                .append(".sgf");
        return string.toString();
    }

    @Override
    public void onBackPressed() {
        temCertezaQueDesejaFinalizarORegisro();
    }

}
