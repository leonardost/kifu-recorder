package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
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
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Jogada;
import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Partida;
import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Tabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.Desenhista;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDePedras;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.TransformadorDeTabuleiro;


public class RegistrarPartidaActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

	// Tempo que um tabuleiro detectado deve se manter inalterado para que seja considerado pelo detector
	long tempoLimite = 1500;

    int[] cantosDoTabuleiro;                // Pontos dos cantos do tabuleiro
    Mat posicaoDoTabuleiroNaImagem;         // Matriz que contem os cantos do tabuleiro
    int dimensaoDoTabuleiro;                // 9x9, 13x13 ou 19x19
    Point[] cantos;
    MatOfPoint contornoDoTabuleiro;
    DetectorDePedras detectorDePedras = new DetectorDePedras();
    long momentoDaUltimaDeteccaoDeTabuleiro;
    long tempoDesdeUltimaMudancaDeTabuleiro;
    Tabuleiro ultimoTabuleiro;
    Partida partida;
    String snapshotAtual;
    Mat tabuleiroOrtogonal;
    // A cada 5 jogadas feitas a partida é salva automaticamente
    int contadorDeJogadas = 0;

    private File arquivoDeRegistro;

    ImageButton btnSalvar;
    ImageButton btnVoltarUltimaJogada;
    ImageButton btnRotacionarEsquerda;
    ImageButton btnRotacionarDireita;
    ImageButton btnPausar;
    ImageButton btnSnapshot;
    Button btnFinalizar;
    SoundPool soundPool;
    int beepId;

    boolean pausado = false;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TestesActivity.TAG, "OpenCV loaded successfully");
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_registro);
        mOpenCvCameraView.setCvCameraViewListener(this);

        Intent i = getIntent();
        String jogadorDePretas = i.getStringExtra("jogadorDePretas");
        String jogadorDeBrancas = i.getStringExtra("jogadorDeBrancas");
        String komi = i.getStringExtra("komi");
        dimensaoDoTabuleiro = i.getIntExtra("dimensaoDoTabuleiro", -1);
        cantosDoTabuleiro = i.getIntArrayExtra("posicaoDoTabuleiroNaImagem");

        processarCantosDoTabuleiro();

        partida = new Partida(dimensaoDoTabuleiro, jogadorDePretas, jogadorDeBrancas, komi);
        ultimoTabuleiro = new Tabuleiro(dimensaoDoTabuleiro);
        momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
        tempoDesdeUltimaMudancaDeTabuleiro = 0;

        arquivoDeRegistro = getFile();

        btnSalvar = (ImageButton) findViewById(R.id.btnSalvar);
        btnSalvar.setOnClickListener(this);
        btnSalvar.setEnabled(true);
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
        btnSnapshot = (ImageButton) findViewById(R.id.btnSnapshot);
        btnSnapshot.setOnClickListener(this);

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        beepId = soundPool.load(this, R.raw.beep, 1);

        Log.i(TestesActivity.TAG, "onCreate() finalizado");
    }

    private void processarCantosDoTabuleiro() {
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
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstaceState) {
        super.onRestoreInstanceState(savedInstaceState);

        restaurarPartidaSalvaTemporariamente();
        processarCantosDoTabuleiro();
    }

    @Override
    public void onPause()
    {
        guardaPartidaTemporariamente();
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
    }

    private void guardaPartidaTemporariamente() {
        File arquivo = getTempFile();
        if (isExternalStorageWritable()) {
            try {
                FileOutputStream fos = new FileOutputStream(arquivo, false);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(partida);
                oos.writeObject(arquivoDeRegistro);
                oos.writeObject(cantosDoTabuleiro);
                oos.close();
                fos.close();
                Log.i(TestesActivity.TAG, "Partida salva temporariamente no arquivo " + arquivo.getName());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.i(TestesActivity.TAG, "ERRO: Armazenamento externo não disponível para guardar registro temporário da partida.");
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    private void restaurarPartidaSalvaTemporariamente() {
        File arquivo = getTempFile();
        if (isExternalStorageWritable()) {
            try {
                FileInputStream fis = new FileInputStream(arquivo);
                ObjectInputStream ois = new ObjectInputStream(fis);
                partida = (Partida) ois.readObject();
                arquivoDeRegistro = (File) ois.readObject();
                cantosDoTabuleiro = (int[]) ois.readObject();
                ois.close();
                fis.close();
                Log.i(TestesActivity.TAG, "Partida recuperada.");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.i(TestesActivity.TAG, "ERRO: Armazenamento externo não disponível para guardar registro temporário da partida.");
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    public void onCameraViewStarted(int width, int height) {

    }

    public void onCameraViewStopped() {

    }

    /**
     * Método chamado sempre que há um frame da câmera pronto para ser processado
     */
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        long tempoEntrou = System.currentTimeMillis();
        Mat imagemFonte = inputFrame.rgba();

        tabuleiroOrtogonal = TransformadorDeTabuleiro.transformar(imagemFonte, posicaoDoTabuleiroNaImagem, null);
        detectorDePedras.setImagemDoTabuleiro(tabuleiroOrtogonal);
        detectorDePedras.setDimensaoDoTabuleiro(dimensaoDoTabuleiro);
        // Desenha o tabuleiro ortogonal na tela
        tabuleiroOrtogonal.copyTo(imagemFonte.rowRange(0, 500).colRange(0, 500));
  
        Jogada jogada = detectorDePedras.detectar(partida.ultimoTabuleiro());
        Tabuleiro tabuleiro = partida.ultimoTabuleiro().gerarNovoTabuleiroComAJogada(jogada);
        snapshotAtual = detectorDePedras.snapshot.toString();
//        Tabuleiro tabuleiro = detectorDePedras.detectar();

        if (!pausado) {

            if (ultimoTabuleiro.equals(tabuleiro)) {
                tempoDesdeUltimaMudancaDeTabuleiro += SystemClock.elapsedRealtime() - momentoDaUltimaDeteccaoDeTabuleiro;
                momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
                if (tempoDesdeUltimaMudancaDeTabuleiro > tempoLimite) {
                    if (partida.adicionarJogadaSeForValida(tabuleiro)) {
                        contadorDeJogadas++;
                        if (contadorDeJogadas % 5 == 0) {
                            salvarArquivo();
                        }

                        soundPool.play(beepId, 1, 1, 0, 0, 1);

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
            } else {
                tempoDesdeUltimaMudancaDeTabuleiro = 0;
                momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
            }

        }

        ultimoTabuleiro = tabuleiro;

        Desenhista.desenharContornoDoTabuleiro(imagemFonte, contornoDoTabuleiro);
        if (pausado) {
            // Quando está pausado, desenha a saída atual do detector de pedras (útil para debugar)
            Desenhista.desenharTabuleiro(imagemFonte, tabuleiro, 0, 500, 400, null);
        }
        else {
            Desenhista.desenharTabuleiro(imagemFonte, partida.ultimoTabuleiro(), 0, 500, 400, partida.ultimaJogada());
        }

        Log.d(TestesActivity.TAG, "TEMPO (onCameraFrame()): " + (System.currentTimeMillis() - tempoEntrou));
        return imagemFonte;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSalvar:
                salvarArquivo();
                break;
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
            case R.id.btnSnapshot:
                tirarSnapshot();
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
                        tempoDesdeUltimaMudancaDeTabuleiro = 0;
                        momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
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

    private void tirarSnapshot() {
        File arquivoSnapshot = getFileSnapshot();
        File arquivoImagemSnapshot = getFileImagemSnapshot();
        try {
            FileOutputStream fos = new FileOutputStream(arquivoSnapshot, false);
            Mat imagemFormatoDeCorCerto = new Mat();
            Imgproc.cvtColor(tabuleiroOrtogonal, imagemFormatoDeCorCerto, Imgproc.COLOR_RGBA2BGR);
            Highgui.imwrite(arquivoImagemSnapshot.getAbsolutePath(), imagemFormatoDeCorCerto);
            fos.write(snapshotAtual.getBytes());
            fos.flush();
            fos.close();
            Log.i(TestesActivity.TAG, "Snapshot salva: " + arquivoSnapshot.getName() + " com conteúdo " + snapshotAtual);
            Toast.makeText(RegistrarPartidaActivity.this,
                    "Snapshot salva no arquivo: " + arquivoSnapshot.getName() + ".",
                    Toast.LENGTH_LONG).show();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void rotacionar(int direcao) {
        int[] cantosDoTabuleiroRotacionados = new int[8];

        if (direcao == -1) {
            cantosDoTabuleiroRotacionados[0] = cantosDoTabuleiro[2];
            cantosDoTabuleiroRotacionados[1] = cantosDoTabuleiro[3];
            cantosDoTabuleiroRotacionados[2] = cantosDoTabuleiro[4];
            cantosDoTabuleiroRotacionados[3] = cantosDoTabuleiro[5];
            cantosDoTabuleiroRotacionados[4] = cantosDoTabuleiro[6];
            cantosDoTabuleiroRotacionados[5] = cantosDoTabuleiro[7];
            cantosDoTabuleiroRotacionados[6] = cantosDoTabuleiro[0];
            cantosDoTabuleiroRotacionados[7] = cantosDoTabuleiro[1];
        }
        // Horário
        else if (direcao == 1) {
            cantosDoTabuleiroRotacionados[0] = cantosDoTabuleiro[6];
            cantosDoTabuleiroRotacionados[1] = cantosDoTabuleiro[7];
            cantosDoTabuleiroRotacionados[2] = cantosDoTabuleiro[0];
            cantosDoTabuleiroRotacionados[3] = cantosDoTabuleiro[1];
            cantosDoTabuleiroRotacionados[4] = cantosDoTabuleiro[2];
            cantosDoTabuleiroRotacionados[5] = cantosDoTabuleiro[3];
            cantosDoTabuleiroRotacionados[6] = cantosDoTabuleiro[4];
            cantosDoTabuleiroRotacionados[7] = cantosDoTabuleiro[5];
        }

        cantosDoTabuleiro = cantosDoTabuleiroRotacionados;
        processarCantosDoTabuleiro();

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

    private void salvarArquivo() {
        String conteudoDaPartida = partida.sgf();
        if (isExternalStorageWritable()) {
            try {
                FileOutputStream fos = new FileOutputStream(arquivoDeRegistro, false);
                fos.write(conteudoDaPartida.getBytes());
                fos.flush();
                fos.close();
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(RegistrarPartidaActivity.this,
                                "Partida salva no arquivo: " + arquivoDeRegistro.getName() + ".",
                                Toast.LENGTH_LONG).show();
                    }
                });
                Log.i(TestesActivity.TAG, "Partida salva: " + arquivoDeRegistro.getName() + " com conteúdo " + conteudoDaPartida);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            Toast.makeText(RegistrarPartidaActivity.this, "ERRO: Armazenamento externo nao disponivel.", Toast.LENGTH_LONG).show();
            Log.i(TestesActivity.TAG, "ERRO: Armazenamento externo não disponível.");
        }
    }

    private void salvarArquivoESair() {
        String conteudoDaPartida = partida.sgf();

        if (isExternalStorageWritable()) {
            try {
                FileOutputStream fos = new FileOutputStream(arquivoDeRegistro, false);
                fos.write(conteudoDaPartida.getBytes());
                fos.flush();
                fos.close();
                Toast.makeText(RegistrarPartidaActivity.this,
                        "Partida salva no arquivo: " + arquivoDeRegistro.getName() + ".",
                        Toast.LENGTH_LONG).show();
                Log.i(TestesActivity.TAG, "Partida salva: " + arquivoDeRegistro.getName() + " com conteúdo " + conteudoDaPartida);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
                finish();
            }
        }
        else {
            Log.i(TestesActivity.TAG, "ERRO: Armazenamento externo não disponível.");
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private File getFile() {
        File pasta = new File(Environment.getExternalStorageDirectory(), "sgfs_salvos");
        if (!pasta.exists()) {
            if (!pasta.mkdirs()) {
                Toast.makeText(
                        RegistrarPartidaActivity.this,
                        "ERRO: Diretório " + pasta.toString() + " não criado.",
                        Toast.LENGTH_LONG).show();
                Log.i(TestesActivity.TAG, "ERRO: Diretório " + pasta.toString() + " não criada.");
            }
        }

        File arquivo = new File(pasta, gerarNomeDeArquivo(0, "sgf"));
        int contador = 1;
        while (arquivo.exists()) {
            String newFilename = gerarNomeDeArquivo(contador, "sgf");
            arquivo = new File(pasta, newFilename);
            contador++;
        }

        return arquivo;
    }

    private File getTempFile() {
        File pasta = new File(Environment.getExternalStorageDirectory(), "sgfs_salvos");
        if (!pasta.exists()) {
            if (!pasta.mkdirs()) {
                Toast.makeText(
                        RegistrarPartidaActivity.this,
                        "ERRO: Diretório " + pasta.toString() + " não criado.",
                        Toast.LENGTH_LONG).show();
                Log.i(TestesActivity.TAG, "ERRO: Diretório " + pasta.toString() + " não criado.");
            }
        }

        return new File(pasta, "arquivo_temporario");
    }

    private File getFileSnapshot() {
        File pasta = new File(Environment.getExternalStorageDirectory(), "sgfs_salvos");
        if (!pasta.exists()) {
            if (!pasta.mkdirs()) {
                Toast.makeText(
                        RegistrarPartidaActivity.this,
                        "ERRO: Diretório " + pasta.toString() + " não criado.",
                        Toast.LENGTH_LONG).show();
                Log.i(TestesActivity.TAG, "ERRO: Diretório " + pasta.toString() + " não criada.");
            }
        }

        File arquivo = new File(pasta, gerarNomeDeArquivo(0, "txt"));
        int contador = 1;
        while (arquivo.exists()) {
            String newFilename = gerarNomeDeArquivo(contador, "txt");
            arquivo = new File(pasta, newFilename);
            contador++;
        }

        return arquivo;
    }

    private File getFileImagemSnapshot() {
        File pasta = new File(Environment.getExternalStorageDirectory(), "sgfs_salvos");
        if (!pasta.exists()) {
            if (!pasta.mkdirs()) {
                Toast.makeText(
                        RegistrarPartidaActivity.this,
                        "ERRO: Diretório " + pasta.toString() + " não criado.",
                        Toast.LENGTH_LONG).show();
                Log.i(TestesActivity.TAG, "ERRO: Diretório " + pasta.toString() + " não criada.");
            }
        }

        File arquivo = new File(pasta, gerarNomeDeArquivo(0, "jpg"));
        int contador = 1;
        while (arquivo.exists()) {
            String newFilename = gerarNomeDeArquivo(contador, "jpg");
            arquivo = new File(pasta, newFilename);
            contador++;
        }

        return arquivo;
    }

    private String gerarNomeDeArquivo(int contadorDeNomeRepetido, String extensao) {
        StringBuilder string = new StringBuilder();
        // http://stackoverflow.com/questions/10203924/displaying-date-in-a-double-digit-format
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        String contador = "";
        if (contadorDeNomeRepetido > 0) {
            contador = "(" + contadorDeNomeRepetido + ")";
        }

        string.append(partida.getJogadorDeBrancas())
                .append("-")
                .append(partida.getJogadorDePretas())
                .append("_")
                .append(sdf.format(new Date(c.getTimeInMillis())))
                .append(contador)
                .append(".")
                .append(extensao);
        return string.toString();
    }

    // Verificar se isto está sendo chamado
    @Override
    public void onBackPressed() {
        temCertezaQueDesejaFinalizarORegisro();
    }

}
