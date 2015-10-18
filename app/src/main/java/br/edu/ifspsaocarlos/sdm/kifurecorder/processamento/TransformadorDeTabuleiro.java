package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Created by leo on 18/10/15.
 */
public class TransformadorDeTabuleiro {

    private static int larguraImagemPreview = 500;
    private static int alturaImagemPreview = 500;

    public static Mat transformar(Mat imagemOriginal, Mat posicaoDoTabuleiroNaImagem, int[] dimensoesDaImagemTransformada) {
//        Mat tabuleiroCorrigido = imagemOriginal.submat(0, alturaImagemPreview + 1, 0, larguraImagemPreview + 1);
        Mat tabuleiroCorrigido = new Mat(alturaImagemPreview, larguraImagemPreview, imagemOriginal.type());

        Mat cantosTransformados = new Mat(4, 1, CvType.CV_32FC2);
        cantosTransformados.put(0, 0,
                0, 0,
                larguraImagemPreview, 0,
                larguraImagemPreview, alturaImagemPreview,
                0, alturaImagemPreview);

        Mat matrizDeTransformacao = Imgproc.getPerspectiveTransform(posicaoDoTabuleiroNaImagem, cantosTransformados);

        // "Travar" a posição do tabuleiro quando tiver detectado corretamente
        Imgproc.warpPerspective(imagemOriginal, tabuleiroCorrigido, matrizDeTransformacao, tabuleiroCorrigido.size());

        return tabuleiroCorrigido;
    }

}
