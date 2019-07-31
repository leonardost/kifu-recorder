package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.Drawer;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.InitialBoardDetector;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.boardDetector.BoardDetector;

public class DetectBoardActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    private CameraBridgeViewBase mOpenCvCameraView;
    private Button btnFixarTabuleiro;

    private int dimensaoDoTabuleiro;
    private Mat posicaoDoTabuleiroNaImagem = null;
    private MatOfPoint contornoDoTabuleiro;
    InitialBoardDetector initialBoardDetector;
    BoardDetector boardDetector;

    private String jogadorDePretas;
    private String jogadorDeBrancas;
    private String komi;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.i(TestsActivity.TAG, "OpenCV loaded successfully");
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_detectar_tabuleiro);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_surface_view1);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);

        btnFixarTabuleiro = (Button) findViewById(R.id.btnFixarTabuleiro);
        btnFixarTabuleiro.setOnClickListener(this);
        btnFixarTabuleiro.setEnabled(false);

        initialBoardDetector = new InitialBoardDetector(true);
        boardDetector = new BoardDetector();

        Intent i = getIntent();
        jogadorDePretas = i.getStringExtra("jogadorDePretas");
        jogadorDeBrancas = i.getStringExtra("jogadorDeBrancas");
        komi = i.getStringExtra("komi");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TestsActivity.TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TestsActivity.TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {}

    public void onCameraViewStopped() {}

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//        return inputFrame.rgba();

        Mat imagemFonte = inputFrame.rgba();

        initialBoardDetector.setImage(imagemFonte.clone());
        initialBoardDetector.setPreviewImage(imagemFonte);
        if (initialBoardDetector.process()) {
            posicaoDoTabuleiroNaImagem =
                    initialBoardDetector.getPositionOfBoardInImage();
            contornoDoTabuleiro = converterParaMatOfPoint(posicaoDoTabuleiroNaImagem);
            dimensaoDoTabuleiro = initialBoardDetector.getBoardDimension();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnFixarTabuleiro.setEnabled(true);
                }
            });
        }
        else if (contornoDoTabuleiro != null) {
            Drawer.drawBoardContour(imagemFonte, contornoDoTabuleiro);
        }

        return imagemFonte;
    }

    private MatOfPoint converterParaMatOfPoint(Mat posicaoDoTabuleiroNaImagem) {
        Point[] cantos = { new Point(posicaoDoTabuleiroNaImagem.get(0, 0)[0], posicaoDoTabuleiroNaImagem.get(0, 0)[1]),
                new Point(posicaoDoTabuleiroNaImagem.get(1, 0)[0], posicaoDoTabuleiroNaImagem.get(1, 0)[1]),
                new Point(posicaoDoTabuleiroNaImagem.get(2, 0)[0], posicaoDoTabuleiroNaImagem.get(2, 0)[1]),
                new Point(posicaoDoTabuleiroNaImagem.get(3, 0)[0], posicaoDoTabuleiroNaImagem.get(3, 0)[1])
        };
        contornoDoTabuleiro = new MatOfPoint(cantos);
        return contornoDoTabuleiro;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnFixarTabuleiro:
                int[] matriz = new int[8];
                matriz[0] = (int)posicaoDoTabuleiroNaImagem.get(0, 0)[0];
                matriz[1] = (int)posicaoDoTabuleiroNaImagem.get(0, 0)[1];
                matriz[2] = (int)posicaoDoTabuleiroNaImagem.get(1, 0)[0];
                matriz[3] = (int)posicaoDoTabuleiroNaImagem.get(1, 0)[1];
                matriz[4] = (int)posicaoDoTabuleiroNaImagem.get(2, 0)[0];
                matriz[5] = (int)posicaoDoTabuleiroNaImagem.get(2, 0)[1];
                matriz[6] = (int)posicaoDoTabuleiroNaImagem.get(3, 0)[0];
                matriz[7] = (int)posicaoDoTabuleiroNaImagem.get(3, 0)[1];

                Intent i = new Intent(this, RecordGameActivity.class);
                i.putExtra("jogadorDePretas", jogadorDePretas);
                i.putExtra("jogadorDeBrancas", jogadorDeBrancas);
                i.putExtra("komi", komi);
                i.putExtra("posicaoDoTabuleiroNaImagem", matriz);
                i.putExtra("dimensaoDoTabuleiro", dimensaoDoTabuleiro);
                startActivity(i);
                break;
        }
    }

}
