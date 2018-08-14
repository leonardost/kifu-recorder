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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Jogada;
import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Partida;
import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Tabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.Desenhista;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDePedras;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.TransformadorDeTabuleiro;

public class RegistrarPartidaActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    private boolean DEBUG = true;
    DetectorDePedras detectorDePedras = new DetectorDePedras();
    Partida partida;
    Tabuleiro ultimoTabuleiroDetectado;

    int contadorDeJogadas = 0;                 // A cada 5 jogadas feitas a partida é salva automaticamente
    long tempoLimite = 2000;                   // Tempo que um tabuleiro detectado deve se manter inalterado para que seja considerado pelo detector
    long momentoDaUltimaDeteccaoDeTabuleiro;
    long tempoDesdeUltimaMudancaDeTabuleiro;
    String snapshotAtual;
    boolean pausado = false;
    long momentoDoUltimoProcessamentoDeImagem;
    long tempoDesdeUltimoProcessamentoDeImagem;

    int dimensaoDoTabuleiro;                   // 9x9, 13x13 ou 19x19
    int[] cantosDoTabuleiro;                   // Pontos dos cantos do tabuleiro
    Mat posicaoDoTabuleiroNaImagem;            // Matriz que contem os cantos do tabuleiro
    Mat tabuleiroOrtogonal;                    // Imagem do tabuleiro transformado em visão ortogonal
    Mat[] imagemDoscantosDoTabuleiro;
    Mat[] imagemDasregioesAoRedorDosCantosDoTabuleiro;
    Mat[] imagensTemporarias1;
    Mat[] imagensTemporarias2;
    Mat[] imagensTemporarias3;
    Mat[] imagensTemporarias4;
	MatOfPoint contornoDoTabuleiro;

    private File pastaDeRegistro;              // Local onde os registros e arquivos de log serão salvos
    private File arquivoDeRegistro;            // Representa o arquivo SGF da partida
    private File arquivoDeLog;                 // Guarda informacoes de debug referentes a partida

    private ImageButton btnSalvar;
    private ImageButton btnVoltarUltimaJogada;
    private ImageButton btnRotacionarEsquerda;
    private ImageButton btnRotacionarDireita;
    private ImageButton btnPausar;
    private ImageButton btnSnapshot;
    private ImageButton btnAdicionarJogada;
    private Button btnFinalizar;
    private SoundPool soundPool;
    private int beepId;
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
//        mOpenCvCameraView.setMaxFrameSize(1000, 1000);
        mOpenCvCameraView.setCvCameraViewListener(this);

        Intent i = getIntent();
        String jogadorDePretas  = i.getStringExtra("jogadorDePretas");
        String jogadorDeBrancas = i.getStringExtra("jogadorDeBrancas");
        String komi             = i.getStringExtra("komi");
        dimensaoDoTabuleiro     = i.getIntExtra("dimensaoDoTabuleiro", -1);
        cantosDoTabuleiro       = i.getIntArrayExtra("posicaoDoTabuleiroNaImagem");
		detectorDePedras.setDimensaoDoTabuleiro(dimensaoDoTabuleiro);

        processarCantosDoTabuleiro();

        partida = new Partida(dimensaoDoTabuleiro, jogadorDePretas, jogadorDeBrancas, komi);
        ultimoTabuleiroDetectado = new Tabuleiro(dimensaoDoTabuleiro);
        momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
        tempoDesdeUltimaMudancaDeTabuleiro = 0;
        momentoDoUltimoProcessamentoDeImagem = SystemClock.elapsedRealtime();
        tempoDesdeUltimoProcessamentoDeImagem = 0;

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
        btnAdicionarJogada = (ImageButton) findViewById(R.id.btnAdicionarPedra);
        btnAdicionarJogada.setOnClickListener(this);

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        beepId = soundPool.load(this, R.raw.beep, 1);

        pastaDeRegistro = new File(Environment.getExternalStorageDirectory(), "kifu_recorder");
        if (!criarPastaDeRegistroSeNaoExistir()) {
            finish();
            return;
        }
        arquivoDeRegistro = getFile("sgf");
        arquivoDeLog = getFile("log");

        Log.i(TestesActivity.TAG, "onCreate() finalizado");
    }

    private void processarCantosDoTabuleiro() {
        posicaoDoTabuleiroNaImagem = new Mat(4, 1, CvType.CV_32FC2);
        posicaoDoTabuleiroNaImagem.put(0, 0,
                cantosDoTabuleiro[0], cantosDoTabuleiro[1],
                cantosDoTabuleiro[2], cantosDoTabuleiro[3],
                cantosDoTabuleiro[4], cantosDoTabuleiro[5],
                cantosDoTabuleiro[6], cantosDoTabuleiro[7]);

        Point[] cantos = new Point[4];
        cantos[0] = new Point(cantosDoTabuleiro[0], cantosDoTabuleiro[1]);
        cantos[1] = new Point(cantosDoTabuleiro[2], cantosDoTabuleiro[3]);
        cantos[2] = new Point(cantosDoTabuleiro[4], cantosDoTabuleiro[5]);
        cantos[3] = new Point(cantosDoTabuleiro[6], cantosDoTabuleiro[7]);
        contornoDoTabuleiro = new MatOfPoint(cantos);
    }

	private boolean criarPastaDeRegistroSeNaoExistir() {
        if (!pastaDeRegistro.exists() && !pastaDeRegistro.mkdirs()) {
            Toast.makeText(RegistrarPartidaActivity.this, "ERRO: Diretório " + pastaDeRegistro.toString() + " não criado, verifique as configurações de armazenamento de seu dispositivo.", Toast.LENGTH_LONG).show();
            Log.e(TestesActivity.TAG, "Diretório " + pastaDeRegistro.toString() + " não criado, verifique as configurações de armazenamento de seu dispositivo.");
            return false;
        }
        return true;
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
        super.onPause();
        Log.d(TestesActivity.TAG, "RegistrarPartidaActivity.onPause");
        guardaPartidaTemporariamente();  // TODO: Isto estava em cima, ver se não quebrou nada
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
            Log.e(TestesActivity.TAG, "Armazenamento externo não disponível para guardar registro temporário da partida.");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TestesActivity.TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TestesActivity.TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void restaurarPartidaSalvaTemporariamente() {
        File arquivo = getTempFile();
        // TODO: Precisa fazer esta checagem aqui? Porque aqui só é feita a leitura de arquivos
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
            Log.e(TestesActivity.TAG, "Armazenamento externo não disponível para restaurar registro temporário da partida.");
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

        atualizarPosicaoDoTabuleiro(imagemFonte);

        // Throttling, processa 2 vezes por segundo
        if (System.currentTimeMillis() - momentoDoUltimoProcessamentoDeImagem < 500) {
            tabuleiroOrtogonal.copyTo(imagemFonte.rowRange(0, 500).colRange(0, 500));
            Desenhista.desenharContornoDoTabuleiro(imagemFonte, contornoDoTabuleiro);
            Desenhista.desenharTabuleiro(imagemFonte, partida.ultimoTabuleiro(), 0, 500, 400, partida.ultimaJogada());
            return imagemFonte;
        }
        else momentoDoUltimoProcessamentoDeImagem = System.currentTimeMillis();

        recordDebugImages(inputFrame);

        tabuleiroOrtogonal = TransformadorDeTabuleiro.transformarOrtogonalmente(imagemFonte, posicaoDoTabuleiroNaImagem);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Verificar qual o tamanho da imagem do tabuleiro ortogonal aqui!!!
        //       Isso traz implicações para o raio calculado no método de calcular a cor média ao redor de uma posição
        // int larguraImagem = (int)tabuleiroOrtogonal.size().width;
        // int alturaImagem = (int)tabuleiroOrtogonal.size().height;
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        detectorDePedras.setImagemDoTabuleiro(tabuleiroOrtogonal);
        // Desenha o tabuleiro ortogonal na tela
        tabuleiroOrtogonal.copyTo(imagemFonte.rowRange(0, 500).colRange(0, 500));

        Tabuleiro tabuleiro = detectorDePedras.detectar(
                partida.ultimoTabuleiro(),
                partida.proximaJogadaPodeSer(Tabuleiro.PEDRA_PRETA),
                partida.proximaJogadaPodeSer(Tabuleiro.PEDRA_BRANCA)
        );

        snapshotAtual = detectorDePedras.snapshot.toString();
        snapshotAtual += partida.ultimoTabuleiro();
        snapshotAtual += "Jogada " + (partida.numeroDeJogadasFeitas() + 1) + "\n";
        snapshotAtual += "Cantos do tabuleiro:";
        snapshotAtual += "(" + cantosDoTabuleiro[0] + ", " + cantosDoTabuleiro[1] + ")\n";
        snapshotAtual += "(" + cantosDoTabuleiro[2] + ", " + cantosDoTabuleiro[3] + ")\n";
        snapshotAtual += "(" + cantosDoTabuleiro[4] + ", " + cantosDoTabuleiro[5] + ")\n";
        snapshotAtual += "(" + cantosDoTabuleiro[6] + ", " + cantosDoTabuleiro[7] + ")\n";
        snapshotAtual += "\n";

        if (!pausado) {

            if (ultimoTabuleiroDetectado.equals(tabuleiro)) {
                tempoDesdeUltimaMudancaDeTabuleiro += SystemClock.elapsedRealtime() - momentoDaUltimaDeteccaoDeTabuleiro;
                momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
                if (tempoDesdeUltimaMudancaDeTabuleiro > tempoLimite && partida.adicionarJogadaSeForValida(tabuleiro)) {
					novaJogadaFoiAdicionada();
					if (DEBUG) {
//                        adicionarAoLog(snapshotAtual.toString());
                        Mat imagemFormatoDeCorCerto = new Mat();
                        Imgproc.cvtColor(tabuleiroOrtogonal, imagemFormatoDeCorCerto, Imgproc.COLOR_RGBA2BGR);
                        Imgcodecs.imwrite(getFile("jogada" + partida.numeroDeJogadasFeitas(), "jpg").getAbsolutePath(), imagemFormatoDeCorCerto);
                    }
                }
            } else {
                tempoDesdeUltimaMudancaDeTabuleiro = 0;
                momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
            }

        }

        ultimoTabuleiroDetectado = tabuleiro;

        Desenhista.desenharContornoDoTabuleiro(imagemFonte, contornoDoTabuleiro);

        if (DEBUG) {
            adicionarAoLog(snapshotAtual.toString());
            Mat imagemCameraFormatoDeCorCerto = new Mat();
            Imgproc.cvtColor(imagemFonte, imagemCameraFormatoDeCorCerto, Imgproc.COLOR_RGBA2BGR);
            Imgcodecs.imwrite(getFile("jogada" + partida.numeroDeJogadasFeitas() + "_camera_com_contorno", "jpg").getAbsolutePath(), imagemCameraFormatoDeCorCerto);
        }

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

    private void atualizarPosicaoDoTabuleiro(Mat imagem) {
        initializeCornerObjectsIfNeeded();

        for (int i = 0; i < 4; i++) {

            // Pega região de interesse ao redor dos pontos dos cantos
            int x = cantosDoTabuleiro[i * 2] - 50 > 0 ? cantosDoTabuleiro[i * 2] - 50 : 0;
            int y = cantosDoTabuleiro[i * 2 + 1] - 50 > 0 ? cantosDoTabuleiro[i * 2 + 1] - 50 : 0;
            int w = x + 100 < imagem.cols() ? 100 : imagem.cols() - x;
            int h = y + 100 < imagem.rows() ? 100 : imagem.rows() - y;

            Rect regionOfInterest = new Rect(x, y, w, h);
            imagemDasregioesAoRedorDosCantosDoTabuleiro[i] = new Mat(imagem, regionOfInterest);
            Mat regionImage = new Mat(imagem, regionOfInterest);

            // Transforma em imagem escala de cinza
            Mat correctColorFormatImage = new Mat(imagem, regionOfInterest);
            Imgproc.cvtColor(regionImage, correctColorFormatImage, Imgproc.COLOR_BGR2GRAY);

//            Imgproc.cvtColor(imagemDaRegiao, imagemDaRegiao, Imgproc.COLOR_RGBA2GRAY);
//            imagensTemporarias1[i] = imagemDaRegiao.clone();

            // Converte para imagem com 1 canal de cinza (é iesso mesmo?)
            Mat gray = new Mat();
            correctColorFormatImage.convertTo(gray, CvType.CV_32F);
            // Aplica algoritmo Harris de detecção de cantos
            Mat dst = new Mat();
            Imgproc.cornerHarris(gray, dst, 2, 3, 0.04);

//            Mat imagemComCantosReconhecidos = new Mat(imagemDaRegiao.size(), CvType.CV_32FC1, Scalar.all((0)));
//            Imgproc.cornerHarris(imagemDaRegiao, imagemComCantosReconhecidos, 2, 3, 0.04);

            // Dilata imagem para atenuar ruídos pequenos
            Imgproc.dilate(dst, dst, new Mat());

////            Core.normalize(imagemComCantosReconhecidos, imagemComCantosReconhecidos, 0, 255, Core.NORM_MINMAX, CvType.CV_32FC1, new Mat());
//            imagensTemporarias2[i] = imagemComCantosReconhecidos.clone();
////            Core.convertScaleAbs(imagemComCantosReconhecidos, imagemComCantosReconhecidos);
//            imagensTemporarias3[i] = imagemComCantosReconhecidos.clone();
//            Imgproc.dilate(imagemComCantosReconhecidos, imagemComCantosReconhecidos, Mat.ones(3, 3, CvType.CV_32F));
//            imagensTemporarias4[i] = imagemComCantosReconhecidos.clone();
//            Core.MinMaxLocResult minmax = Core.minMaxLoc(imagemComCantosReconhecidos);

            // Pega ponto mais claro da imagem
            double[] red = {0, 0, 255, 0};
            double max = dst.get(0, 0)[0];
            for (int a = 0; a < dst.height(); a++) {
                for (int j = 0; j < dst.width(); j++) {
                    if (dst.get(a, j)[0] > max) {
                        max = dst.get(a, j)[0];
                    }
                }
            }
            // Threshould é 1% do máximo
            double threshouldCorner = 0.01 * max;

            // Marca tudo que é maior que o threshold de vermelho
            for (int a = 0; a < dst.height(); a++) {
                for (int j = 0; j < dst.width(); j++) {
                    if (dst.get(a, j)[0] > threshouldCorner) {
                        regionImage.put(a, j, red);
                    }
                }
            }


//            Mat foundCorners = new Mat();
//            Imgproc.cvtColor(imagemComCantosReconhecidos, foundCorners, Imgproc.COLOR_GRAY2RGBA);
//            for(int j = 0; j < foundCorners.rows(); j++) {
//                for(int k = 0; k < foundCorners.cols(); k++) {
//                    if ((int) foundCorners.get(j,k)[0] > minmax.maxVal * 0.8) {
//                        Imgproc.circle(foundCorners, new Point(j,k), 1, new Scalar(255, 0, 0), 1 ,8 , 0);
//                    }
//                }
//            }

            // Cria imagem toda preta
            Mat centerCornerImage = new Mat(100, 100, CvType.CV_8UC3);
            // byte[] blue = {(byte)255, 0, 0};
            byte[] black = {0, 0, 0};
            for (int a = 0; a < dst.height(); a++) {
                for (int j = 0; j < dst.width(); j++) {
                    centerCornerImage.put(a, j, black);
                }
            }

            // Adiciona pontos marcados a clusters de pontos
//            System.out.println(centerCornerImage);
            double smallestDistance = 999999999;
            Ponto closestPoint = new Ponto(-1, -1);
            List<PointCluster> pointClusters = new ArrayList<>();
            pointClusters.add(new PointCluster());

            for (int a = 0; a < dst.height(); a++) {
                for (int j = 0; j < dst.width(); j++) {
                    if (regionImage.get(a, j)[0] == 0 && regionImage.get(a, j)[1] == 0 && regionImage.get(a, j)[2] == 255) {
                        addPointToClosestPointCluster(new Ponto(j, a), pointClusters);
                    }
                }
            }

            List<Ponto> possibleCenters = new ArrayList<>();

            // Marca o centroide de cada cluster em azul
            byte[] blue = {(byte)255, 0, 0};
            for (PointCluster pointCluster : pointClusters) {
//                Ponto centroid = pointCluster.getCentroid();
                possibleCenters.add(pointCluster.getCentroid());
//                centerCornerImage.put(centroid.y, centroid.x, blue);
            }
//            Imgcodecs.imwrite("canto" + index + ".jpg", centerCornerImage);

            Ponto pointClosesToCenterOfRegion = getNearestPointToCenter(possibleCenters);

            if (pointClosesToCenterOfRegion != null) {
                cantosDoTabuleiro[i * 2] = pointClosesToCenterOfRegion.x;
                cantosDoTabuleiro[i * 2 + 1] = pointClosesToCenterOfRegion.y;
            }

            imagemDasregioesAoRedorDosCantosDoTabuleiro[i] = centerCornerImage;
        }

/*
Se imagemDosCantosDoTabuleiro for vazio
    Pega imagem dos cantos do tabuleiro
Fim
Pega imagem da região ao redor dos cantos do tabuleiro
Procura imagem dos cantos do tabuleiro nas imagens das regiões ao redor dos cantos do tabuleiro
Atualiza posições dos cantos de acordo com as posições encontradas
 */
    }

    private Ponto getNearestPointToCenter(List<Ponto> points) {
        Ponto center = new Ponto(50, 50);
        Ponto nearestPoint = null;
        double minimumDistance = 999999999;
        for (Ponto point : points) {
            if (point.distanceTo(center) < minimumDistance) {
                minimumDistance = point.distanceTo(center);
                nearestPoint = point;
            }
        }
        return nearestPoint;
    }

    private static void addPointToClosestPointCluster(Ponto point, List<PointCluster> pointClusters) {
        double distanceThreshouldForPointCluster = 100;
        boolean foundCluster = false;

        for (PointCluster pointCluster : pointClusters) {
            if (pointCluster.distanceTo(point) < distanceThreshouldForPointCluster) {
                pointCluster.add(point);
                foundCluster = true;
            }
        }

        if (!foundCluster) {
            PointCluster pointCluster = new PointCluster();
            pointCluster.add(point);
            pointClusters.add(pointCluster);
        }
    }

    private void recordDebugImages(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        if (DEBUG) {
            Mat imagemCameraFormatoDeCorCerto = new Mat();
            Imgproc.cvtColor(inputFrame.rgba(), imagemCameraFormatoDeCorCerto, Imgproc.COLOR_RGBA2BGR);
            Imgcodecs.imwrite(getFile("jogada" + partida.numeroDeJogadasFeitas() + "_camera", "jpg").getAbsolutePath(), imagemCameraFormatoDeCorCerto);

            for (int i = 0; i < 4; i++) {
                Mat regiaoAoRedorDoCanto = new Mat();
//                Imgproc.cvtColor(imagensTemporarias1[i], regiaoAoRedorDoCanto, Imgproc.COLOR_GRAY2BGR);
//                Imgcodecs.imwrite(getFile("temporario1_" + (i + 1), "jpg").getAbsolutePath(), regiaoAoRedorDoCanto);
//                Imgproc.cvtColor(imagensTemporarias2[i], regiaoAoRedorDoCanto, Imgproc.COLOR_GRAY2BGR);
//                Imgcodecs.imwrite(getFile("temporario2_" + (i + 1), "jpg").getAbsolutePath(), regiaoAoRedorDoCanto);
//                Imgproc.cvtColor(imagensTemporarias3[i], regiaoAoRedorDoCanto, Imgproc.COLOR_GRAY2BGR);
//                Imgcodecs.imwrite(getFile("temporario3_" + (i + 1), "jpg").getAbsolutePath(), regiaoAoRedorDoCanto);
//                Imgproc.cvtColor(imagensTemporarias4[i], regiaoAoRedorDoCanto, Imgproc.COLOR_GRAY2BGR);
//                Imgcodecs.imwrite(getFile("temporario4_" + (i + 1), "jpg").getAbsolutePath(), regiaoAoRedorDoCanto);
                Imgproc.cvtColor(imagemDasregioesAoRedorDosCantosDoTabuleiro[i], regiaoAoRedorDoCanto, Imgproc.COLOR_RGBA2BGR);
//                Imgproc.cvtColor(imagemDasregioesAoRedorDosCantosDoTabuleiro[i], regiaoAoRedorDoCanto, Imgproc.COLOR_GRAY2BGR);
                Imgcodecs.imwrite(getFile("regiao_do_canto_" + (i + 1) + "_" + partida.numeroDeJogadasFeitas(), "jpg").getAbsolutePath(), regiaoAoRedorDoCanto);
            }
        }
    }

    private void initializeCornerObjectsIfNeeded() {
        if (imagemDasregioesAoRedorDosCantosDoTabuleiro == null) {
            imagemDasregioesAoRedorDosCantosDoTabuleiro = new Mat[4];
        }
        if (imagensTemporarias1 == null) {
            imagensTemporarias1 = new Mat[4];
        }
        if (imagensTemporarias2 == null) {
            imagensTemporarias2 = new Mat[4];
        }
        if (imagensTemporarias3 == null) {
            imagensTemporarias3 = new Mat[4];
        }
        if (imagensTemporarias4 == null) {
            imagensTemporarias4 = new Mat[4];
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSalvar:
                salvarArquivoNoDiscoESair(false);
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
                atualizarBotaoDePausa();
                break;
            case R.id.btnFinalizar:
                temCertezaQueDesejaFinalizarORegisro();
                break;
            case R.id.btnSnapshot:
                tirarSnapshot();
                break;
            case R.id.btnAdicionarPedra:
                adicionarJogadaAoRegistro();
                break;
        }
    }

	private void atualizarBotaoDePausa() {
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnPausar.setImageResource(pausado ? R.drawable.play : R.drawable.pause);
            }
        });
	}

    private void temCertezaQueDesejaVoltarAUltimaJogada(String mensagem) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_tem_certeza)
                .setMessage(mensagem)
                .setPositiveButton(R.string.sim, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Jogada removida = partida.voltarUltimaJogada();
                        tempoDesdeUltimaMudancaDeTabuleiro = 0;
                        momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
                        atualizarBotaoDeVoltar();
                        adicionarAoLog("Voltando jogada " + removida + "\n\n");
                    }
                })
                .setNegativeButton(R.string.nao, null)
                .show();
    }

    private void tirarSnapshot() {
        try {
            Mat imagemFormatoDeCorCerto = new Mat();
            Imgproc.cvtColor(tabuleiroOrtogonal, imagemFormatoDeCorCerto, Imgproc.COLOR_RGBA2BGR);
            Imgcodecs.imwrite(getFile("jpg").getAbsolutePath(), imagemFormatoDeCorCerto);

            if (DEBUG) {
                FileOutputStream fos = new FileOutputStream(getFile("txt"), false);
                fos.write(snapshotAtual.getBytes());
                fos.flush();
                fos.close();
            }

            Toast.makeText(RegistrarPartidaActivity.this, "Snapshot salva no arquivo: " + getFile("jpg").getName() + ".", Toast.LENGTH_LONG).show();
            Log.i(TestesActivity.TAG, "Snapshot salva: " + getFile("txt").getName() + " com conteúdo " + snapshotAtual);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void rotacionar(int direcao) {
        int[] cantosDoTabuleiroRotacionados = new int[8];

		// Anti-horário
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
                        salvarArquivoNoDiscoESair(true);
                    }
                })
                .setNegativeButton(R.string.nao, null)
                .show();
    }

	/**
	 * Salva o arquivo da partida e o log no armazenamento secundário. Se o
	 * parâmetro 'sair' for verdadeiro, finaliza o registro depois de salvar.
	 */
	private void salvarArquivoNoDiscoESair(boolean sairDepoisDeSalvar) {
        String conteudoDaPartida = partida.sgf();
        if (isExternalStorageWritable()) {
            try {
                FileOutputStream fos = new FileOutputStream(arquivoDeRegistro, false);
                fos.write(conteudoDaPartida.getBytes());
                fos.flush();
                fos.close();

                // Isto tem que rodar na thread de UI porque a activity é fechada após o toast ser mostrado
                runOnUiThread(new Runnable() {
                    public void run() {
                    Toast.makeText(RegistrarPartidaActivity.this, "Partida salva no arquivo: " + arquivoDeRegistro.getName() + ".", Toast.LENGTH_LONG).show();
                    }
                });
                Log.i(TestesActivity.TAG, "Partida salva: " + arquivoDeRegistro.getName() + " com conteúdo " + conteudoDaPartida);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            finally {
				if (sairDepoisDeSalvar) {
                    Intent intent = new Intent(getApplicationContext(), TelaInicialActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
				}
			}
        }
        else {
            Toast.makeText(RegistrarPartidaActivity.this, "ERRO: Armazenamento externo nao disponivel.", Toast.LENGTH_LONG).show();
            Log.e(TestesActivity.TAG, "Armazenamento externo não disponível.");
        }
	}

    private void adicionarAoLog(String conteudo) {
	    if (!DEBUG) return;
        try {
            FileOutputStream fos = new FileOutputStream(arquivoDeLog, true);
            fos.write(conteudo.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    private File getFile(String extensao) {
        File arquivo = new File(pastaDeRegistro, gerarNomeDeArquivo(0, extensao));
        int contador = 1;

        while (arquivo.exists()) {
            String newFilename = gerarNomeDeArquivo(contador, extensao);
            arquivo = new File(pastaDeRegistro, newFilename);
            contador++;
        }

        return arquivo;
    }

    private File getFile(String nome, String extensao) {
        File arquivo = new File(pastaDeRegistro, gerarNomeDeArquivo(0, nome, extensao));
        int contador = 1;

        while (arquivo.exists()) {
            String newFilename = gerarNomeDeArquivo(contador, nome, extensao);
            arquivo = new File(pastaDeRegistro, newFilename);
            contador++;
        }

        return arquivo;
    }

    private File getTempFile() {
        return new File(pastaDeRegistro, "arquivo_temporario");
    }

    private String gerarNomeDeArquivo(int contadorDeNomeRepetido, String extensao) {
        // http://stackoverflow.com/questions/10203924/displaying-date-in-a-double-digit-format
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd");
        String data = sdf.format(new Date(Calendar.getInstance().getTimeInMillis()));
        String contador = "";
        if (contadorDeNomeRepetido > 0) {
            contador = "(" + contadorDeNomeRepetido + ")";
        }

        StringBuilder string = new StringBuilder();
        string.append(partida.getJogadorDeBrancas())
                .append("-")
                .append(partida.getJogadorDePretas())
                .append("_")
                .append(data)
                .append(contador)
                .append(".")
                .append(extensao);
        return string.toString();
    }

    private String gerarNomeDeArquivo(int contadorDeNomeRepetido, String nome, String extensao) {
        // http://stackoverflow.com/questions/10203924/displaying-date-in-a-double-digit-format
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd");
        String data = sdf.format(new Date(Calendar.getInstance().getTimeInMillis()));
        String contador = "";
        if (contadorDeNomeRepetido > 0) {
            contador = "(" + contadorDeNomeRepetido + ")";
        }

        StringBuilder string = new StringBuilder();
        string.append(partida.getJogadorDeBrancas())
                .append("-")
                .append(partida.getJogadorDePretas())
                .append("_")
                .append(data)
                .append("_")
                .append(nome)
                .append("_")
                .append(contador)
                .append(".")
                .append(extensao);
        return string.toString();
    }

    // TODO: Verificar se isto está sendo chamado
    @Override
    public void onBackPressed() {
        Log.d(TestesActivity.TAG, "RegistrarPartidaActivity.onBackPressed()");
	    temCertezaQueDesejaFinalizarORegisro();
    }

    private void adicionarJogadaAoRegistro() {
        AlertDialog.Builder dialogo = new AlertDialog.Builder(this);

        final EditText input = new EditText(RegistrarPartidaActivity.this);

        dialogo.setTitle(R.string.dialog_adicionar_jogada)
                .setMessage(getString(R.string.dialog_adicionar_jogada))
                .setPositiveButton(R.string.sim, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Jogada adicionadaManualmente = processarJogadaManual(input.getText().toString());
                        Tabuleiro novoTabuleiro = partida.ultimoTabuleiro().gerarNovoTabuleiroComAJogada(adicionadaManualmente);
                        if (partida.adicionarJogadaSeForValida(novoTabuleiro)) {
                            novaJogadaFoiAdicionada();
                            partida.adicionouJogadaManualmente();
                            adicionarAoLog("Jogada " + adicionadaManualmente + " foi adicionada manualmente.\n\n");
                        }
                    }
                })
                .setNegativeButton(R.string.nao, null)
                .setView(input)
                .show();
    }
    
    private Jogada processarJogadaManual(String textoJogada) {
		textoJogada = textoJogada.trim();
		if (textoJogada.length() != 3) return null;
		textoJogada = textoJogada.toLowerCase();
		int cor = textoJogada.charAt(0) == 'b' ? Tabuleiro.PEDRA_PRETA : Tabuleiro.PEDRA_BRANCA;
		int linha = textoJogada.charAt(1) - 'a';
		int coluna = textoJogada.charAt(2) - 'a';
		return new Jogada(linha, coluna, cor);
	}

    private void novaJogadaFoiAdicionada() {
        contadorDeJogadas++;
        soundPool.play(beepId, 1, 1, 0, 0, 1);
        if (contadorDeJogadas % 5 == 0) salvarArquivoNoDiscoESair(false);
        atualizarBotaoDeVoltar();
    }

	private void atualizarBotaoDeVoltar() {
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnVoltarUltimaJogada.setEnabled(partida.numeroDeJogadasFeitas() > 0);
            }
        });
	}

}








//////////////////


class Ponto {
    public int x;
    public int y;
    Ponto() {}
    Ponto(int x, int y) { this.x = x; this.y = y; }
    double distanceTo(Ponto point) {
        return (y - point.y) * (y - point.y) + (x - point.x) * (x - point.x);
    }
}

class PointCluster {
    private Ponto centroid;
    private List<Ponto> points;

    public PointCluster() {
        centroid = new Ponto(-1, -1);
        points = new ArrayList<>();
    }

    public Ponto getCentroid() { return centroid; }

    public void add(Ponto ponto) {
        points.add(ponto);
        updateCentroid();
    }

    private void updateCentroid() {
        int accumulatedY = 0;
        int accumulatedX = 0;
        for (Ponto point: points) {
            accumulatedY = accumulatedY + point.y;
            accumulatedX = accumulatedX + point.x;
        }
        centroid.y = accumulatedY / points.size();
        centroid.x = accumulatedX / points.size();
    }

    public double distanceTo(Ponto point) {
        if (centroid.y == -1) return 0;
        return centroid.distanceTo(point);
    }

}