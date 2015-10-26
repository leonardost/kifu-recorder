package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Tabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.Desenhista;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDePedras;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDeTabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.TransformadorDeTabuleiro;

public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public static final String   TAG                 = "KifuRecorder";

    public static final int      VIEW_MODE_RGBA      = 0;
    public static final int      VIEW_MODE_DETECTOR  = 1;
    public static final int      VIEW_MODE_TEST      = 2;
    private MenuItem             mItemPreviewRGBA;
    private MenuItem             mItemPreviewLeo;
    private MenuItem             mItemPreviewTest;

    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat                  imagemProcessada;

    private static int           viewMode = VIEW_MODE_RGBA;
    private boolean              jaProcessouAImagemDeTeste = false;
    private String               numeroDeImagemTeste;
    private boolean              escolheuImagemTeste = false;

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
        Log.i(TAG, "Instantiated new " + this);
    }

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
            viewMode = VIEW_MODE_DETECTOR;
        else if (item == mItemPreviewTest) {
            escolheuImagemTeste = false;
            escolherImagemDeTeste();
            jaProcessouAImagemDeTeste = false;
            viewMode = VIEW_MODE_TEST;
        }
        return true;
    }

    private void escolherImagemDeTeste() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Digite um número de imagem (1-18)");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                numeroDeImagemTeste = input.getText().toString();
                escolheuImagemTeste = true;
            }
        });

        builder.show();
    }

    public void onCameraViewStarted(int width, int height) {
//        mIntermediateMat = new Mat();
    }

    public void onCameraViewStopped() {
        // Explicitly deallocate Mats
//        if (mIntermediateMat != null)
//            mIntermediateMat.release();
//        mIntermediateMat = null;
    }

    /**
     * Método chamado a cada instante em que há um novo frame de preview da câmera.
     *
     * @param inputFrame
     * @return
     */
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();

        if (MainActivity.viewMode == VIEW_MODE_RGBA) return rgba;

        if (MainActivity.viewMode == VIEW_MODE_TEST && escolheuImagemTeste == false) return rgba;

        if (MainActivity.viewMode == VIEW_MODE_TEST && jaProcessouAImagemDeTeste) {
            return imagemProcessada;
        }

        Mat imagemLidaDeArquivo = new Mat();
        if (MainActivity.viewMode == VIEW_MODE_TEST) {
            imagemLidaDeArquivo = lerImagemDosResourcesAdaptada(resourceSelecionado(numeroDeImagemTeste), rgba.size());
        };

        Mat imagemFonte = (MainActivity.viewMode == VIEW_MODE_DETECTOR) ?
                rgba.clone() :
                imagemLidaDeArquivo.clone();
        Mat imagemOriginal = imagemFonte.clone();

        DetectorDeTabuleiro detectorDeTabuleiro = new DetectorDeTabuleiro(true);
        detectorDeTabuleiro.setImagem(imagemFonte.clone());
        detectorDeTabuleiro.setImagemDePreview(imagemFonte);
        if (!detectorDeTabuleiro.processar()) {
            return imagemFonte;
        }

        Mat imagemDoTabuleiroCorrigido =
                TransformadorDeTabuleiro.transformar(
                    imagemOriginal,
                    detectorDeTabuleiro.getPosicaoDoTabuleiroNaImagem(),
                    null
                );
        imagemDoTabuleiroCorrigido.copyTo(imagemFonte.rowRange(0, 500).colRange(0, 500));

        DetectorDePedras detectorDePedras = new DetectorDePedras();
        detectorDePedras.setDimensaoDoTabuleiro(detectorDeTabuleiro.getDimensaoDoTabuleiro());
        detectorDePedras.setImagemDoTabuleiro(imagemDoTabuleiroCorrigido);

        Tabuleiro tabuleiro = detectorDePedras.detectar();
        if (tabuleiro != null) {
            Desenhista.desenharTabuleiro(imagemFonte, tabuleiro, 0, 500, 400);
        }

        imagemProcessada = imagemFonte;
        jaProcessouAImagemDeTeste = true;

        rgba.release();
        return imagemFonte;
    }

    private int resourceSelecionado(String texto) {
        switch (texto) {
            case "1": return R.drawable.imagem1;
            case "2": return R.drawable.imagem2;
            case "3": return R.drawable.imagem3;
            case "4": return R.drawable.imagem4;
            case "5": return R.drawable.imagem5;
            case "6": return R.drawable.imagem6;
            case "7": return R.drawable.imagem7;
            case "8": return R.drawable.imagem8;
            case "9": return R.drawable.imagem9;
            case "10": return R.drawable.imagem10;
            case "11": return R.drawable.imagem11;
            case "12": return R.drawable.imagem12;
            case "13": return R.drawable.imagem13;
            case "14": return R.drawable.imagem14;
            case "15": return R.drawable.imagem15;
            case "16": return R.drawable.imagem16;
            case "17": return R.drawable.imagem17;
            case "18": return R.drawable.imagem18;
            default: return R.drawable.imagem1;
        }
    }

    /**
     * Lê uma imagem dos arquivos de resource do projeto
     * @param idDaImagemResource
     * @param tamanhoDaImagem
     * @return Mat contendo a imagem JPG lida dos arquivos de resource do projeto com o tamanho
     * especificado
     */
    private Mat lerImagemDosResourcesAdaptada(int idDaImagemResource, Size tamanhoDaImagem) {
        Mat imagemLidaDeArquivo = new Mat();
        try {
            imagemLidaDeArquivo = Utils.loadResource(MainActivity.this, idDaImagemResource);
            // Converte do formato BGR (usado pelo OpenCV) para o RGB
            Imgproc.cvtColor(imagemLidaDeArquivo, imagemLidaDeArquivo, Imgproc.COLOR_BGR2RGB);
            Imgproc.resize(imagemLidaDeArquivo, imagemLidaDeArquivo, tamanhoDaImagem);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imagemLidaDeArquivo;
    }

}
