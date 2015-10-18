package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Tabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.Desenhista;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDePedras;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDeTabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.TransformadorDeTabuleiro;

/**
 * Created by leo on 30/07/15.
 */
public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public static final String   TAG                 = "KifuRecorder";

    public static final int      VIEW_MODE_RGBA      = 0;
    public static final int      VIEW_MODE_LEO       = 1;

    private MenuItem             mItemPreviewRGBA;
    private MenuItem             mItemPreviewLeo;
    private CameraBridgeViewBase mOpenCvCameraView;

    private Mat                  mIntermediateMat;

    public static int            viewMode = VIEW_MODE_RGBA;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this);  // .getClass()
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_layout);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mItemPreviewRGBA  = menu.add("Preview RGBA");
        mItemPreviewLeo = menu.add("Detector");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemPreviewRGBA)
            viewMode = VIEW_MODE_RGBA;
        else if (item == mItemPreviewLeo)
            viewMode = VIEW_MODE_LEO;
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
        mIntermediateMat = new Mat();
    }

    public void onCameraViewStopped() {
        // Explicitly deallocate Mats
        if (mIntermediateMat != null)
            mIntermediateMat.release();

        mIntermediateMat = null;
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        Size sizeRgba = rgba.size();

        Mat rgbaInnerWindow;

        switch (MainActivity.viewMode) {
            case MainActivity.VIEW_MODE_RGBA:
                break;

            case MainActivity.VIEW_MODE_LEO:
                rgbaInnerWindow = rgba.submat(0, (int)sizeRgba.height, 0, (int)sizeRgba.width);

                Mat imagemOriginal = rgbaInnerWindow.clone();

                /*
                Mat imagem = Highgui.imread("file:///android_asset/raw/imagem1.jpg", Highgui.CV_LOAD_IMAGE_COLOR);
                Log.d(TAG, rgba.type() + " " + rgba.depth() + " " + rgba.width());
                Log.d(TAG, imagem.type() + " " + imagem.depth() + " " + imagem.width());
                Imgproc.cvtColor(imagem, imagem, Imgproc.COLOR_BGR2YUV, 4);
                return rgba;
//                Tabuleiro tabuleiro = processadorDeImagem.detectarTabuleiro(imagem);
                */

                DetectorDeTabuleiro detectorDeTabuleiro = new DetectorDeTabuleiro(true);
                detectorDeTabuleiro.setImagem(rgbaInnerWindow.clone());
                detectorDeTabuleiro.setImagemDePreview(rgbaInnerWindow);
                detectorDeTabuleiro.processar();

                if (!detectorDeTabuleiro.isProcessadoComSucesso()) {
                    break;
                }

                Mat imagemDoTabuleiroCorrigido =
                        TransformadorDeTabuleiro.transformar(
                            imagemOriginal,
                            detectorDeTabuleiro.getPosicaoDoTabuleiroNaImagem(),
                            null
                        );
                imagemDoTabuleiroCorrigido.copyTo(rgbaInnerWindow.rowRange(0, 500).colRange(0, 500));

                DetectorDePedras detectorDePedras = new DetectorDePedras();
                detectorDePedras.setDimensaoDoTabuleiro(
                        detectorDeTabuleiro.getDimensaoDoTabuleiro()
                );
                detectorDePedras.setImagemDoTabuleiro(imagemDoTabuleiroCorrigido);

                Tabuleiro tabuleiro = detectorDePedras.detectar();

                //Tabuleiro tabuleiro = processadorDeImagem.detectarTabuleiro(rgbaInnerWindow.clone());
                if (tabuleiro != null) {
                    Desenhista.desenharTabuleiro(rgbaInnerWindow, tabuleiro, 0, 500, 400);
                }

                rgbaInnerWindow.release();
                break;

        }

        return rgba;
    }

    /**
     * ProcessadorDeImagem: Responsável por processar um frame de imagem e
     *     por gerar um objeto Tabuleiro correspondente a essa imagem
     * + ProcessadorDeImagem(hierarquia) - Detecta um tabuleiro com base na
     *       hierarquia de quadrados fornecida
     *   OU
     * + ProcessadorDeImagem(Mat rgbaInnerwindow) ? Deixa a classe tabuleiro
     *       fazer todo o processamento de imagem. Melhor não, isso fere o
     *       encapsulamento
     * + Tabuleiro processar(Mat rgbaInnerWindow) - Processa o frame atual e
     *       gera um Tabuleiro
     * Na verdade, acho que é melhor deixar apenas o método processar(). O
     * Processador pode guardar informações pertinentes a tarefa de processar o
     * tabuleiro, como timestamp do último frame processado, coordenadas dos
     * pontos da borda do último tabuleiro encontrado (possivelmente para
     * comparar com o próximo encontrado e ver se houve modificação na posição
     * da câmera).
     *
     * ProcessadorDeImagem precisa ter uma referência à imagem que esta sendo
     * processada para poder realizar suas funções.
     *
     * Partida: Conjunto de jogadas que formam uma partida
     * - List<String> Jogadas;
     * + void adicionarJogada(String jogada);
     * (depois penso em como fazer jogadas alternativas)
     *
     * Tabuleiro: Representa um estado específico do jogo
     * - int[][] tabuleiro - Guarda uma configuração de tabuleiro
     * + int getPosicao(linha, coluna) - Retorna 0 se a posição estiver vazia,
     *       1 se a posição estiver ocupada por uma pedra preta, 2 se a posição
     *       estiver ocupada por uma pedra branca
     * + String diferenca(Tabuleiro t2) - Identifica a diferença entre este
     *       objeto Tabuleiro e outro, retornando a coordenada da jogada que foi
     *       realizada no formato SGF (se foi uma jogada válida)
     *
     */

}
