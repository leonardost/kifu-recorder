package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

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

import br.edu.ifspsaocarlos.sdm.kifurecorder.TestesActivity;
import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Partida;
import br.edu.ifspsaocarlos.sdm.kifurecorder.processamento.cornerDetector.Corner;

public class FileHelper {

    private String gameName;
    private File gameRecordFolder;
    private File gameRecordLogFolder;
    private File gameFile;

    public FileHelper(Partida partida) {
        gameName = generateGameName(partida);
        gameRecordFolder = new File(Environment.getExternalStorageDirectory() + "/kifu_recorder");
        gameRecordLogFolder = new File(Environment.getExternalStorageDirectory() + "/kifu_recorder/" + gameName);
        gameFile = getGameFile();
        createGameRecordFolder();
    }

    private String generateGameName(Partida partida) {
        // http://stackoverflow.com/questions/10203924/displaying-date-in-a-double-digit-format
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd_HHmm");
        String timestamp = sdf.format(new Date(Calendar.getInstance().getTimeInMillis()));

        return timestamp + "_" + partida.getJogadorDeBrancas() + "-" + partida.getJogadorDePretas();
    }

    public void createGameRecordFolder() {
        if (!gameRecordLogFolder.exists() && !gameRecordLogFolder.mkdirs()) {
            // TODO: Throw an exception
//            Toast.makeText(RegistrarPartidaActivity.this, "ERRO: Diretório " + gameRecordLogFolder.toString() + " não criado, verifique as configurações de armazenamento de seu dispositivo.", Toast.LENGTH_LONG).show();
            Log.e(TestesActivity.TAG, "Folder " + gameRecordLogFolder.toString() + " could not be created, check your device's storage configuration.");
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

    public File getFile(String nome, String extensao) {
        File arquivo = new File(gameRecordLogFolder, generateFilename(0, nome, extensao));
        int contador = 1;

        while (arquivo.exists()) {
            String newFilename = generateFilename(contador, nome, extensao);
            arquivo = new File(gameRecordLogFolder, newFilename);
            contador++;
        }

        return arquivo;
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

    public boolean saveGameFile(Partida partida) {
        String conteudoDaPartida = partida.sgf();

        if (isExternalStorageWritable()) {
            try {
                FileOutputStream fos = new FileOutputStream(gameFile, false);
                fos.write(conteudoDaPartida.getBytes());
                fos.flush();
                fos.close();

                Log.i(TestesActivity.TAG, "Partida salva: " + gameFile.getName());
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
////            Toast.makeText(RegistrarPartidaActivity.this, "ERRO: Armazenamento externo nao disponivel.", Toast.LENGTH_LONG).show();
////            Log.e(TestesActivity.TAG, "Armazenamento externo não disponível.");
//            return false
//        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public void storeGameTemporarily(Partida partida, Corner[] cantosDoTabuleiro) {
        File arquivo = getTempFile();
        if (isExternalStorageWritable()) {
            try {
                FileOutputStream fos = new FileOutputStream(arquivo, false);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(partida);
                oos.writeObject(cantosDoTabuleiro);
                oos.close();
                fos.close();
                Log.i(TestesActivity.TAG, "Partida salva temporariamente no arquivo " + arquivo.getName());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.e(TestesActivity.TAG, "Armazenamento externo não disponível para guardar registro temporário da partida.");
        }
    }

    public void restoreGameStoredTemporarily(Partida partida, Corner[] cantosDoTabuleiro) {
        File arquivo = getTempFile();
        // TODO: Precisa fazer esta checagem aqui? Porque aqui só é feita a leitura de arquivos
        if (isExternalStorageWritable()) {
            try {
                FileInputStream fis = new FileInputStream(arquivo);
                ObjectInputStream ois = new ObjectInputStream(fis);
                partida = (Partida) ois.readObject();
                cantosDoTabuleiro = (Corner[]) ois.readObject();
                ois.close();
                fis.close();
                Log.i(TestesActivity.TAG, "Partida recuperada.");
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.e(TestesActivity.TAG, "Armazenamento externo não disponível para restaurar registro temporário da partida.");
        }
    }

    public void writeJpgImage(Mat image, int colorConversionCode, String filename) {
        Mat correctColorFormatImage = new Mat();
        Imgproc.cvtColor(image, correctColorFormatImage, colorConversionCode);
        Imgcodecs.imwrite(getFile(filename, "jpg").getAbsolutePath(), correctColorFormatImage);
    }

    public void writePngImage(Mat image, int colorConversionCode, String filename) {
        Mat correctColorFormatImage = new Mat();
        Imgproc.cvtColor(image, correctColorFormatImage, colorConversionCode);
        Imgcodecs.imwrite(getFile(filename, "png").getAbsolutePath(), correctColorFormatImage);
    }

}
