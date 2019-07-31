package br.edu.ifspsaocarlos.sdm.kifurecorder.processing;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import br.edu.ifspsaocarlos.sdm.kifurecorder.models.Game;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.cornerDetector.Corner;

/**
 * Log file format:
 *
 * ============================================
 * Frame X:
 *
 * Number of plays: X
 *
 * Corners:
 *     (X1, Y1), (X2, Y2), (X3, Y3)...
 *
 * Stone detection:
 *     (1, 1):
 *         asdf
 *     (2, 1):
 *         qwer
 *     ...
 * ============================================
 * Frame X + 1:
 * ...
 */

public class Logger {

    private long frameNumber = 0;
    private StringBuilder logText;
    private FileHelper fileHelper;
    private long startProcessingTime;
    private boolean isActive = false;
    private boolean shouldSaveImages = false;

    private Mat cameraFrame;
    private Mat cameraImageWithBoardContour;
    private Mat ortogonalBoardImage;
    private Mat ortogonalBoardImage2;
    private Mat lastValidOrtogonalBoardImage;
    private Game game;

    private File logFile;

    public Logger(Game game, FileHelper fileHelper) {
        this.game = game;
        this.fileHelper = fileHelper;

        logFile = fileHelper.getFile("", "log");
    }

    public long getFrameNumber() {
        return frameNumber;
    }

    public void startLoggingFrame() {
        frameNumber++;
        startProcessingTime = System.currentTimeMillis();
        logText = new StringBuilder();
        addToLog("===============================");
        addToLog("Frame " + frameNumber);
        addToLog();
    }

    public void addToLog() {
        logText.append("\n");
    }

    public void addToLog(String text) {
        logText.append(text + "\n");
    }

    public void setCameraFrame(Mat cameraFrame) {
        this.cameraFrame = cameraFrame;
    }

    public void setCameraImageWithBoardContour(Mat image) {
        this.cameraImageWithBoardContour = image;
    }

    public void setOrtogonalBoardImage(Mat image) {
        this.ortogonalBoardImage = image;
    }

    public void setOrtogonalBoardImage2(Mat image) {
        this.ortogonalBoardImage2 = image;
    }

    public void setLastValidOrtogonalBoardImage(Mat image) {
        this.lastValidOrtogonalBoardImage = image;
    }

    public void logCornerPositions(Corner[] cornerPositions) {
        if (shouldLog(LoggingConfiguration.CORNER_POSITIONS)) {
            addToLog("Corner positions:");
            for (int i = 0; i < 4; i++) {
                addToLog("    " + cornerPositions[i].toString());
            }
            addToLog();
        }
    }

    public void logNumberOfQuadrilateralsFoundByBoardDetector(int numberOfQuadrilaterals) {
        if (shouldLog(LoggingConfiguration.NUMBER_OF_QUADRILATERALS_FOUND_BY_BOARD_DETECTOR)) {
            addToLog("Number of quadrilaterals found by board detector: " + numberOfQuadrilaterals);
            addToLog();
        }
    }

    public void logCurrentBoardState() {
        if (shouldLog(LoggingConfiguration.CURRENT_BOARD_STATE)) {
            addToLog("Current board state");
            addToLog(game.getLastBoard().toString());
            addToLog();
        }
    }

    public void log() {
        if (!isActive) return;

        addToLog("Number of plays: " + game.getNumberOfMoves());
        addToLog();
        addToLog("Frame processing time: " + (System.currentTimeMillis() - startProcessingTime) + "ms");
        writeToLogFile();

        if (!shouldSaveImages) return;

        if (shouldLog(LoggingConfiguration.RAW_CAMERA_IMAGE) && cameraFrame != null) {
            fileHelper.writePngImage(cameraFrame, Imgproc.COLOR_RGBA2BGR, generateImageFilename("camera"));
        }

        if (shouldLog(LoggingConfiguration.CAMERA_IMAGE_WITH_BOARD_CONTOUR) && cameraImageWithBoardContour != null) {
            fileHelper.writePngImage(cameraImageWithBoardContour, Imgproc.COLOR_RGBA2BGR, generateImageFilename("camera_com_contorno"));
        }

        if (shouldLog(LoggingConfiguration.ORTOGONAL_BOARD_IMAGE) && ortogonalBoardImage != null) {
            fileHelper.writePngImage(ortogonalBoardImage, Imgproc.COLOR_RGBA2BGR, generateImageFilename("tabuleiro_ortogonal"));
        }

        if (ortogonalBoardImage2 != null) {
            fileHelper.writePngImage(ortogonalBoardImage2, Imgproc.COLOR_RGBA2BGR, generateImageFilename("segundo_tabuleiro_ortogonal"));
        }

        if (lastValidOrtogonalBoardImage != null) {
            fileHelper.writePngImage(lastValidOrtogonalBoardImage, Imgproc.COLOR_RGBA2BGR, generateImageFilename("ultimo_tabuleiro_valido"));
        }
    }

    private boolean shouldLog(int flag) {
        return LoggingConfiguration.shouldLog(flag);
    }

    private String generateImageFilename(String filename) {
        return "frame_" + frameNumber + "_" + "jogada_" + game.getNumberOfMoves() + "_" + filename;
    }

    private void writeToLogFile() {
        try {
            FileOutputStream fos = new FileOutputStream(logFile, true);
            fos.write(logText.toString().getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void takeSnapshot(Mat cameraFrame, Mat ortogonalBoard) {
        fileHelper.writePngImage(cameraFrame, Imgproc.COLOR_RGBA2BGR, generateImageFilename("snapshot_camera"));
        fileHelper.writePngImage(ortogonalBoard, Imgproc.COLOR_RGBA2BGR, generateImageFilename("snapshot_ortogonal_board"));
    }

}
