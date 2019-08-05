package br.edu.ifspsaocarlos.sdm.kifurecorder.processing;

import android.os.Environment;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import br.edu.ifspsaocarlos.sdm.kifurecorder.models.Game;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processing.cornerDetector.Corner;

public class FileHelper {

    private String gameName;
    private File gameRecordFolder;
    private File gameRecordLogFolder;
    private File gameFile;

    public FileHelper(Game game) {
        gameName = generateGameName(game);
        gameRecordFolder = new File(Environment.getExternalStorageDirectory() + "/kifu_recorder");
        gameRecordLogFolder = new File(Environment.getExternalStorageDirectory() + "/kifu_recorder/" + gameName);
        gameFile = getGameFile();
        createGameRecordFolder();
    }

    private String generateGameName(Game game) {
        // http://stackoverflow.com/questions/10203924/displaying-date-in-a-double-digit-format
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd_HHmm");
        String timestamp = sdf.format(new Date(Calendar.getInstance().getTimeInMillis()));

        return timestamp + "_" + game.getWhitePlayer() + "-" + game.getBlackPlayer();
    }

    public void createGameRecordFolder() {
        if (!gameRecordLogFolder.exists() && !gameRecordLogFolder.mkdirs()) {
            // TODO: Throw an exception
//            Toast.makeText(RecordGameActivity.this, "ERRO: Diretório " + gameRecordLogFolder.toString() + " não criado, verifique as configurações de armazenamento de seu dispositivo.", Toast.LENGTH_LONG).show();
            Log.e("KifuRecorder", "Folder " + gameRecordLogFolder.toString() + " could not be created, check your device's available space and storage configuration.");
        }
    }

    private File getGameFile() {
        File file = new File(gameRecordFolder, generateFilename(0, "", "sgf"));
        int counter = 1;

        while (file.exists()) {
            String newFilename = generateFilename(counter, "", "sgf");
            file = new File(gameRecordFolder, newFilename);
            counter++;
        }

        return file;
    }

    public File getFile(String name, String extension) {
        File file = new File(gameRecordLogFolder, generateFilename(0, name, extension));
        int counter = 1;

        while (file.exists()) {
            String newFilename = generateFilename(counter, name, extension);
            file = new File(gameRecordLogFolder, newFilename);
            counter++;
        }

        return file;
    }

    private String generateFilename(int repeatedNameCounter, String filename, String extension) {
        String counter = repeatedNameCounter > 0 ?
            "(" + repeatedNameCounter + ")" : "";

        StringBuilder string = new StringBuilder();
        string.append(gameName);
        if (!filename.isEmpty()) {
            string.append("_").append(filename);
        }
        string.append("_").append(counter).append(".").append(extension);
        return string.toString();
    }

    public File getTempFile() {
        return new File(gameRecordFolder, "temp_file");
    }

    public boolean saveGameFile(Game game) {
        String gameContent = game.sgf();

        if (isExternalStorageWritable()) {
            try {
                FileOutputStream fos = new FileOutputStream(gameFile, false);
                fos.write(gameContent.getBytes());
                fos.flush();
                fos.close();

                Log.i("KifuRecorder", "Game saved: " + gameFile.getName());
                return true;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
//
//        else {
//            // TODO: Throw exception
////            Toast.makeText(RecordGameActivity.this, "ERRO: Armazenamento externo nao disponivel.", Toast.LENGTH_LONG).show();
////            Log.e("KifuRecorder", "Armazenamento externo não disponível.");
//            return false
//        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public void storeGameTemporarily(Game game, Corner[] cantosDoTabuleiro) {
        File file = getTempFile();
        if (isExternalStorageWritable()) {
            try {
                FileOutputStream fos = new FileOutputStream(file, false);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(game);
                oos.writeObject(cantosDoTabuleiro);
                oos.close();
                fos.close();
                Log.i("KifuRecorder", "Game temporarily saved in " + file.getName());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.e("KifuRecorder", "External storage not available to store temporary game state.");
        }
    }

    public void restoreGameStoredTemporarily(Game game, Corner[] boardCorners) {
        File arquivo = getTempFile();
        // TODO: Precisa fazer esta checagem aqui? Porque aqui só é feita a leitura de arquivos
        if (isExternalStorageWritable()) {
            try {
                FileInputStream fis = new FileInputStream(arquivo);
                ObjectInputStream ois = new ObjectInputStream(fis);
                game = (Game) ois.readObject();
                boardCorners = (Corner[]) ois.readObject();
                ois.close();
                fis.close();
                Log.i("KifuRecorder", "Partida recuperada.");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.e("KifuRecorder", "External storage not available to restore temporary game state.");
        }
    }

    public void writePngImage(Mat image, int colorConversionCode, String filename) {
        Mat correctColorFormatImage = new Mat();
        Imgproc.cvtColor(image, correctColorFormatImage, colorConversionCode);
        Imgcodecs.imwrite(getFile(filename, "png").getAbsolutePath(), correctColorFormatImage);
    }

    public void writePngImage(Mat image, String filename) {
        Imgcodecs.imwrite(getFile(filename, "png").getAbsolutePath(), image);
    }

}
