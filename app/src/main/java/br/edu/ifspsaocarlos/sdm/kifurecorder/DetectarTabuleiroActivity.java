package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;

import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.Desenhista;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDeTabuleiro;

public class DetectarTabuleiroActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    private CameraBridgeViewBase mOpenCvCameraView;
    private Button btnFixarTabuleiro;

    private int dimensaoDoTabuleiro;
    private Mat posicaoDoTabuleiroNaImagem = null;
    private MatOfPoint contornoDoTabuleiro;
    DetectorDeTabuleiro detectorDeTabuleiro;

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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_detectar_tabuleiro);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_surface_view1);
        mOpenCvCameraView.setCvCameraViewListener(this);

        btnFixarTabuleiro = (Button) findViewById(R.id.btnFixarTabuleiro);
        btnFixarTabuleiro.setOnClickListener(this);
        btnFixarTabuleiro.setEnabled(false);

        detectorDeTabuleiro = new DetectorDeTabuleiro(true);
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

        detectorDeTabuleiro.setImagem(imagemFonte.clone());
        detectorDeTabuleiro.setImagemDePreview(imagemFonte);
        if (detectorDeTabuleiro.processar()) {
            posicaoDoTabuleiroNaImagem =
                    detectorDeTabuleiro.getPosicaoDoTabuleiroNaImagem();
            contornoDoTabuleiro = converterParaMatOfPoint(posicaoDoTabuleiroNaImagem);
            dimensaoDoTabuleiro = detectorDeTabuleiro.getDimensaoDoTabuleiro();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    btnFixarTabuleiro.setEnabled(true);
                }
            });
        }
        else if (contornoDoTabuleiro != null) {
            Desenhista.desenharContornoDoTabuleiro(imagemFonte, contornoDoTabuleiro);
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

                Intent i = new Intent(this, RegistrarPartidaActivity.class);
                i.putExtra("posicaoDoTabuleiroNaImagem", matriz);
                i.putExtra("dimensaoDoTabuleiro", dimensaoDoTabuleiro);
                startActivity(i);
                break;
        }
    }

}
