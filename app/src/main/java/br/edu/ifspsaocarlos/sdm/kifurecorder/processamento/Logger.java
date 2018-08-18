package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Partida;

public class Logger {

    private long frameNumber = 0;
    private StringBuilder logText;
    private FileHelper fileHelper;

    private long startProcessingTime;
    private Mat cameraFrame;
    private Mat cameraImageWithBoardContour;
    private Mat ortogonalBoardImage;
    private Partida partida;

    private File arquivoDeLog;                 // Guarda informacoes de debug referentes a partida

    public Logger(Partida partida, FileHelper fileHelper) {
        this.partida = partida;
        this.fileHelper = fileHelper;

        arquivoDeLog = fileHelper.getFile("", "log");
    }

    public void increaseFrameNumber() {
        frameNumber++;
        logText = new StringBuilder();
        addToLog("=====");
        addToLog("Frame " + frameNumber);
    }

    public void addToLog(String text) {
        logText.append(text + "\n");
    }

    public void setStartProcessingTime(long time) {
        this.startProcessingTime = time;
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

    public void setCornerPositions(Ponto[] cornerPositions) {
        if (LoggingConfiguration.shouldLog(LoggingConfiguration.CORNER_POSITIONS)) {
            addToLog("    Corner positions:");
            for (int i = 0; i < 4; i++) {
                addToLog("        " + cornerPositions[i].toString());
            }
        }
    }

    public void log() {

        if (LoggingConfiguration.shouldLog(LoggingConfiguration.RAW_CAMERA_IMAGE)) {
            fileHelper.writeJpgImage(cameraFrame, Imgproc.COLOR_RGBA2BGR, generateImageLogFilename("camera"));
        }

        if (LoggingConfiguration.shouldLog(LoggingConfiguration.CAMERA_IMAGE_WITH_BOARD_CONTOUR)) {
            fileHelper.writeJpgImage(cameraImageWithBoardContour, Imgproc.COLOR_RGBA2BGR, generateImageLogFilename("camera_com_contorno"));
        }

        if (LoggingConfiguration.shouldLog(LoggingConfiguration.ORTOGONAL_BOARD_IMAGE)) {
            fileHelper.writeJpgImage(ortogonalBoardImage, Imgproc.COLOR_RGBA2BGR, generateImageLogFilename("tabuleiro_ortogonal"));
        }

        addToLog("    Tempo de processamento: " + (System.currentTimeMillis() - startProcessingTime) + "ms");

        writeToLogFile();

        // add text information to log

        // Output images
        // Output everything to a file
        //

        // ============================================
        // Frame X:
        //
        // Number of plays: X
        //
        // Corners:
        //    (X1, Y1), (X2, Y2), (X3, Y3)...
        //
        // Stone detection:
        //    (1, 1):
        //        asdf
        //    (2, 1):
        //        qwer
        // ============================================
    }

    private String generateImageLogFilename(String filename) {
        return "frame_" + frameNumber + "_" + "jogada_" + partida.numeroDeJogadasFeitas() + "_" + filename;
    }

    private void writeToLogFile() {
        try {
            FileOutputStream fos = new FileOutputStream(arquivoDeLog, true);
            fos.write(logText.toString().getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
