package br.edu.ifspsaocarlos.sdm.kifurecorder.processing;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import br.edu.ifspsaocarlos.sdm.kifurecorder.TestsActivity;

/**
 * Detecta a posição de um tabuleiro em uma imagem e sua dimensão (9x9, 13x13 ou 19x19)
 */
public class InitialBoardDetector {

    // Imagem da câmera
    private Mat imagem;
    private Mat imagemDePreview;

    // Atributos calculados pela classe
    private boolean processadoComSucesso = false;
    private int dimensaoDoTabuleiro;
    private Mat posicaoDoTabuleiroNaImagem;
    private boolean desenharPreview = false;

    public InitialBoardDetector(boolean desenharPreview) {
        this.desenharPreview = desenharPreview;
    }

    public void setImagem(Mat imagem) {
        this.imagem = imagem;
    }

    public void setImagemDePreview(Mat imagemDePreview) {
        this.imagemDePreview = imagemDePreview;
    }

    /**
     * Processa a imagem fornecida. Retorna verdadeiro se o processamento completo ocorreu com
     * sucesso, ou seja, se um tabuleiro de Go foi detectado na imagem. Retorna falso, caso
     * contrário.
     *
     * @return boolean
     */
    public boolean processar() {
        if (imagem == null) {
            // lançar erro
            return false;
        }

        Mat imagemComBordasEmEvidencia = detectarBordas();

        // Se quiser ver a saída do detector de bordas Cammy
        //Imgproc.cvtColor(imagemComBordasEmEvidencia, imagemDePreview, Imgproc.COLOR_GRAY2BGR, 4); if (true) return false;
//        imagemDePreview = imagemComBordasEmEvidencia; if (true) return false;

        List<MatOfPoint> contornos = detectarContornos(imagemComBordasEmEvidencia);

        if (contornos.isEmpty()) {
            Log.i(TestsActivity.TAG, "> Processamento de imagem: contornos não foram encontrados.");
            return false;
        }

        // Se quiser ver a saída do detector de contornos
        //Imgproc.drawContours(imagemDePreview, contornos, -1, new Scalar(0, 0, 255), 2); if (true) return false;

        List<MatOfPoint> quadrilateros = detectarQuadrilateros(contornos);

        if (quadrilateros.isEmpty()) {
            Log.i(TestsActivity.TAG, "> Processamento de imagem: quadriláteros não foram encontrados.");
            return false;
        }

        //Se quiser ver a saída do detector de quadriláteros
        //for (MatOfPoint quadrilatero : quadrilateros) { List<MatOfPoint> listaContorno = new ArrayList<MatOfPoint>(); listaContorno.add(quadrilatero); Imgproc.drawContours(imagemDePreview, listaContorno, -1, new Scalar(255, 0, 0), 3); } if (true) return false;

        MatOfPoint quadrilateroDoTabuleiro = detectarTabuleiro(quadrilateros);

        if (quadrilateroDoTabuleiro == null) {
            Log.i(TestsActivity.TAG, "> Processamento de imagem: quadrilátero do tabuleiro não foi encontrado.");
            return false;
        }

        QuadrilateralHierarchy quadrilateralHierarchy = new QuadrilateralHierarchy(quadrilateros);
        double areaMedia = 0;
        for (MatOfPoint quadrilatero : quadrilateralHierarchy.hierarquia.get(quadrilateroDoTabuleiro)) {
            areaMedia += Imgproc.contourArea(quadrilatero);
        }
        areaMedia /= quadrilateralHierarchy.hierarquia.get(quadrilateroDoTabuleiro).size();
        double areaDoTabuleiro = Imgproc.contourArea(quadrilateroDoTabuleiro);
        double razao = areaMedia / areaDoTabuleiro;
//        Log.d(TestsActivity.TAG, "Razão entre a área dos quadrados internos e a área do tabuleiro = " + razao);

        // Determina a dimensão do tabuleiro de acordo com a razão da área dos quadrados internos
        // com a área do quadrado do tabuleiro
        if (razao <= 1.0 / 324.0) {     // 18 quadrados por 18
            dimensaoDoTabuleiro = 19;
        }
        else if (razao <= 1.0 / 144.0) {
            dimensaoDoTabuleiro = 13;   // 12 quadrados por 12
        }
        else {
            dimensaoDoTabuleiro = 9;
        }

        List<Point> cantosDoTabuleiro = ordenarCantos(quadrilateroDoTabuleiro);

        if (desenharPreview) {
//            Drawer.desenhaInterseccoesECantosDoTabuleiro(imagemDePreview, intersecoes, cantosDoTabuleiro);
            Drawer.drawBoardContour(imagemDePreview, quadrilateroDoTabuleiro);
        }

        posicaoDoTabuleiroNaImagem = new Mat(4, 1, CvType.CV_32FC2);
        posicaoDoTabuleiroNaImagem.put(0, 0,
                (int) cantosDoTabuleiro.get(0).x, (int) cantosDoTabuleiro.get(0).y,
                (int) cantosDoTabuleiro.get(1).x, (int) cantosDoTabuleiro.get(1).y,
                (int) cantosDoTabuleiro.get(2).x, (int) cantosDoTabuleiro.get(2).y,
                (int) cantosDoTabuleiro.get(3).x, (int) cantosDoTabuleiro.get(3).y);

        /*
        for (int i = 0; i < posicaoDoTabuleiroNaImagem.rows(); ++i) {
            for (int j = 0; j < posicaoDoTabuleiroNaImagem.cols(); ++j) {
                double[] valor = posicaoDoTabuleiroNaImagem.get(i, j);
                Log.d(TestsActivity.TAG, "(" + i + ", " + j + ") = " + valor[0] + ", " + valor[1]);
            }
        }
        */

        processadoComSucesso = true;

        return true;
    }

    private Mat detectarBordas() {
        Mat imagemIntermediaria = new Mat();
        //Imgproc.Canny(imagem, mIntermediateMat, 80, 90);
        //Imgproc.Canny(imagem, mIntermediateMat, 35, 70);
        //Imgproc.Canny(imagem, mIntermediateMat, 30, 90);

        // Não parece que o filtro gaussiano ajudou muito a diminuir os ruídos das imagens
//        Size size = new Size(5, 5);
//        Imgproc.GaussianBlur(imagem, mIntermediateMat, size, 2);

        Imgproc.Canny(imagem, imagemIntermediaria, 30, 100);
//        Imgproc.Canny(imagem, mIntermediateMat, 30, 100);
        //Imgproc.Canny(imagem, mIntermediateMat, 45, 100);
        //Imgproc.Canny(imagem, mIntermediateMat, 50, 100);   // Melhores resultados até agora
        //Imgproc.Canny(imagem, mIntermediateMat, 100, 200);    // Fica bem limpo, mas perde alguns contornos válidos
        //Imgproc.Canny(imagem, mIntermediateMat, 75, 150); // Ainda perde alguns contornos
        //Imgproc.Canny(imagem, mIntermediateMat, 65, 130);

//        Imgproc.Canny(imagem, imagemIntermediaria, 40, 110);

        Imgproc.dilate(imagemIntermediaria, imagemIntermediaria, Mat.ones(3, 3, CvType.CV_32F));
//        Imgproc.dilate(imagemIntermediaria, imagemIntermediaria, new Mat());
        return imagemIntermediaria;
    }

    private List<MatOfPoint> detectarContornos(Mat imagemComBordasEmEvidencia) {
        // Os contornos delimitados pelas linhas são encontrados
        List<MatOfPoint> contornos = new ArrayList<>();
        Mat hierarquia = new Mat();
        Imgproc.findContours(imagemComBordasEmEvidencia, contornos, hierarquia, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        Log.d("kifu-recorder", "Número de contornos encontrados: " + contornos.size());

        // Remove os contornos muito pequenos, que provavelmente são ruído
        for (Iterator<MatOfPoint> it = contornos.iterator(); it.hasNext();) {
            MatOfPoint contorno = it.next();
            // Com 1000 já perde os quadrados menores no tabuleiro 19x19
            // O ideal seria fazer isto aqui como uma razão sobre a área da imagem
            if (Imgproc.contourArea(contorno) < 700) {
                it.remove();
            }
        }

        // A imagem é convertida para um formato colorido novamente
        Imgproc.cvtColor(imagemComBordasEmEvidencia, imagem, Imgproc.COLOR_GRAY2BGR, 4);
        imagemComBordasEmEvidencia.release();
        return contornos;
    }

    private List<MatOfPoint> detectarQuadrilateros(List<MatOfPoint> contornos) {
        List<MatOfPoint> quadrilateros = new ArrayList<>();

        for (MatOfPoint contorno : contornos) {
            MatOfPoint2f contour2f = new MatOfPoint2f();
            MatOfPoint2f approx2f = new MatOfPoint2f();
            contorno.convertTo(contour2f, CvType.CV_32FC2);
            // * 0.02 e * 0.03 também têm resultados interessantes
            // Aparentemente, quanto maior o epsilon, mais curvas que não se encaixam perfeitamente nos contornos
            // são consideradas. Contudo, esse parâmetro parece ser bastante sensível.
            //Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.04, true);
//            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.008, true);  // perde muitos quadrados
//            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.01, true);  // borda do tabuleiro encontrada bem, mas quadrados internos sofrem. Talvez seja melhor usar este por detectar melhor o quadrado externo
//            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.015, true);  // melhores resultados até agora
            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.012, true);
//            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.008, true);

            MatOfPoint approx = new MatOfPoint();
            approx2f.convertTo(approx, CvType.CV_32S);
            double contourArea = Math.abs(Imgproc.contourArea(approx2f));

            // Se tem 4 lados, é convexo e não é muito pequeno, é um quadrado válido
            if (approx2f.toList().size() == 4 &&
                    contourArea > 400 &&
                    Imgproc.isContourConvex(approx)) {
                quadrilateros.add(approx);
            }
        }

        Log.d("kifu-recorder", "Número de quadriláteros encontrados: " + quadrilateros.size());
        return quadrilateros;
    }

    private MatOfPoint detectarTabuleiro(List<MatOfPoint> quadrilateros) {
        QuadrilateralHierarchy quadrilateralHierarchy = new QuadrilateralHierarchy(quadrilateros);

        MatOfPoint contornoMaisProximoDoTabuleiro = null;
        int numeroDeFilhos = 9999;
        // Tem que ter pelo menos esse número de quadriláteros folha dentro
        int threshold = 10;

        for (MatOfPoint contorno : quadrilateralHierarchy.externos) {
            if (quadrilateralHierarchy.hierarquia.get(contorno).size() < numeroDeFilhos &&
                    quadrilateralHierarchy.hierarquia.get(contorno).size() > threshold) {
                contornoMaisProximoDoTabuleiro = contorno;
                numeroDeFilhos = quadrilateralHierarchy.hierarquia.get(contorno).size();
            }
        }

//        if (desenharPreview) {
//            Drawer.drawRelevantContours(imagemDePreview, quadrilateralHierarchy, contornoMaisProximoDoTabuleiro);
//        }

        return contornoMaisProximoDoTabuleiro;
    }

    private List<Point> ordenarCantos(MatOfPoint quadrilateroDoTabuleiro) {
        List<Point> cantos = new ArrayList<>();
        cantos.add(quadrilateroDoTabuleiro.toArray()[0]);
        cantos.add(quadrilateroDoTabuleiro.toArray()[3]);
        cantos.add(quadrilateroDoTabuleiro.toArray()[2]);
        cantos.add(quadrilateroDoTabuleiro.toArray()[1]);
        return cantos;
    }

    public int getDimensaoDoTabuleiro() {
        if (!processadoComSucesso) {
            // lançar erro
        }
        return dimensaoDoTabuleiro;
    }

    public Mat getPosicaoDoTabuleiroNaImagem() {
        if (!processadoComSucesso) {
            // lançar erro
        }
        return posicaoDoTabuleiroNaImagem;
    }

}
