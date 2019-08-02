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

import br.edu.ifspsaocarlos.sdm.kifurecorder.models.Board;
import br.edu.ifspsaocarlos.sdm.kifurecorder.models.Game;
import br.edu.ifspsaocarlos.sdm.kifurecorder.models.Move;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.Drawer;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.StoneDetector;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.FileHelper;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.ImageUtils;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.Logger;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.LoggingConfiguration;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.Ponto;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.boardDetector.BoardDetector;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.cornerDetector.Corner;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.cornerDetector.CornerDetector;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.similarityCalculator.FingerprintMatching;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.similarityCalculator.SimilarityCalculatorInterface;

public class RecordGameActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, View.OnClickListener {

    public static int STATE_RUNNING = 1;
    public static int STATE_LOOKING_FOR_BOARD = 2;
    private static int MOVEMENT_THRESHOLD = 10;
    private int state;

    Logger logger;
    FileHelper fileHelper;

    BoardDetector boardDetector = new BoardDetector();
    StoneDetector stoneDetector = new StoneDetector();
    CornerDetector[] cornerDetector;
    Game game;
    Board lastDetectedBoard;

    int moveCounter = 0;
    // Time during which a detected board must remain equal to be registered by the detector
    long timeLimit = 2000;
    long timeOfLastBoardDetection;
    long timeSinceLastBoardChange;
    boolean paused = false;
    // TODO: Check if this can be replaced by timeSinceLastBoardChange
    long timeOfLastImageProcessing;
    boolean isCornerTrackingActive = true;
    Mat lastValidOrtogonalBoardImage = null;
    SimilarityCalculatorInterface fingerprintMatching = new FingerprintMatching();

    // This array stores the number of frames that each corner has stayed without
    // ellipses being detected over them. This is used for false positive checking
    int[] numberOfFramesWithoutStone = { 0, 0, 0, 0 };
    // This is also used to bring the detector back when it's strayed too far away
    int numberOfFramesWithDissimilarOrtogonalImages = 0;

    // Domain objects
    // Board dimension can be 9x9, 13x13 or 19x19
    int boardDimension;
    Corner[] boardCorners;
    Corner[] originalBoardCorners;
    Mat cameraFrame;
    // Contains the board corners
    Mat boardPositionInImage;

    // Board image transformed to orthogonal perspective
    Mat orthogonalBoard;
    MatOfPoint boardContour;

    private ImageButton btnUndoLastMove;
    private ImageButton btnRotateCounterClockwise;
    private ImageButton btnRotateClockwise;
    private ImageButton btnPause;
    private ImageButton btnSnapshot;
    private ImageButton btnAddMove;
    private ImageButton btnToggleCornerTracking;
    private ImageButton btnResetBoardPosition;
    private Button btnFinish;
    private SoundPool soundPool;
    private int beepId;
    private CameraBridgeViewBase mOpenCvCameraView;

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
        setContentView(R.layout.activity_registrar_partida);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        state = STATE_RUNNING;

        initializeCamera();
        initializeProcessing();
        initializeUserInterface();
        initializeLogging();

        Log.i(TestsActivity.TAG, "onCreate() finished");
    }

    private void initializeCamera() {
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_registro);
//        mOpenCvCameraView.setMaxFrameSize(1000, 1000);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    private void initializeProcessing() {
        Intent i = getIntent();
        String blackPlayer = i.getStringExtra("blackPlayer");
        String whitePlayer = i.getStringExtra("whitePlayer");
        String komi        = i.getStringExtra("komi");
        boardDimension = i.getIntExtra("boardDimension", -1);
        int[] foundBoardCorners = i.getIntArrayExtra("boardPositionInImage");
        stoneDetector.setBoardDimension(boardDimension);

        game = new Game(boardDimension, blackPlayer, whitePlayer, komi);
        lastDetectedBoard = new Board(boardDimension);
        timeOfLastBoardDetection = SystemClock.elapsedRealtime();
        timeSinceLastBoardChange = 0;
        timeOfLastImageProcessing = SystemClock.elapsedRealtime();

        originalBoardCorners = new Corner[4];
        originalBoardCorners[0] = new Corner(foundBoardCorners[0], foundBoardCorners[1]);
        originalBoardCorners[1] = new Corner(foundBoardCorners[2], foundBoardCorners[3]);
        originalBoardCorners[2] = new Corner(foundBoardCorners[4], foundBoardCorners[5]);
        originalBoardCorners[3] = new Corner(foundBoardCorners[6], foundBoardCorners[7]);
        boardCorners = new Corner[4];
        boardCorners[0] = new Corner(foundBoardCorners[0], foundBoardCorners[1]);
        boardCorners[1] = new Corner(foundBoardCorners[2], foundBoardCorners[3]);
        boardCorners[2] = new Corner(foundBoardCorners[4], foundBoardCorners[5]);
        boardCorners[3] = new Corner(foundBoardCorners[6], foundBoardCorners[7]);
        cornerDetector = new CornerDetector[4];
        for (int cornerIndex = 0; cornerIndex < 4; cornerIndex++) {
            cornerDetector[cornerIndex] = new CornerDetector();
            cornerDetector[cornerIndex].setCorner(boardCorners[cornerIndex]);
            cornerDetector[cornerIndex].setCornerIndex(cornerIndex + 1);
        }
        processBoardCorners();
    }

    private void initializeUserInterface() {
        btnUndoLastMove = findViewById(R.id.btnVoltarUltimaJogada);
        btnUndoLastMove.setOnClickListener(this);
        btnUndoLastMove.setEnabled(false);
        btnRotateCounterClockwise = findViewById(R.id.btnRotacionarEsquerda);
        btnRotateCounterClockwise.setOnClickListener(this);
        btnRotateClockwise = findViewById(R.id.btnRotacionarDireita);
        btnRotateClockwise.setOnClickListener(this);
        btnPause = findViewById(R.id.btnPausar);
        btnPause.setOnClickListener(this);
        btnFinish = findViewById(R.id.btnFinalizar);
        btnFinish.setOnClickListener(this);
        btnSnapshot = findViewById(R.id.btnSnapshot);
        btnSnapshot.setOnClickListener(this);
        btnAddMove = findViewById(R.id.btnAdicionarPedra);
        btnAddMove.setOnClickListener(this);
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
        LoggingConfiguration.activateLogging(LoggingConfiguration.ORTHOGONAL_BOARD_IMAGE);
        LoggingConfiguration.activateLogging(LoggingConfiguration.CORNER_POSITIONS);
        LoggingConfiguration.activateLogging(LoggingConfiguration.NUMBER_OF_QUADRILATERALS_FOUND_BY_BOARD_DETECTOR);

        fileHelper = new FileHelper(game);
        logger = new Logger(game, fileHelper);

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

        boardPositionInImage = new Mat(4, 1, CvType.CV_32FC2);
        boardPositionInImage.put(0, 0,
                cornerPoints[0].x, cornerPoints[0].y,
                cornerPoints[1].x, cornerPoints[1].y,
                cornerPoints[2].x, cornerPoints[2].y,
                cornerPoints[3].x, cornerPoints[3].y);

        boardContour = new MatOfPoint(cornerPoints);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstaceState) {
        super.onRestoreInstanceState(savedInstaceState);
        fileHelper.restoreGameStoredTemporarily(game, boardCorners);
        processBoardCorners();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TestsActivity.TAG, "RecordGameActivity.onPause");
        fileHelper.storeGameTemporarily(game, boardCorners);
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
            Log.d(TestsActivity.TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TestsActivity.TAG, "OpenCV library found inside package. Using it!");
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
            if (orthogonalBoard != null) {
                orthogonalBoard.copyTo(originalImage.rowRange(0, 500).colRange(0, 500));
            }
            Drawer.drawLostBoardContour(originalImage, boardContour);
            Drawer.drawBoard(originalImage, game.getLastBoard(), 0, 500, 400, game.getLastMove());
            logger.log();
            return originalImage;
        }

        // Throttling, processes twice per second
        if (System.currentTimeMillis() - timeOfLastImageProcessing < 500) {
            if (orthogonalBoard != null) {
                orthogonalBoard.copyTo(originalImage.rowRange(0, 500).colRange(0, 500));
            }
            Drawer.drawBoardContour(originalImage, boardContour);
            Drawer.drawBoard(originalImage, game.getLastBoard(), 0, 500, 400, game.getLastMove());
            logger.log();
            return originalImage;
        }
        else timeOfLastImageProcessing = System.currentTimeMillis();

        orthogonalBoard = ImageUtils.transformOrthogonally(originalImage, boardPositionInImage);
        logger.setOrtogonalBoardImage(orthogonalBoard.clone());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // TODO: Check the size of the orthogonal board image here
        //       This affects the radius size when calculating the average color around an intersection
        // int imageWidth = (int)orthogonalBoard.size().width;
        // int imageHeight = (int)orthogonalBoard.size().height;
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        stoneDetector.setBoardImage(orthogonalBoard);

        Board board = stoneDetector.detect(
                game.getLastBoard(),
                game.canNextMoveBe(Board.BLACK_STONE),
                game.canNextMoveBe(Board.WHITE_STONE)
        );

//        snapshotAtual = stoneDetector.snapshot.toString();
        logger.logCurrentBoardState();

        if (!paused) {

            if (lastDetectedBoard.equals(board)) {
                timeSinceLastBoardChange += SystemClock.elapsedRealtime() - timeOfLastBoardDetection;
                timeOfLastBoardDetection = SystemClock.elapsedRealtime();
                if (timeSinceLastBoardChange > timeLimit && game.addMoveIfItIsValid(board)) {
                    newMoveWasAdded();
                }
            } else {
                timeSinceLastBoardChange = 0;
                timeOfLastBoardDetection = SystemClock.elapsedRealtime();
            }

        }

        lastDetectedBoard = board;

        Drawer.drawBoardContour(originalImage, boardContour);
        logger.setCameraImageWithBoardContour(originalImage.clone());

        // Draw the orthogonal board on the screen
        if (orthogonalBoard != null) {
            orthogonalBoard.copyTo(originalImage.rowRange(0, 500).colRange(0, 500));
        }

        if (paused) {
            // When it's paused, draws stone detector's current output (useful for debugging)
            Drawer.drawBoard(originalImage, board, 0, 500, 400, null);
        }
        else {
            Drawer.drawBoard(originalImage, game.getLastBoard(), 0, 500, 400, game.getLastMove());
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

        Mat orthogonalBoardImage = wereAllCornersFound
            ? ImageUtils.generateOrthogonalBoardImage(image, possibleNewCorners)
            : null;

        if (wereAllCornersFound && boardDetector.isBoardContainedIn(orthogonalBoardImage)) {
            logger.addToLog("Board is inside contour");
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

            Mat orthogonalBoardImage2 = ImageUtils.generateOrthogonalBoardImage(image, possibleNewCorners);
            double similarity = lastValidOrtogonalBoardImage != null ? fingerprintMatching.calculateSimilatiryBetween(lastValidOrtogonalBoardImage, orthogonalBoardImage2) : -1;
            logger.addToLog("Similarity between new orthogonal board image to last valid one = " + similarity);
            logger.setOrtogonalBoardImage2(orthogonalBoardImage2);

            if (logger.getFrameNumber() <= 3 || numberOfFramesWithDissimilarOrtogonalImages >= 5 || fingerprintMatching.areImagesSimilar(lastValidOrtogonalBoardImage, orthogonalBoardImage2)) {
                // This condition should be time based and not frame based
                if (numberOfFramesWithDissimilarOrtogonalImages >= 5) {
                    logger.addToLog("Forcing orthogonal image to be similar");
                } else {
                    logger.addToLog("New orthogonal board image is similar to last valid one");
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
                lastValidOrtogonalBoardImage = orthogonalBoardImage2.clone();
                logger.setLastValidOrtogonalBoardImage(lastValidOrtogonalBoardImage);
            } else {
                logger.addToLog("New orthogonal board image is NOT similar to last valid one");
                numberOfFramesWithDissimilarOrtogonalImages++;
            }

            processBoardCorners();
            state = STATE_RUNNING;
        } else {
            state = STATE_LOOKING_FOR_BOARD;
            logger.addToLog("Board is NOT inside contour");
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
            case R.id.btnVoltarUltimaJogada:
                areYouSureYouWantToUndoTheLastMove(getString(R.string.btn_voltar_ultima_jogada));
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
                areYouSureYouWantToFinishRecording();
                break;
            case R.id.btnSnapshot:
                takeSnapshot();
                break;
            case R.id.btnAdicionarPedra:
                addMoveToGameRecord();
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
                btnPause.setImageResource(paused ? R.drawable.play : R.drawable.pause);
            }
        });
	}

    private void areYouSureYouWantToUndoTheLastMove(String mensagem) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_tem_certeza)
                .setMessage(mensagem)
                .setPositiveButton(R.string.sim, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Move removida = game.undoLastMove();
                        timeSinceLastBoardChange = 0;
                        timeOfLastBoardDetection = SystemClock.elapsedRealtime();
                        updateUndoButton();
                        logger.addToLog("Undoing last move " + removida);
                    }
                })
                .setNegativeButton(R.string.nao, null)
                .show();
    }

    private void takeSnapshot() {
        logger.takeSnapshot(cameraFrame, orthogonalBoard);
        Toast.makeText(RecordGameActivity.this, R.string.toast_save_snapshot, Toast.LENGTH_SHORT).show();
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
        game.rotate(direction);
    }

    private void areYouSureYouWantToFinishRecording() {
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
     * Saves the game record and log in secondary storage.
     */
	private void saveGameRecordOnDiskAndExit() {
        saveGameRecordOnDisk();

        Intent intent = new Intent(getApplicationContext(), TelaInicialActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
	}

	private void saveGameRecordOnDisk() {
        if (fileHelper.saveGameFile(game)) {
            // This has to run in the UI thread because the activity may be closed after the toast is shown
            runOnUiThread(new Runnable() {
                public void run() {
//                    Toast.makeText(RecordGameActivity.this, "Partida salva no arquivo: " + arquivoDeRegistro.getName() + ".", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    // TODO: Check if this is being called
    @Override
    public void onBackPressed() {
        Log.d(TestsActivity.TAG, "RecordGameActivity.onBackPressed()");
	    areYouSureYouWantToFinishRecording();
    }

    private void addMoveToGameRecord() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);

        final EditText input = new EditText(RecordGameActivity.this);

        dialog.setTitle(R.string.dialog_adicionar_jogada)
            .setMessage(getString(R.string.dialog_adicionar_jogada))
            .setPositiveButton(R.string.sim, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Move manuallyAddedMove = processManualMove(input.getText().toString());
                    Board newBoard = game.getLastBoard().generateNewBoardWith(manuallyAddedMove);
                    if (game.addMoveIfItIsValid(newBoard)) {
                        newMoveWasAdded();
                        game.updateNumberOfManualAdditions();
                        logger.addToLog("Move " + manuallyAddedMove + " was manually added");
                    }
                }
            })
            .setNegativeButton(R.string.nao, null)
            .setView(input)
            .show();
    }
    
    private Move processManualMove(String moveString) {
		moveString = moveString.trim();
		if (moveString.length() != 3) return null;
		moveString = moveString.toLowerCase();
		int color = moveString.charAt(0) == 'b' ? Board.BLACK_STONE : Board.WHITE_STONE;
		int row = moveString.charAt(1) - 'a';
		int column = moveString.charAt(2) - 'a';
		return new Move(row, column, color);
	}

    private void newMoveWasAdded() {
        moveCounter++;
        soundPool.play(beepId, 1, 1, 0, 0, 1);
        saveGameRecordOnDisk();
        updateUndoButton();
    }

	private void updateUndoButton() {
		runOnUiThread(new Runnable() {
            @Override
            public void run() {
                btnUndoLastMove.setEnabled(game.getNumberOfMoves() > 0);
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
                    RecordGameActivity.this,
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
            RecordGameActivity.this,
            R.string.toast_restore_original_corner_positions,
            Toast.LENGTH_LONG
        ).show();
        processBoardCorners();
    }

}
