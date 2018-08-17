package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import android.os.Environment;

import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DebugHelper {

    static File pastaDeRegistro;

    static {
        pastaDeRegistro = new File(Environment.getExternalStorageDirectory(), "kifu_recorder");
        criarPastaDeRegistroSeNaoExistir();
    }

    private static void criarPastaDeRegistroSeNaoExistir() {
        if (!pastaDeRegistro.exists()) pastaDeRegistro.mkdirs();
    }

    public static void writeImage(Mat image, int colorConversionCode, String filename) {
        Mat correctColorFormatImage = new Mat();
        Imgproc.cvtColor(image, correctColorFormatImage, colorConversionCode);
        Imgcodecs.imwrite(getFile(filename, "jpg").getAbsolutePath(), correctColorFormatImage);
    }

    private static File getFile(String nome, String extensao) {
        File arquivo = new File(pastaDeRegistro, gerarNomeDeArquivo(0, nome, extensao));
        int contador = 1;

        while (arquivo.exists()) {
            String newFilename = gerarNomeDeArquivo(contador, nome, extensao);
            arquivo = new File(pastaDeRegistro, newFilename);
            contador++;
        }

        return arquivo;
    }

    private static String gerarNomeDeArquivo(int contadorDeNomeRepetido, String nome, String extensao) {
        // http://stackoverflow.com/questions/10203924/displaying-date-in-a-double-digit-format
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd");
        String data = sdf.format(new Date(Calendar.getInstance().getTimeInMillis()));
        String contador = "";
        if (contadorDeNomeRepetido > 0) {
            contador = "(" + contadorDeNomeRepetido + ")";
        }

        StringBuilder string = new StringBuilder();
        string.append(data)
                .append("_")
                .append(nome)
                .append("_")
                .append(contador)
                .append(".")
                .append(extensao);
        return string.toString();
    }

}
