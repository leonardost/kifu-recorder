package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Partida;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.cornerDetector.Corner;

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

    private Mat cameraFrame;
    private Mat cameraImageWithBoardContour;
    private Mat ortogonalBoardImage;
    private Partida partida;

    private File logFile;

    public Logger(Partida partida, FileHelper fileHelper) {
        this.partida = partida;
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
            addToLog(partida.ultimoTabuleiro().toString());
            addToLog();
        }
    }

    public void log() {
        addToLog("Number of plays: " + partida.numeroDeJogadasFeitas());
        addToLog();

        addToLog("Frame processing time: " + (System.currentTimeMillis() - startProcessingTime) + "ms");

        if (shouldLog(LoggingConfiguration.RAW_CAMERA_IMAGE) && cameraFrame != null) {
            fileHelper.writePngImage(cameraFrame, Imgproc.COLOR_RGBA2BGR, generateImageFilename("camera"));
        }

        if (shouldLog(LoggingConfiguration.CAMERA_IMAGE_WITH_BOARD_CONTOUR) && cameraImageWithBoardContour != null) {
            fileHelper.writePngImage(cameraImageWithBoardContour, Imgproc.COLOR_RGBA2BGR, generateImageFilename("camera_com_contorno"));
        }

        if (shouldLog(LoggingConfiguration.ORTOGONAL_BOARD_IMAGE) && ortogonalBoardImage != null) {
            fileHelper.writePngImage(ortogonalBoardImage, Imgproc.COLOR_RGBA2BGR, generateImageFilename("tabuleiro_ortogonal"));
        }

        writeToLogFile();
    }

    private boolean shouldLog(int flag) {
        return LoggingConfiguration.shouldLog(flag);
    }

    private String generateImageFilename(String filename) {
        return "frame_" + frameNumber + "_" + "jogada_" + partida.numeroDeJogadasFeitas() + "_" + filename;
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

}
