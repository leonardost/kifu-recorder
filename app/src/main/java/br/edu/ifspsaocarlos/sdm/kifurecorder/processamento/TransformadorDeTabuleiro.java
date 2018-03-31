package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

/**
 * Responsável por fazer a transformação ortogonal da imagem do tabuleiro.
 */
public class TransformadorDeTabuleiro {

    private static int larguraImagemPreview = 500;
    private static int alturaImagemPreview = 500;

    public static Mat transformarOrtogonalmente(Mat imagemOriginal, Mat posicaoDoTabuleiroNaImagem) {
//        Mat tabuleiroOrtogonal = imagemOriginal.submat(0, alturaImagemPreview + 1, 0, larguraImagemPreview + 1);
        Mat tabuleiroOrtogonal = new Mat(alturaImagemPreview, larguraImagemPreview, imagemOriginal.type());

        Mat cantosDaImagemOrtogonal = new Mat(4, 1, CvType.CV_32FC2);
        cantosDaImagemOrtogonal.put(0, 0,
                0, 0,
                larguraImagemPreview, 0,
                larguraImagemPreview, alturaImagemPreview,
                0, alturaImagemPreview);

        Mat matrizDeTransformacao = Imgproc.getPerspectiveTransform(posicaoDoTabuleiroNaImagem, cantosDaImagemOrtogonal);
        Imgproc.warpPerspective(imagemOriginal, tabuleiroOrtogonal, matrizDeTransformacao, tabuleiroOrtogonal.size());

        return tabuleiroOrtogonal;
    }

}
