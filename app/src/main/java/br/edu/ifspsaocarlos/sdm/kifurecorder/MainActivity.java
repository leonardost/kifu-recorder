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
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Tabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.Desenhista;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDePedras;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDeTabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.TransformadorDeTabuleiro;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public static final String   TAG                 = "KifuRecorder";

    public static final int      VIEW_MODE_RGBA      = 0;
    public static final int      VIEW_MODE_LEO       = 1;
    public static final int      VIEW_MODE_TEST      = 2;
    private MenuItem             mItemPreviewRGBA;
    private MenuItem             mItemPreviewLeo;
    private MenuItem             mItemPreviewTest;

    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat                  mIntermediateMat;
    private Mat                  imagem1;
    private Mat                  imagem1_8uc4;

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
        mItemPreviewRGBA = menu.add("Preview RGBA");
        mItemPreviewLeo = menu.add("Detector");
        mItemPreviewTest = menu.add("Testes");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemPreviewRGBA)
            viewMode = VIEW_MODE_RGBA;
        else if (item == mItemPreviewLeo)
            viewMode = VIEW_MODE_LEO;
        else if (item == mItemPreviewTest)
            viewMode = VIEW_MODE_TEST;
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

            case MainActivity.VIEW_MODE_TEST:
                rgbaInnerWindow = rgba.submat(0, (int) sizeRgba.height, 0, (int) sizeRgba.width);

//                imagem1_8uc4 = new Mat();
                try {
                    imagem1 = Utils.loadResource(MainActivity.this, R.drawable.imagem1);
//                    imagem1.convertTo(imagem1_8uc4, CvType.CV_8UC4);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                Imgproc.cvtColor(imagem1, imagem1, Imgproc.COLOR_BGR2RGB);
                Mat alpha = new Mat(imagem1.rows(), imagem1.cols(), CvType.CV_8UC1);
                alpha.setTo(new Scalar(255));
                Mat imagem8uc4 = new Mat(imagem1.rows(), imagem1.cols(), CvType.CV_8UC4);

//                imagem1.convertTo(imagem8uc4, CvType.CV_8UC4);
//                Imgproc.cvtColor(imagem1, imagem8uc4, Imgproc.COLOR_BGR2BGRA, 4);
                List<Mat> src = new ArrayList<>();
                src.add(imagem1);
                src.add(alpha);
                List<Mat> dst = new ArrayList<>();
                dst.add(imagem8uc4);
                MatOfInt fromTo = new MatOfInt(0, 0, 1, 1, 2, 2, 3, 3);
                Core.mixChannels(src, dst, fromTo);

//                Imgproc.cvtColor(imagem8uc4, imagem8uc4, Imgproc.COLOR_BGRA2YUV_IYUV);

                Log.i(TAG, "ASDF " + rgbaInnerWindow.type() + ", " + imagem1.type() + ", " + imagem8uc4.type());
                Log.i(TAG, "ASDF (" + rgbaInnerWindow.size().width + "x" + rgbaInnerWindow.size().height + ") ("
                        + imagem1.size().width + "x" + imagem1.size().height + ") ("
                        + imagem8uc4.size().width + "x" + imagem8uc4.size().height + ")");
                Log.i(TAG, CvType.CV_8UC4 + ", ----");

//                imagem8uc4.copyTo(rgbaInnerWindow.rowRange(0, 500).colRange(0, 500));
//                imagem8uc4.copyTo(rgbaInnerWindow);

//                for (int i = 0; i < 1000; ++i) {
//                    for (int j = 0; j < 1000; ++j) {
//                        rgbaInnerWindow.put(i, j, imagem8uc4.get(i, j));
//                    }
//                }

                Log.i(TAG, "| " + asdf(imagem1.get(0, 0)) + ", " + asdf(imagem1.get(0, 1)) + ", " + asdf(imagem1.get(0, 2)));
                Log.i(TAG, "| " + asdf(imagem1.get(1, 0)) + ", " + asdf(imagem1.get(1, 1)) + ", " + asdf(imagem1.get(1, 2)));
                Log.i(TAG, "| " + asdf(imagem1.get(2, 0)) + ", " + asdf(imagem1.get(2, 1)) + ", " + asdf(imagem1.get(2, 2)));
                Log.i(TAG, "<><><>");
                Log.i(TAG, "| " + asdf(imagem8uc4.get(0, 0)) + ", " + asdf(imagem8uc4.get(0, 1)) + ", " + asdf(imagem8uc4.get(0, 2)));
                Log.i(TAG, "| " + asdf(imagem8uc4.get(1, 0)) + ", " + asdf(imagem8uc4.get(1, 1)) + ", " + asdf(imagem8uc4.get(1, 2)));
                Log.i(TAG, "| " + asdf(imagem8uc4.get(2, 0)) + ", " + asdf(imagem8uc4.get(2, 1)) + ", " + asdf(imagem8uc4.get(2, 2)));
                Log.i(TAG, "<><><>");
                Log.i(TAG, "| " + asdf(rgbaInnerWindow.get(0, 0)) + ", " + asdf(rgbaInnerWindow.get(0, 1)) + ", " + asdf(rgbaInnerWindow.get(0, 2)));
                Log.i(TAG, "| " + asdf(rgbaInnerWindow.get(1, 0)) + ", " + asdf(rgbaInnerWindow.get(1, 1)) + ", " + asdf(rgbaInnerWindow.get(1, 2)));
                Log.i(TAG, "| " + asdf(rgbaInnerWindow.get(2, 0)) + ", " + asdf(rgbaInnerWindow.get(2, 1)) + ", " + asdf(rgbaInnerWindow.get(2, 2)));

                Size newSize = new Size(rgbaInnerWindow.width(), rgbaInnerWindow.height());
                Imgproc.resize(imagem8uc4, imagem8uc4, newSize);

                return imagem8uc4;
/*
                Scalar mBlue  = new Scalar(  0,   0, 255);
                Core.circle(rgbaInnerWindow, new Point(100, 100), 50, mBlue);
                rgbaInnerWindow.release();

                break;*/
        }

        return rgba;
    }

    private double[] invert(double[] colors) {
        double[] colorsInv = new double[4];
        colorsInv[0] = colors[2];
        colorsInv[1] = colors[1];
        colorsInv[2] = colors[2];
        colorsInv[3] = 255;
        return colorsInv;
    }

    private String asdf(double[] array) {
        StringBuilder retorno = new StringBuilder("(");
        for (double v : array) {
            retorno.append(v + ", ");
        }
        retorno.append(")");
        return retorno.toString();
    }

}
