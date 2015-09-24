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

/**
 * Created by leo on 30/07/15.
 */
public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String  TAG                 = "KifuRecorder";

    public static final int      VIEW_MODE_RGBA      = 0;
    public static final int      VIEW_MODE_CANNY     = 1;
    public static final int      VIEW_MODE_HOUGH     = 2;
    public static final int      VIEW_MODE_LEO       = 3;

    private MenuItem mItemPreviewRGBA;
    private MenuItem             mItemPreviewCanny;
    private MenuItem             mItemPreviewHough;
    private MenuItem             mItemPreviewLeo;
    private CameraBridgeViewBase mOpenCvCameraView;

    private Mat                  mLines;
    private Mat                  mIntermediateMat;
    private Scalar               mRed;
    private ProcessadorDeImagem processadorDeImagem;

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
        mItemPreviewCanny = menu.add("Canny");
        mItemPreviewHough = menu.add("Hough");
        mItemPreviewLeo = menu.add("Leo");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemPreviewRGBA)
            viewMode = VIEW_MODE_RGBA;
        else if (item == mItemPreviewCanny)
            viewMode = VIEW_MODE_CANNY;
        else if (item == mItemPreviewHough)
            viewMode = VIEW_MODE_HOUGH;
        else if (item == mItemPreviewLeo)
            viewMode = VIEW_MODE_LEO;
        return true;
    }

    public void onCameraViewStarted(int width, int height) {
        mLines = new Mat();
        mIntermediateMat = new Mat();
        mRed = new Scalar(255, 0, 0);
        processadorDeImagem = new ProcessadorDeImagem();
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

        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;

        int left = cols / 8;
        int top = rows / 8;

        int width = cols * 3 / 4;
        int height = rows * 3 / 4;

        int threshould = 50, minLineSize = 50, lineGap = 20;

        switch (MainActivity.viewMode) {
            case MainActivity.VIEW_MODE_RGBA:
                break;

            case MainActivity.VIEW_MODE_CANNY:
                rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
                Imgproc.Canny(rgbaInnerWindow, mIntermediateMat, 80, 90);
                Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGRA, 4);
                rgbaInnerWindow.release();
                break;

            case MainActivity.VIEW_MODE_HOUGH:

                rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
                //Imgproc.Canny(rgbaInnerWindow, mIntermediateMat, 80, 90);
                Imgproc.Canny(rgbaInnerWindow, mIntermediateMat, 35, 70);
                Imgproc.HoughLinesP(mIntermediateMat, mLines, 1, Math.PI / 180, threshould, minLineSize, lineGap);
                Imgproc.cvtColor(mIntermediateMat, rgbaInnerWindow, Imgproc.COLOR_GRAY2BGR, 4);

                for (int x = 0; x < mLines.cols(); x++) {
                    double[] vec = mLines.get(0, x);
                    if (vec != null) {
                        double x1 = vec[0],
                                y1 = vec[1],
                                x2 = vec[2],
                                y2 = vec[3];
                        Point start = new Point(x1, y1);
                        Point end = new Point(x2, y2);

                        Core.line(rgbaInnerWindow, start, end, mRed, 5);
                        //Log.d(TAG, "Line from (" + x1 + ", " + y1 + ") to (" + x2 + ", " + y2 + ")");
                    }
                }

                rgbaInnerWindow.release();
                break;

            case MainActivity.VIEW_MODE_LEO:
                rgbaInnerWindow = rgba.submat(0, (int)sizeRgba.height, 0, (int)sizeRgba.width);

                /*
                Mat imagem = Highgui.imread("file:///android_asset/raw/imagem1.jpg", Highgui.CV_LOAD_IMAGE_COLOR);
                Log.d(TAG, rgba.type() + " " + rgba.depth() + " " + rgba.width());
                Log.d(TAG, imagem.type() + " " + imagem.depth() + " " + imagem.width());
                Imgproc.cvtColor(imagem, imagem, Imgproc.COLOR_BGR2YUV, 4);
                return rgba;
//                Tabuleiro tabuleiro = processadorDeImagem.detectarTabuleiro(imagem);
                */

                Tabuleiro tabuleiro = processadorDeImagem.detectarTabuleiro(rgbaInnerWindow);
                if (tabuleiro != null) {
                    tabuleiro.desenhar(rgbaInnerWindow, 0, 500, 400);
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

    /**
     * Retorna o ponto de intersecção entre duas retas.
     * @param a
     * @param b
     * @return
     */
    private Point computeIntersection(double[] a, double[] b) {
        double x1 = a[0], y1 = a[1], x2 = a[2], y2 = a[3];
        double x3 = b[0], y3 = b[1], x4 = b[2], y4 = b[3];
        double d = (x1-x2) * (y3-y4) - (y1-y2) * (x3-x4);
        if (d != 0) {
            Point pt = new Point();
            pt.x = ((x1*y2 - y1*x2) * (x3-x4) - (x1-x2) * (x3*y4 - y3*x4)) / d;
            pt.y = ((x1*y2 - y1*x2) * (y3-y4) - (y1-y2) * (x3*y4 - y3*x4)) / d;
            return pt;
        }
        else
            return new Point(-1, -1);
    }

}
