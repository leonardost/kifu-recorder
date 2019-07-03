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

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

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
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.TransformadorDeTabuleiro;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.boardDetector.BoardDetector;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.cornerDetector.Corner;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.cornerDetector.CornerDetector;

public class RegistrarPartidaActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    public static int STATE_RUNNING = 1;
    public static int STATE_LOOKING_FOR_BOARD = 2;
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
    boolean pausado = false;
    long momentoDoUltimoProcessamentoDeImagem;
    long tempoDesdeUltimoProcessamentoDeImagem;

    // Domain objects
    int dimensaoDoTabuleiro;                   // 9x9, 13x13 ou 19x19
    Corner[] boardCorners;
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

        boardCorners = new Corner[4];
        boardCorners[0] = new Corner(cantosDoTabuleiroEncontrados[0], cantosDoTabuleiroEncontrados[1]);
        boardCorners[1] = new Corner(cantosDoTabuleiroEncontrados[2], cantosDoTabuleiroEncontrados[3]);
        boardCorners[2] = new Corner(cantosDoTabuleiroEncontrados[4], cantosDoTabuleiroEncontrados[5]);
        boardCorners[3] = new Corner(cantosDoTabuleiroEncontrados[6], cantosDoTabuleiroEncontrados[7]);
        cornerDetector = new CornerDetector[4];
        for (int cornerIndex = 0; cornerIndex < 4; cornerIndex++) {
            cornerDetector[cornerIndex].setCorner(boardCorners[cornerIndex]);
            cornerDetector[cornerIndex].setCornerIndex(cornerIndex + 1);
        }
        processarCantosDoTabuleiro();
    }

    private void initializeUserInterface() {
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
    }

    private void processarCantosDoTabuleiro() {
        posicaoDoTabuleiroNaImagem = new Mat(4, 1, CvType.CV_32FC2);
        posicaoDoTabuleiroNaImagem.put(0, 0,
                boardCorners[0].getX(), boardCorners[0].getY(),
                boardCorners[1].getX(), boardCorners[1].getY(),
                boardCorners[2].getX(), boardCorners[2].getY(),
                boardCorners[3].getX(), boardCorners[3].getY());

        org.opencv.core.Point[] cantos = new org.opencv.core.Point[4];
        cantos[0] = new org.opencv.core.Point(boardCorners[0].getX(), boardCorners[0].getY());
        cantos[1] = new org.opencv.core.Point(boardCorners[1].getX(), boardCorners[1].getY());
        cantos[2] = new org.opencv.core.Point(boardCorners[2].getX(), boardCorners[2].getY());
        cantos[3] = new org.opencv.core.Point(boardCorners[3].getX(), boardCorners[3].getY());
        contornoDoTabuleiro = new MatOfPoint(cantos);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstaceState) {
        super.onRestoreInstanceState(savedInstaceState);
        fileHelper.restoreGameStoredTemporarily(partida, boardCorners);
        processarCantosDoTabuleiro();
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
        logger.setStartProcessingTime(System.currentTimeMillis());
        logger.setCameraFrame(inputFrame.rgba().clone());

        Mat imagemFonte = inputFrame.rgba();

        updateCornerPositions(imagemFonte);

        if (state == STATE_LOOKING_FOR_BOARD) {
            logger.addToLog("Board is not inside contour");
            logger.addToLog("");
            tabuleiroOrtogonal.copyTo(imagemFonte.rowRange(0, 500).colRange(0, 500));
            Desenhista.drawLostBoardContour(imagemFonte, contornoDoTabuleiro);
            Desenhista.desenharTabuleiro(imagemFonte, partida.ultimoTabuleiro(), 0, 500, 400, partida.ultimaJogada());
            logger.log();
            return imagemFonte;
        }

        // Throttling, processa 2 vezes por segundo
        if (System.currentTimeMillis() - momentoDoUltimoProcessamentoDeImagem < 500) {
            tabuleiroOrtogonal.copyTo(imagemFonte.rowRange(0, 500).colRange(0, 500));
            Desenhista.desenharContornoDoTabuleiro(imagemFonte, contornoDoTabuleiro);
            Desenhista.desenharTabuleiro(imagemFonte, partida.ultimoTabuleiro(), 0, 500, 400, partida.ultimaJogada());
            logger.log();
            return imagemFonte;
        }
        else momentoDoUltimoProcessamentoDeImagem = System.currentTimeMillis();

        tabuleiroOrtogonal = TransformadorDeTabuleiro.transformarOrtogonalmente(imagemFonte, posicaoDoTabuleiroNaImagem);
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

        if (!pausado) {

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

        Desenhista.desenharContornoDoTabuleiro(imagemFonte, contornoDoTabuleiro);
        logger.setCameraImageWithBoardContour(imagemFonte.clone());

        // Desenha o tabuleiro ortogonal na tela
        tabuleiroOrtogonal.copyTo(imagemFonte.rowRange(0, 500).colRange(0, 500));

        if (pausado) {
            // Quando está pausado, desenha a saída atual do detector de pedras (útil para debugar)
            Desenhista.desenharTabuleiro(imagemFonte, tabuleiro, 0, 500, 400, null);
        }
        else {
            Desenhista.desenharTabuleiro(imagemFonte, partida.ultimoTabuleiro(), 0, 500, 400, partida.ultimaJogada());
        }

        logger.log();

        return imagemFonte;
    }

    private void updateCornerPositions(Mat image) {
        Corner[] possibleNewCorners = new Corner[4];
        boolean wereAllCornersFound = true;
        for (int i = 0; i < 4; i++) {
            possibleNewCorners[i] = cornerDetector[i].detectCornerIn(image);
            if (possibleNewCorners[i] == null) {
                wereAllCornersFound = false;
            }
        }

        Mat ortogonalBoardImage = ImageUtils.generateOrtogonalBoardImage(image, possibleNewCorners);

        if (boardDetector.isBoardContainedIn(ortogonalBoardImage) && wereAllCornersFound) {
            System.out.println("Board is inside countour");
            int numberOfCornersThatMoved = getNumberOfCornersThatMoved(possibleNewCorners, boardCorners);
            System.out.println("Number of corners that moved: " + numberOfCornersThatMoved);
            double[] distanceToNewPoint = new double[4];
            for (int i = 0; i < 4; i++) {
                distanceToNewPoint[i] = possibleNewCorners[i].distanceTo(boardCorners[i]);
                System.out.println("Distance to old corner point " + (i + 1) + " = " + distanceToNewPoint[i]);
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
                    // All corners moved together, so this is probably a board displacemente and we
                    // don't update the corners's relative position to the real corners
                    possibleNewCorners[i].displacementToRealCorner = boardCorners[i].displacementToRealCorner;
                }

                boardCorners[i] = possibleNewCorners[i];
            }

            processarCantosDoTabuleiro();
            state = STATE_RUNNING;
        } else {
            state = STATE_LOOKING_FOR_BOARD;
            System.out.println("Board is NOT inside countour");
        }

//        logger.logNumberOfQuadrilateralsFoundByBoardDetector(boardDetector.getNumberOfQuadrilateralsFound());
        logger.logCornerPositions(boardCorners);
    }

    private static int getNumberOfCornersThatMoved(Corner[] possibleNewCorners, Corner[] corners) {
        int MOVEMENT_THRESHOULD = 10;
        int numberOfCornersThatMoved = 0;
        for (int i = 0; i < 4; i++) {
            if (possibleNewCorners[i].distanceTo(corners[i]) > MOVEMENT_THRESHOULD) {
                numberOfCornersThatMoved++;
            }
        }
        return numberOfCornersThatMoved;
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
                rotacionar(-1);
                break;
            case R.id.btnRotacionarDireita:
                rotacionar(1);
                break;
            case R.id.btnPausar:
                pausado = !pausado;
                updatePauseButton();
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

	private void updatePauseButton() {
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
//                        adicionarAoLog("Voltando jogada " + removida + "\n\n");
                    }
                })
                .setNegativeButton(R.string.nao, null)
                .show();
    }

    private void tirarSnapshot() {
//        try {
//
//            Mat imagemFormatoDeCorCerto = new Mat();
//            Imgproc.cvtColor(tabuleiroOrtogonal, imagemFormatoDeCorCerto, Imgproc.COLOR_RGBA2BGR);
//            Imgcodecs.imwrite(getFile("jpg").getAbsolutePath(), imagemFormatoDeCorCerto);
//
//            if (DEBUG) {
//                FileOutputStream fos = new FileOutputStream(getFile("txt"), false);
//                fos.write(snapshotAtual.getBytes());
//                fos.flush();
//                fos.close();
//            }
//
//            Toast.makeText(RegistrarPartidaActivity.this, "Snapshot salva no arquivo: " + getFile("jpg").getName() + ".", Toast.LENGTH_LONG).show();
//            Log.i(TestesActivity.TAG, "Snapshot salva: " + getFile("txt").getName() + " com conteúdo " + snapshotAtual);
//        }
//        catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void rotacionar(int direcao) {
        Corner[] cantosDoTabuleiroRotacionados = new Corner[4];

		// Anti-horário
        if (direcao == -1) {
            cantosDoTabuleiroRotacionados[0].set(boardCorners[1]);
            cantosDoTabuleiroRotacionados[1].set(boardCorners[2]);
            cantosDoTabuleiroRotacionados[2].set(boardCorners[3]);
            cantosDoTabuleiroRotacionados[3].set(boardCorners[0]);
        }
        // Horário
        else if (direcao == 1) {
            cantosDoTabuleiroRotacionados[0].set(boardCorners[3]);
            cantosDoTabuleiroRotacionados[1].set(boardCorners[0]);
            cantosDoTabuleiroRotacionados[2].set(boardCorners[1]);
            cantosDoTabuleiroRotacionados[3].set(boardCorners[2]);
        }

        boardCorners = cantosDoTabuleiroRotacionados;
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
//                            adicionarAoLog("Jogada " + adicionadaManualmente + " foi adicionada manualmente.\n\n");
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

}
