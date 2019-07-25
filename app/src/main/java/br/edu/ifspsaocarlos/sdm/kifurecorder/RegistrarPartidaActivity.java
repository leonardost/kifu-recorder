package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Jogada;
import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Partida;
import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Tabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.Desenhista;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.DetectorDePedras;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.FileHelper;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.ImageUtils;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.Logger;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.LoggingConfiguration;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.Ponto;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.boardDetector.BoardDetector;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.cornerDetector.Corner;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.cornerDetector.CornerDetector;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.similarityCalculator.FingerprintMatching;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.similarityCalculator.SimilarityCalculatorInterface;

public class RegistrarPartidaActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    public static int STATE_RUNNING = 1;
    public static int STATE_LOOKING_FOR_BOARD = 2;
    private static int MOVEMENT_THRESHOLD = 10;
    private int state;

    Logger logger;
    FileHelper fileHelper;

    BoardDetector boardDetector = new BoardDetector();
    DetectorDePedras detectorDePedras = new DetectorDePedras();
    CornerDetector[] cornerDetector;
    Partida partida;
    Tabuleiro ultimoTabuleiroDetectado;

    int contadorDeJogadas = 0;                 // A cada 5 jogadas feitas a partida é salva automaticamente
    long tempoLimite = 2000;                   // Tempo que um tabuleiro detectado deve se manter inalterado para que seja considerado pelo detector
    long momentoDaUltimaDeteccaoDeTabuleiro;
    long tempoDesdeUltimaMudancaDeTabuleiro;
    boolean paused = false;
    long momentoDoUltimoProcessamentoDeImagem;
    long tempoDesdeUltimoProcessamentoDeImagem;
    boolean isCornerTrackingActive = true;
    Mat lastValidOrtogonalBoardImage = null;
    SimilarityCalculatorInterface fingerprintMatching = new FingerprintMatching();

    // This array stores the number of frames that each corner has stayed without
    // ellipses being detected over them. This is used for false positive checking
    int[] numberOfFramesWithoutStone = { 0, 0, 0, 0 };
    // This is also used to bring the detector back when it's strayed too far away
    int numberOfFramesWithDissimilarOrtogonalImages = 0;

    // Domain objects
    int dimensaoDoTabuleiro;                   // 9x9, 13x13 ou 19x19
    Corner[] boardCorners;
    Corner[] originalBoardCorners;
    Mat cameraFrame;
    Mat posicaoDoTabuleiroNaImagem;            // Matriz que contem os cantos do tabuleiro
    Mat tabuleiroOrtogonal;                    // Imagem do tabuleiro transformado em visão ortogonal
	MatOfPoint contornoDoTabuleiro;

    private ImageButton btnSalvar;
    private ImageButton btnVoltarUltimaJogada;
    private ImageButton btnRotacionarEsquerda;
    private ImageButton btnRotacionarDireita;
    private ImageButton btnPausar;
    private ImageButton btnSnapshot;
    private ImageButton btnAdicionarJogada;
    private ImageButton btnToggleCornerTracking;
    private ImageButton btnResetBoardPosition;
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
        state = STATE_RUNNING;

        initializeCamera();
        initializeProcessing();
        initializeUserInterface();
        initializeLogging();

        Log.i(TestesActivity.TAG, "onCreate() finalizado");
    }

    private void initializeCamera() {
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_registro);
//        mOpenCvCameraView.setMaxFrameSize(1000, 1000);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    private void initializeProcessing() {
        Intent i = getIntent();
        String jogadorDePretas  = i.getStringExtra("jogadorDePretas");
        String jogadorDeBrancas = i.getStringExtra("jogadorDeBrancas");
        String komi             = i.getStringExtra("komi");
        dimensaoDoTabuleiro     = i.getIntExtra("dimensaoDoTabuleiro", -1);
        int[] cantosDoTabuleiroEncontrados = i.getIntArrayExtra("posicaoDoTabuleiroNaImagem");
        detectorDePedras.setDimensaoDoTabuleiro(dimensaoDoTabuleiro);

        partida = new Partida(dimensaoDoTabuleiro, jogadorDePretas, jogadorDeBrancas, komi);
        ultimoTabuleiroDetectado = new Tabuleiro(dimensaoDoTabuleiro);
        momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
        tempoDesdeUltimaMudancaDeTabuleiro = 0;
        momentoDoUltimoProcessamentoDeImagem = SystemClock.elapsedRealtime();
        tempoDesdeUltimoProcessamentoDeImagem = 0;

        originalBoardCorners = new Corner[4];
        originalBoardCorners[0] = new Corner(cantosDoTabuleiroEncontrados[0], cantosDoTabuleiroEncontrados[1]);
        originalBoardCorners[1] = new Corner(cantosDoTabuleiroEncontrados[2], cantosDoTabuleiroEncontrados[3]);
        originalBoardCorners[2] = new Corner(cantosDoTabuleiroEncontrados[4], cantosDoTabuleiroEncontrados[5]);
        originalBoardCorners[3] = new Corner(cantosDoTabuleiroEncontrados[6], cantosDoTabuleiroEncontrados[7]);
        boardCorners = new Corner[4];
        boardCorners[0] = new Corner(cantosDoTabuleiroEncontrados[0], cantosDoTabuleiroEncontrados[1]);
        boardCorners[1] = new Corner(cantosDoTabuleiroEncontrados[2], cantosDoTabuleiroEncontrados[3]);
        boardCorners[2] = new Corner(cantosDoTabuleiroEncontrados[4], cantosDoTabuleiroEncontrados[5]);
        boardCorners[3] = new Corner(cantosDoTabuleiroEncontrados[6], cantosDoTabuleiroEncontrados[7]);
        cornerDetector = new CornerDetector[4];
        for (int cornerIndex = 0; cornerIndex < 4; cornerIndex++) {
            cornerDetector[cornerIndex] = new CornerDetector();
            cornerDetector[cornerIndex].setCorner(boardCorners[cornerIndex]);
            cornerDetector[cornerIndex].setCornerIndex(cornerIndex + 1);
        }
        processBoardCorners();
    }

    private void initializeUserInterface() {
        btnSalvar = findViewById(R.id.btnSalvar);
        btnSalvar.setOnClickListener(this);
        btnSalvar.setEnabled(true);
        btnVoltarUltimaJogada = findViewById(R.id.btnVoltarUltimaJogada);
        btnVoltarUltimaJogada.setOnClickListener(this);
        btnVoltarUltimaJogada.setEnabled(false);
        btnRotacionarEsquerda = findViewById(R.id.btnRotacionarEsquerda);
        btnRotacionarEsquerda.setOnClickListener(this);
        btnRotacionarDireita = findViewById(R.id.btnRotacionarDireita);
        btnRotacionarDireita.setOnClickListener(this);
        btnPausar = findViewById(R.id.btnPausar);
        btnPausar.setOnClickListener(this);
        btnFinalizar = findViewById(R.id.btnFinalizar);
        btnFinalizar.setOnClickListener(this);
        btnSnapshot = findViewById(R.id.btnSnapshot);
        btnSnapshot.setOnClickListener(this);
        btnAdicionarJogada = findViewById(R.id.btnAdicionarPedra);
        btnAdicionarJogada.setOnClickListener(this);
        btnToggleCornerTracking = findViewById(R.id.btnToggleCornerTracking);
        btnToggleCornerTracking.setOnClickListener(this);
        btnResetBoardPosition = findViewById(R.id.btnResetBoardPosition);
        btnResetBoardPosition.setOnClickListener(this);

        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
        beepId = soundPool.load(this, R.raw.beep, 1);
    }

    private void initializeLogging() {
        LoggingConfiguration.activateLogging();
        LoggingConfiguration.activateLogging(LoggingConfiguration.RAW_CAMERA_IMAGE);
        LoggingConfiguration.activateLogging(LoggingConfiguration.CAMERA_IMAGE_WITH_BOARD_CONTOUR);
        LoggingConfiguration.activateLogging(LoggingConfiguration.ORTOGONAL_BOARD_IMAGE);
        LoggingConfiguration.activateLogging(LoggingConfiguration.CORNER_POSITIONS);
        LoggingConfiguration.activateLogging(LoggingConfiguration.NUMBER_OF_QUADRILATERALS_FOUND_BY_BOARD_DETECTOR);

        fileHelper = new FileHelper(partida);
        logger = new Logger(partida, fileHelper);

        for (int cornerIndex = 0; cornerIndex < 4; cornerIndex++) {
            cornerDetector[cornerIndex].setFileHelper(fileHelper);
        }
    }

    private void processBoardCorners() {
        Point[] cornerPoints = new Point[4];
        for (int i = 0; i < 4; i++) {
            Ponto ponto = boardCorners[i].getRealCornerPosition();
            cornerPoints[i] = new Point(ponto.x, ponto.y);
        }

        posicaoDoTabuleiroNaImagem = new Mat(4, 1, CvType.CV_32FC2);
        posicaoDoTabuleiroNaImagem.put(0, 0,
                cornerPoints[0].x, cornerPoints[0].y,
                cornerPoints[1].x, cornerPoints[1].y,
                cornerPoints[2].x, cornerPoints[2].y,
                cornerPoints[3].x, cornerPoints[3].y);

        contornoDoTabuleiro = new MatOfPoint(cornerPoints);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstaceState) {
        super.onRestoreInstanceState(savedInstaceState);
        fileHelper.restoreGameStoredTemporarily(partida, boardCorners);
        processBoardCorners();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TestesActivity.TAG, "RegistrarPartidaActivity.onPause");
        fileHelper.storeGameTemporarily(partida, boardCorners);
        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }
    }

    @Override
    public void onStop()
    {
        super.onStop();
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
     * This method is called every time a camera frame is ready to be processed
     */
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        logger.startLoggingFrame();
        logger.setCameraFrame(inputFrame.rgba().clone());

        cameraFrame = inputFrame.rgba();
        Mat originalImage = inputFrame.rgba();

        if (isCornerTrackingActive) {
            for (int i = 0; i < 4; i++) {
                cornerDetector[i].setImageIndex((int)logger.getFrameNumber());
            }
            updateCornerPositions(originalImage.clone());
        }

        if (state == STATE_LOOKING_FOR_BOARD) {
            logger.addToLog("Board is not inside contour");
            logger.addToLog("");
            if (tabuleiroOrtogonal != null) {
                tabuleiroOrtogonal.copyTo(originalImage.rowRange(0, 500).colRange(0, 500));
            }
            Desenhista.drawLostBoardContour(originalImage, contornoDoTabuleiro);
            Desenhista.desenharTabuleiro(originalImage, partida.ultimoTabuleiro(), 0, 500, 400, partida.ultimaJogada());
            logger.log();
            return originalImage;
        }

        // Throttling, processes twice per second
        if (System.currentTimeMillis() - momentoDoUltimoProcessamentoDeImagem < 500) {
            if (tabuleiroOrtogonal != null) {
                tabuleiroOrtogonal.copyTo(originalImage.rowRange(0, 500).colRange(0, 500));
            }
            Desenhista.desenharContornoDoTabuleiro(originalImage, contornoDoTabuleiro);
            Desenhista.desenharTabuleiro(originalImage, partida.ultimoTabuleiro(), 0, 500, 400, partida.ultimaJogada());
            logger.log();
            return originalImage;
        }
        else momentoDoUltimoProcessamentoDeImagem = System.currentTimeMillis();

        tabuleiroOrtogonal = ImageUtils.transformarOrtogonalmente(originalImage, posicaoDoTabuleiroNaImagem);
        logger.setOrtogonalBoardImage(tabuleiroOrtogonal.clone());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Verificar qual o tamanho da imagem do tabuleiro ortogonal aqui!!!
        //       Isso traz implicações para o raio calculado no método de calcular a cor média ao redor de uma posição
        // int larguraImagem = (int)tabuleiroOrtogonal.size().width;
        // int alturaImagem = (int)tabuleiroOrtogonal.size().height;
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        detectorDePedras.setImagemDoTabuleiro(tabuleiroOrtogonal);

        Tabuleiro tabuleiro = detectorDePedras.detectar(
                partida.ultimoTabuleiro(),
                partida.proximaJogadaPodeSer(Tabuleiro.PEDRA_PRETA),
                partida.proximaJogadaPodeSer(Tabuleiro.PEDRA_BRANCA)
        );

//        snapshotAtual = detectorDePedras.snapshot.toString();
        logger.logCurrentBoardState();

        if (!paused) {

            if (ultimoTabuleiroDetectado.equals(tabuleiro)) {
                tempoDesdeUltimaMudancaDeTabuleiro += SystemClock.elapsedRealtime() - momentoDaUltimaDeteccaoDeTabuleiro;
                momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
                if (tempoDesdeUltimaMudancaDeTabuleiro > tempoLimite && partida.adicionarJogadaSeForValida(tabuleiro)) {
					novaJogadaFoiAdicionada();
                }
            } else {
                tempoDesdeUltimaMudancaDeTabuleiro = 0;
                momentoDaUltimaDeteccaoDeTabuleiro = SystemClock.elapsedRealtime();
            }

        }

        ultimoTabuleiroDetectado = tabuleiro;

        Desenhista.desenharContornoDoTabuleiro(originalImage, contornoDoTabuleiro);
        logger.setCameraImageWithBoardContour(originalImage.clone());

        // Desenha o tabuleiro ortogonal na tela
        if (tabuleiroOrtogonal != null) {
            tabuleiroOrtogonal.copyTo(originalImage.rowRange(0, 500).colRange(0, 500));
        }

        if (paused) {
            // Quando está pausado, desenha a saída atual do detector de pedras (útil para debugar)
            Desenhista.desenharTabuleiro(originalImage, tabuleiro, 0, 500, 400, null);
        }
        else {
            Desenhista.desenharTabuleiro(originalImage, partida.ultimoTabuleiro(), 0, 500, 400, partida.ultimaJogada());
        }

        logger.log();

        return originalImage;
    }

    private void updateCornerPositions(Mat image) {
        Imgproc.cvtColor(image, image, Imgproc.COLOR_RGBA2BGR);

        Corner[] possibleNewCorners = new Corner[4];
        boolean wereAllCornersFound = true;
        for (int i = 0; i < 4; i++) {
            long start = System.currentTimeMillis();
            possibleNewCorners[i] = cornerDetector[i].detectCornerIn(image);
            long duration = System.currentTimeMillis() - start;
            if (possibleNewCorners[i] != null) {
                logger.addToLog(possibleNewCorners[i].toString());
            } else {
                logger.addToLog("Corner " + i + " was not found");
            }
            logger.addToLog("Time to process corner " + i + " = " + duration + "ms");
            if (possibleNewCorners[i] == null) {
                wereAllCornersFound = false;
            }
        }

        Mat ortogonalBoardImage = wereAllCornersFound
            ? ImageUtils.generateOrtogonalBoardImage(image, possibleNewCorners)
            : null;

        if (wereAllCornersFound && boardDetector.isBoardContainedIn(ortogonalBoardImage)) {
            logger.addToLog("Board is inside countour");
            int numberOfCornersThatMoved = getNumberOfCornersThatMoved(possibleNewCorners, boardCorners);
            logger.addToLog("Number of corners that moved: " + numberOfCornersThatMoved);
            int numberOfEmptyCornersThatMoved = getNumberOfEmptyCornersThatMoved(possibleNewCorners, boardCorners);
            logger.addToLog("Number of empty corners that moved: " + numberOfEmptyCornersThatMoved);
            double[] distanceToNewPoint = new double[4];
            for (int i = 0; i < 4; i++) {
                distanceToNewPoint[i] = possibleNewCorners[i].distanceTo(boardCorners[i]);
                logger.addToLog("Distance to old corner point " + (i + 1) + " = " + distanceToNewPoint[i]);
            }

            for (int i = 0; i < 4; i++) {
                if (numberOfCornersThatMoved < 4) {
                    // Not all corners moved, so this is probably a corner adjustment
                    // Update relative corner position of possible corners with stones
                    if (possibleNewCorners[i].isStone) {
                        if (!boardCorners[i].isStone) {
                            possibleNewCorners[i].updateDisplacementVectorRelativeTo(boardCorners[i].position);
                        } else {
                            possibleNewCorners[i].updateDisplacementVectorRelativeTo(boardCorners[i].getRealCornerPosition());
                        }
                    }
                } else if (possibleNewCorners[i].isStone) {
                    // All corners moved together, so this is probably a board displacement and we
                    // don't update the corners's relative position to the real corners
                    possibleNewCorners[i].displacementToRealCorner = boardCorners[i].displacementToRealCorner;
                }
            }

            Mat ortogonalBoardImage2 = ImageUtils.generateOrtogonalBoardImage(image, possibleNewCorners);
            double similarity = lastValidOrtogonalBoardImage != null ? fingerprintMatching.calculateSimilatiryBetween(lastValidOrtogonalBoardImage, ortogonalBoardImage2) : -1;
            logger.addToLog("Similarity between new ortogonal board image to last valid one = " + similarity);
            logger.setOrtogonalBoardImage2(ortogonalBoardImage2);

            if (logger.getFrameNumber() <= 3 || numberOfFramesWithDissimilarOrtogonalImages >= 5 || fingerprintMatching.areImagesSimilar(lastValidOrtogonalBoardImage, ortogonalBoardImage2)) {
                // This condition should be time based and not frame based
                if (numberOfFramesWithDissimilarOrtogonalImages >= 5) {
                    logger.addToLog("Forcing ortogonal image to be similar");
                } else {
                    logger.addToLog("New ortogonal board image is similar to last valid one");
                }
                for (int i = 0; i < 4; i++) {
                    if (!possibleNewCorners[i].isStone) {
                        numberOfFramesWithoutStone[i]++;
                    } else {
                        numberOfFramesWithoutStone[i] = 0;
                    }

                    if (!boardCorners[i].isStone && !possibleNewCorners[i].isStone && numberOfCornersThatMoved < 3 && numberOfEmptyCornersThatMoved == 1) {
                        // This means a single empty corner moved by itself, which is not possible. This addresses a wrong
                        // corner detection in frame 70 of sequence 16.
                        logger.addToLog("Corner " + i + " - This empty corner moved by itself");
                        continue;
                    }
                    if (!possibleNewCorners[i].isStone && boardCorners[i].isStone && possibleNewCorners[i].distanceTo(boardCorners[i].getRealCornerPosition()) > MOVEMENT_THRESHOLD
                            // This condition should be time based instead of frame based, something like 2 or 3 seconds or so
                            && numberOfFramesWithoutStone[i] < 5
                    ) {
                        // If a corner was a stone and is not anymore, the new empty corner should match the real corner
                        // position that the stone was on. This addresses a wrong corner detection in frame 74 of sequence 14.
                        logger.addToLog("Corner " + i + " - This now empty corner is in a wrong position");
                        logger.addToLog("Number of frames without stone = " + numberOfFramesWithoutStone[i]);
                        continue;
                    }
                    boardCorners[i] = possibleNewCorners[i];
                    cornerDetector[i].setCorner(possibleNewCorners[i]);
                }
                numberOfFramesWithDissimilarOrtogonalImages = 0;
                lastValidOrtogonalBoardImage = ortogonalBoardImage2.clone();
                logger.setLastValidOrtogonalBoardImage(lastValidOrtogonalBoardImage);
            } else {
                logger.addToLog("New ortogonal board image is NOT similar to last valid one");
                numberOfFramesWithDissimilarOrtogonalImages++;
            }

            processBoardCorners();
            state = STATE_RUNNING;
        } else {
            state = STATE_LOOKING_FOR_BOARD;
            logger.addToLog("Board is NOT inside countour");
            logger.addToLog("were all corners found = " + wereAllCornersFound);
        }

        logger.logCornerPositions(boardCorners);
    }

    private int getNumberOfCornersThatMoved(Corner[] possibleNewCorners, Corner[] corners) {
        int numberOfCornersThatMoved = 0;
        for (int i = 0; i < 4; i++) {
            if (possibleNewCorners[i].distanceTo(corners[i]) > MOVEMENT_THRESHOLD) {
                numberOfCornersThatMoved++;
            }
        }
        return numberOfCornersThatMoved;
    }

    private int getNumberOfEmptyCornersThatMoved(Corner[] possibleNewCorners, Corner[] corners) {
        int numberOfEmptyCornersThatMoved = 0;
        for (int i = 0; i < 4; i++) {
            if (!possibleNewCorners[i].isStone
                    // && !corners[i].isStone
                    && possibleNewCorners[i].distanceTo(corners[i]) > MOVEMENT_THRESHOLD) {
                numberOfEmptyCornersThatMoved++;
            }
        }
        return numberOfEmptyCornersThatMoved;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnSalvar:
                saveGameRecordOnDisk();
                break;
            case R.id.btnVoltarUltimaJogada:
                temCertezaQueDesejaVoltarAUltimaJogada(getString(R.string.btn_voltar_ultima_jogada));
                break;
            case R.id.btnRotacionarEsquerda:
                rotate(-1);
                break;
            case R.id.btnRotacionarDireita:
                rotate(1);
                break;
            case R.id.btnPausar:
                updatePauseButton();
                break;
            case R.id.btnFinalizar:
                temCertezaQueDesejaFinalizarORegisro();
                break;
            case R.id.btnSnapshot:
                takeSnapshot();
                break;
            case R.id.btnAdicionarPedra:
                adicionarJogadaAoRegistro();
                break;
            case R.id.btnToggleCornerTracking:
                toggleCornerTracking();
                break;
            case R.id.btnResetBoardPosition:
                resetCornersToTheirOriginalPositions();
                break;
        }
    }

	private void updatePauseButton() {
        paused = !paused;
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnPausar.setImageResource(paused ? R.drawable.play : R.drawable.pause);
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
                        logger.addToLog("Undoing last move " + removida);
                    }
                })
                .setNegativeButton(R.string.nao, null)
                .show();
    }

    private void takeSnapshot() {
        logger.takeSnapshot(cameraFrame, tabuleiroOrtogonal);
        Toast.makeText(RegistrarPartidaActivity.this, R.string.toast_save_snapshot, Toast.LENGTH_SHORT).show();
    }

    private void rotate(int direction) {
        logger.addToLog("Rotated board in direction " + direction);

        Corner[] rotatedBoardCorners = new Corner[4];
        for (int i = 0; i < 4; i++) {
            rotatedBoardCorners[i] = new Corner();
        }

        // Counter-clockwise
        if (direction == -1) {
            rotatedBoardCorners[0] = boardCorners[1];
            rotatedBoardCorners[1] = boardCorners[2];
            rotatedBoardCorners[2] = boardCorners[3];
            rotatedBoardCorners[3] = boardCorners[0];
        }
        // Clockwise
        else if (direction == 1) {
            rotatedBoardCorners[0] = boardCorners[3];
            rotatedBoardCorners[1] = boardCorners[0];
            rotatedBoardCorners[2] = boardCorners[1];
            rotatedBoardCorners[3] = boardCorners[2];
        }

        boardCorners = rotatedBoardCorners;
        for (int i = 0; i < 4; i++) {
            cornerDetector[i].setCorner(boardCorners[i]);
        }
        processBoardCorners();

        lastValidOrtogonalBoardImage = ImageUtils.rotateImage(lastValidOrtogonalBoardImage, direction);
        logger.setLastValidOrtogonalBoardImage(lastValidOrtogonalBoardImage);
        partida.rotacionar(direction);
    }

    private void temCertezaQueDesejaFinalizarORegisro() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.dialog_tem_certeza)
            .setMessage(getString(R.string.dialog_finalizar_registro))
            .setPositiveButton(R.string.sim, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    saveGameRecordOnDiskAndExit();
                }
            })
            .setNegativeButton(R.string.nao, null)
            .show();
    }

	/**
	 * Salva o arquivo da partida e o log no armazenamento secundário. Se o
	 * parâmetro 'sair' for verdadeiro, finaliza o registro depois de salvar.
	 */
	private void saveGameRecordOnDiskAndExit() {
        saveGameRecordOnDisk();

        Intent intent = new Intent(getApplicationContext(), TelaInicialActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
	}

	private void saveGameRecordOnDisk() {
        if (fileHelper.saveGameFile(partida)) {
            // Isto tem que rodar na thread de UI porque a activity é fechada após o toast ser mostrado
            runOnUiThread(new Runnable() {
                public void run() {
//                    Toast.makeText(RegistrarPartidaActivity.this, "Partida salva no arquivo: " + arquivoDeRegistro.getName() + ".", Toast.LENGTH_LONG).show();
                }
            });
        }
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
                        logger.addToLog("Move " + adicionadaManualmente + " was manually added");
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
        if (contadorDeJogadas % 5 == 0) saveGameRecordOnDisk();
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

	private void toggleCornerTracking() {
	    isCornerTrackingActive = !isCornerTrackingActive;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnToggleCornerTracking.setImageResource(isCornerTrackingActive ? R.drawable.corner_tracking_active: R.drawable.corner_tracking_inactive);
                Toast.makeText(
                    RegistrarPartidaActivity.this,
                    isCornerTrackingActive ? R.string.toast_activate_corner_tracking : R.string.toast_deactivate_corner_tracking,
                    Toast.LENGTH_LONG
                ).show();
            }
        });
    }

	private void resetCornersToTheirOriginalPositions() {
	    logger.addToLog("Resetting corners to their original positions");
	    for (int i = 0; i < 4; i++) {
	        boardCorners[i].set(originalBoardCorners[i]);
        }
        Toast.makeText(
            RegistrarPartidaActivity.this,
            R.string.toast_restore_original_corner_positions,
            Toast.LENGTH_LONG
        ).show();
        processBoardCorners();
    }

}
