package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Partida;
import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Tabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.Desenhista;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDePedras;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.TransformadorDeTabuleiro;


public class RegistrarPartidaActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    Mat posicaoDoTabuleiroNaImagem;
    int dimensaoDoTabuleiro;
    MatOfPoint contornoDoTabuleiro;
    DetectorDePedras detectorDePedras = new DetectorDePedras();
    long momentoDaUltimaDeteccaoDeTabuleiro;
    long tempoDesdeUltimaMudancaDeTabuleiro;
    Tabuleiro ultimoTabuleiro;
    Partida partida;

    Button btnVoltarUltimaJogada;

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

        dimensaoDoTabuleiro = getIntent().getExtras().getInt("dimensaoDoTabuleiro");
        int[] cantosDoTabuleiro = getIntent().getExtras().getIntArray("posicaoDoTabuleiroNaImagem");

        posicaoDoTabuleiroNaImagem = new Mat(4, 1, CvType.CV_32FC2);
        posicaoDoTabuleiroNaImagem.put(0, 0,
                cantosDoTabuleiro[0], cantosDoTabuleiro[1],
                cantosDoTabuleiro[2], cantosDoTabuleiro[3],
                cantosDoTabuleiro[4], cantosDoTabuleiro[5],
                cantosDoTabuleiro[6], cantosDoTabuleiro[7]);

        Point[] cantos = { new Point(cantosDoTabuleiro[0], cantosDoTabuleiro[1]),
                new Point(cantosDoTabuleiro[2], cantosDoTabuleiro[3]),
                new Point(cantosDoTabuleiro[4], cantosDoTabuleiro[5]),
                new Point(cantosDoTabuleiro[6], cantosDoTabuleiro[7])
        };
        contornoDoTabuleiro = new MatOfPoint(cantos);

        partida = new Partida(dimensaoDoTabuleiro);
        ultimoTabuleiro = new Tabuleiro(dimensaoDoTabuleiro);
        momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
        tempoDesdeUltimaMudancaDeTabuleiro = 0;

        btnVoltarUltimaJogada = (Button) findViewById(R.id.btnVoltarUltimaJogada);
        btnVoltarUltimaJogada.setEnabled(false);
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
        }
        else {
            tempoDesdeUltimaMudancaDeTabuleiro = 0;
            momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
        }
        ultimoTabuleiro = tabuleiro;

        Desenhista.desenharContornoDoTabuleiro(imagemFonte, contornoDoTabuleiro);
//        Desenhista.desenharTabuleiro(imagemFonte, tabuleiro, 0, 500, 400);
        Desenhista.desenharTabuleiro(imagemFonte, partida.ultimoTabuleiro(), 0, 500, 400);

        return imagemFonte;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnVoltarUltimaJogada:
                temCertezaQueDesejaVoltarAUltimaJogada(getString(R.string.btn_voltar_ultima_jogada));
                break;
            case R.id.btnFinalizar:
                salvarArquivoESair();
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

    private void salvarArquivoESair() {
        String nomeArquivo = "jogoTeste.sgf";
        String conteudoDaPartida = partida.sgf();
        // TODO: abrir arquivo no sistema de arquivos e salvar conteúdo
        Log.d(MainActivity.TAG, "Partida salva: " + nomeArquivo + " com conteúdo " + conteudoDaPartida);
    }


}
