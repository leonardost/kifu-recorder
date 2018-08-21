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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Tabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.Desenhista;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDePedras;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDeTabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.TransformadorDeTabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.testes.CasoDeTeste;

public class TestesActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    public static final String   TAG                      = "KifuRecorder";

    public static final int      VIEW_MODE_RGBA           = 0;
    public static final int      VIEW_MODE_DETECTOR       = 1;
    public static final int      VIEW_MODE_TEST           = 2;
    public static final int      VIEW_MODE_AUTOMATIC_TEST = 3;
    private MenuItem             mItemPreviewRGBA;
    private MenuItem             mItemPreviewLeo;
    private MenuItem             mItemPreviewTest;
    private MenuItem             mItemPreviewAutomaticTest;

    private CameraBridgeViewBase mOpenCvCameraView;
    private Mat                  imagemProcessada;

    private static int           viewMode = VIEW_MODE_RGBA;
    private boolean              jaProcessouAImagemDeTeste = false;
    private String               numeroDeImagemTeste;
    private boolean              escolheuImagemTeste = false;
    private CasoDeTeste[]        casosDeTeste;

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

    public TestesActivity() {
        Log.i(TAG, "Instantiated new " + this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_layout);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);

        loadTestCases();
    }

    private void loadTestCases() {
        casosDeTeste = new CasoDeTeste[35];
        int currentTestCase = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.images_outputs)));
        String line;
        Pattern p = Pattern.compile("^(\\d+)#(\\d+)$");
        int dimensaoDoTabuleiro;
        int linhaAtual = 0;

        try {
            while ((line = reader.readLine()) != null) {
                if (line.contains("#")) {
                    Matcher m = p.matcher(line);
                    if (!m.matches()) {
                        break;
                    }
                    String numeroDaImagem = m.group(1);
                    String dimensao = m.group(2);

                    currentTestCase++;
                    casosDeTeste[currentTestCase - 1] = new CasoDeTeste();
                    casosDeTeste[currentTestCase - 1].setNumeroDaImagem(Integer.valueOf(numeroDaImagem));
                    dimensaoDoTabuleiro = Integer.valueOf(dimensao);
                    casosDeTeste[currentTestCase - 1].criarTabuleiroComDimensao(dimensaoDoTabuleiro);
                    linhaAtual = 0;
                }
                else {
                    for (int i = 0; i < line.length(); ++i) {
                        int valor = Tabuleiro.VAZIO;
                        if (line.charAt(i) == 'P') {
                            valor = Tabuleiro.PEDRA_PRETA;
                        }
                        else if (line.charAt(i) == 'B') {
                            valor = Tabuleiro.PEDRA_BRANCA;
                        }
                        if (valor != Tabuleiro.VAZIO) {
                            casosDeTeste[currentTestCase - 1].getTabuleiro().colocarPedra(linhaAtual, i, valor);
                        }
                    }
                    linhaAtual++;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < casosDeTeste.length; ++i) {
            Log.i(TAG, "Tabuleiro correspondente à imagem " + (i + 1) + ": ");
            if (casosDeTeste[i] != null) {
                Log.i(TAG, "" + casosDeTeste[i].getTabuleiro());
            }
        }
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
        mItemPreviewAutomaticTest = menu.add("Testes automáticos");
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
        else if (item == mItemPreviewAutomaticTest) {
            viewMode = VIEW_MODE_AUTOMATIC_TEST;
        }
        return true;
    }

    private void escolherImagemDeTeste() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Digite um número de imagem (1-35)");

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

    }

    public void onCameraViewStopped() {

    }

    /**
     * Método chamado a cada instante em que há um novo frame de preview da câmera.
     *
     * @param inputFrame
     * @return
     */
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();

        if (TestesActivity.viewMode == VIEW_MODE_RGBA) return rgba;

        if (TestesActivity.viewMode == VIEW_MODE_TEST && escolheuImagemTeste == false) return rgba;

        if (TestesActivity.viewMode == VIEW_MODE_TEST && jaProcessouAImagemDeTeste) {
            return imagemProcessada;
        }

        if (TestesActivity.viewMode == VIEW_MODE_AUTOMATIC_TEST) {

            for (int indiceImagem = 1; indiceImagem <= 35; ++indiceImagem) {
                Mat imagemLidaDeArquivo = lerImagemDosResourcesAdaptada(
                        resourceSelecionado(new Integer(indiceImagem).toString()), rgba.size());
                Mat imagemFonte = imagemLidaDeArquivo.clone();
                Mat imagemOriginal = imagemFonte.clone();

                // Detecção do tabuleiro
                DetectorDeTabuleiro detectorDeTabuleiro = new DetectorDeTabuleiro(true);
                detectorDeTabuleiro.setImagem(imagemFonte.clone());
                detectorDeTabuleiro.setImagemDePreview(imagemFonte);
                if (!detectorDeTabuleiro.processar()) {
//                    return imagemFonte;
                    continue;
                }

                // Transformação da imagem do tabuleiro para a posição ortogonal
                Mat imagemDoTabuleiroCorrigido =
                        TransformadorDeTabuleiro.transformarOrtogonalmente(
                                imagemOriginal,
                                detectorDeTabuleiro.getPosicaoDoTabuleiroNaImagem()
                        );
                imagemDoTabuleiroCorrigido.copyTo(imagemFonte.rowRange(0, 500).colRange(0, 500));

                // Detecção das pedras
                DetectorDePedras detectorDePedras = new DetectorDePedras();
                detectorDePedras.setDimensaoDoTabuleiro(detectorDeTabuleiro.getDimensaoDoTabuleiro());
                detectorDePedras.setImagemDoTabuleiro(imagemDoTabuleiroCorrigido);

                Tabuleiro tabuleiro = detectorDePedras.detectar();
                if (tabuleiro != null && tabuleiro.equals(casosDeTeste[indiceImagem - 1].getTabuleiro())) {
//                    Desenhista.desenharTabuleiro(imagemFonte, tabuleiro, 0, 500, 400);
                    Log.i(TAG, "Caso de teste #" + indiceImagem + " passou!");
                }
                else {
//                    Desenhista.desenharTabuleiro(imagemFonte, tabuleiro, 0, 500, 400);
                    Log.i(TAG, "Caso de teste #" + indiceImagem + " falhou:");
                    Log.i(TAG, "Caso de teste #" + indiceImagem + " " + tabuleiro);
                    Log.i(TAG, "não bateu com");
                    Log.i(TAG, "Caso de teste #" + indiceImagem + " " + casosDeTeste[indiceImagem - 1].getTabuleiro());
                }
            }
            rgba.release();
            return rgba;
        }

        Mat imagemLidaDeArquivo = new Mat();
        if (TestesActivity.viewMode == VIEW_MODE_TEST) {
            imagemLidaDeArquivo = lerImagemDosResourcesAdaptada(resourceSelecionado(numeroDeImagemTeste), rgba.size());
        };

        Mat imagemFonte = (TestesActivity.viewMode == VIEW_MODE_DETECTOR) ?
                rgba.clone() :
                imagemLidaDeArquivo.clone();
        Mat imagemOriginal = imagemFonte.clone();

        // Detecção do tabuleiro
        DetectorDeTabuleiro detectorDeTabuleiro = new DetectorDeTabuleiro(true);
        detectorDeTabuleiro.setImagem(imagemFonte.clone());
        detectorDeTabuleiro.setImagemDePreview(imagemFonte);
        if (!detectorDeTabuleiro.processar()) {
            return imagemFonte;
        }

        // Cria uma cópia da imagem em preto e branco
//        Mat imagemOriginalPretoEBranco = imagemOriginal.clone();
//        Imgproc.cvtColor(imagemOriginalPretoEBranco, imagemOriginalPretoEBranco, Imgproc.COLOR_RGB2GRAY);
//        Imgproc.cvtColor(imagemOriginalPretoEBranco, imagemOriginalPretoEBranco, Imgproc.COLOR_GRAY2RGB);

        // Transformação da imagem do tabuleiro para a posição ortogonal
        Mat imagemDoTabuleiroCorrigido =
                TransformadorDeTabuleiro.transformarOrtogonalmente(
                    imagemOriginal,
//                    imagemOriginalPretoEBranco,
                    detectorDeTabuleiro.getPosicaoDoTabuleiroNaImagem()
                );

        /*
        Imgproc.cvtColor(imagemDoTabuleiroCorrigido, imagemDoTabuleiroCorrigido, Imgproc.COLOR_RGB2GRAY);
        MaTomatet circulos = new Mat();
        Imgproc.HoughCircles(imagemDoTabuleiroCorrigido, circulos, Imgproc.CV_HOUGH_GRADIENT, 1, 10, 200, 100, 0, 0);
        float circle[] = new float[3];
        for (int i = 0; i < circulos.cols(); ++i) {
            circulos.get(0, i, circle);
            Point center = new Point();
            center.x = circle[0];
            center.y = circle[1];
            Core.circle(imagemDoTabuleiroCorrigido, center, (int)circle[2], new Scalar(255, 255, 0), 3);
        }
        Imgproc.cvtColor(imagemDoTabuleiroCorrigido, imagemDoTabuleiroCorrigido, Imgproc.COLOR_GRAY2RGB);
*/

        imagemDoTabuleiroCorrigido.copyTo(imagemFonte.rowRange(0, 500).colRange(0, 500));

        // Detecção das pedras
        DetectorDePedras detectorDePedras = new DetectorDePedras();
        detectorDePedras.setDimensaoDoTabuleiro(detectorDeTabuleiro.getDimensaoDoTabuleiro());
        detectorDePedras.setImagemDoTabuleiro(imagemDoTabuleiroCorrigido);

        Tabuleiro tabuleiro = detectorDePedras.detectar();
        if (tabuleiro != null) {
            Desenhista.desenharTabuleiro(imagemFonte, tabuleiro, 0, 500, 400, null);
        }

        imagemProcessada = imagemFonte;
        jaProcessouAImagemDeTeste = true;

        rgba.release();
        return imagemFonte;
    }

    private int resourceSelecionado(String texto) {
        switch (texto) {
            default: return 0;
            /*case "1": return R.drawable.imagem1;
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
            case "19": return R.drawable.imagem19;
            case "20": return R.drawable.imagem20;
            case "21": return R.drawable.imagem21;
            case "22": return R.drawable.imagem22;
            case "23": return R.drawable.imagem23;
            case "24": return R.drawable.imagem24;
            case "25": return R.drawable.imagem25;
            case "26": return R.drawable.imagem26;
            case "27": return R.drawable.imagem27;
            case "28": return R.drawable.imagem28;
            case "29": return R.drawable.imagem29;
            case "30": return R.drawable.imagem30;
            case "31": return R.drawable.imagem31;
            case "32": return R.drawable.imagem32;
            case "33": return R.drawable.imagem33;
            case "34": return R.drawable.imagem34;
            case "35": return R.drawable.imagem35;
            default: return R.drawable.imagem1;*/
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
            imagemLidaDeArquivo = Utils.loadResource(TestesActivity.this, idDaImagemResource);
            // Converte do formato BGR (usado pelo OpenCV) para o RGB
            Imgproc.cvtColor(imagemLidaDeArquivo, imagemLidaDeArquivo, Imgproc.COLOR_BGR2RGB);
            Log.i(TAG, "Tamanho da imagem = " + tamanhoDaImagem.height + "x" + tamanhoDaImagem.width);
            Imgproc.resize(imagemLidaDeArquivo, imagemLidaDeArquivo, tamanhoDaImagem);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return imagemLidaDeArquivo;
    }

}
