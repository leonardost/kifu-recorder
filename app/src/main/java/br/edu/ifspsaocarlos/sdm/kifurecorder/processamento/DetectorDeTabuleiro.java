package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * Detecta a posição de um tabuleiro em uma imagem e sua dimensão (9x9, 13x13 ou 19x19)
 */
public class DetectorDeTabuleiro {

    // Imagem da câmera
    private Mat imagem;
    private Mat imagemDePreview;

    // Atributos calculados pela classe
    private boolean processadoComSucesso = false;
    private int dimensaoDoTabuleiro;
    private Mat posicaoDoTabuleiroNaImagem;
    private boolean desenharPreview = false;

    public DetectorDeTabuleiro(boolean desenharPreview) {
        this.desenharPreview = desenharPreview;
    }

    public boolean isProcessadoComSucesso() {
        return processadoComSucesso;
    }

    public void setImagem(Mat imagem) {
        this.imagem = imagem;
    }

    public void setImagemDePreview(Mat imagemDePreview) {
        this.imagemDePreview = imagemDePreview;
    }

    public void processar() {
        if (imagem == null) {
            // lançar erro
        }

        Mat imagemComBordasEmEvidencia = detectarBordas();
        List<MatOfPoint> contornos = detectarContornos(imagemComBordasEmEvidencia);

        if (contornos.isEmpty()) {
            return;
        }

        List<MatOfPoint> quadrilateros = detectarQuadrilateros(contornos);

        if (quadrilateros.isEmpty()) {
            return;
        }

        MatOfPoint quadrilateroDoTabuleiro = detectarTabuleiro(quadrilateros);

        if (quadrilateroDoTabuleiro == null) {
            return;
        }

        List<ClusterDeVertices> intersecoes = detectarIntersecoes(quadrilateros, quadrilateroDoTabuleiro);

        processadoComSucesso = true;

        if (intersecoes.size() > 13 * 13) {
            dimensaoDoTabuleiro = 19;
        }
        else if (intersecoes.size() > 9 * 9) {
            dimensaoDoTabuleiro = 13;
        }
        else {
            dimensaoDoTabuleiro = 9;
        }

        List<Point> cantosDoTabuleiro = ordenarCantos(quadrilateroDoTabuleiro);

        if (desenharPreview) {
            Desenhista.desenhaInterseccoesECantosDoTabuleiro(imagemDePreview, intersecoes, cantosDoTabuleiro);
        }

        posicaoDoTabuleiroNaImagem = new Mat(4, 1, CvType.CV_32FC2);
        posicaoDoTabuleiroNaImagem.put(0, 0,
                (int) cantosDoTabuleiro.get(0).x, (int) cantosDoTabuleiro.get(0).y,
                (int) cantosDoTabuleiro.get(1).x, (int) cantosDoTabuleiro.get(1).y,
                (int) cantosDoTabuleiro.get(2).x, (int) cantosDoTabuleiro.get(2).y,
                (int) cantosDoTabuleiro.get(3).x, (int) cantosDoTabuleiro.get(3).y);
    }

    private Mat detectarBordas() {
        Mat mIntermediateMat = new Mat();
        //Imgproc.Canny(rgbaInnerWindow, mIntermediateMat, 80, 90);
        Imgproc.Canny(imagem, mIntermediateMat, 35, 70);
        Imgproc.dilate(mIntermediateMat, mIntermediateMat, new Mat());
        return mIntermediateMat;
    }

    private List<MatOfPoint> detectarContornos(Mat imagemComBordasEmEvidencia) {
        // Os contornos delimitados pelas linhas são encontrados
        List<MatOfPoint> contornos = new ArrayList<>();
        Mat hierarquia = new Mat();
        Imgproc.findContours(imagemComBordasEmEvidencia, contornos, hierarquia, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        Log.d("kifu-recorder", "Número de contornos encontrados: " + contornos.size());

        // A imagem é convertida para um formato colorido novamente
//        imagemComBordasEmEvidencia.release();
        Imgproc.cvtColor(imagemComBordasEmEvidencia, imagem, Imgproc.COLOR_GRAY2BGR, 4);
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
            Imgproc.approxPolyDP(contour2f, approx2f, Imgproc.arcLength(contour2f, true) * 0.04, true);

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
        HierarquiaDeQuadrilateros hierarquiaDeQuadrilateros = new HierarquiaDeQuadrilateros(quadrilateros);

        MatOfPoint contornoMaisProximoDoTabuleiro = null;
        int numeroDeFilhos = 9999;
        // Tem que ter pelo menos esse número de quadriláteros folha dentro
        int threshold = 10;

        for (MatOfPoint contorno : hierarquiaDeQuadrilateros.externos) {
            if (hierarquiaDeQuadrilateros.hierarquia.get(contorno).size() < numeroDeFilhos &&
                    hierarquiaDeQuadrilateros.hierarquia.get(contorno).size() > threshold) {
                contornoMaisProximoDoTabuleiro = contorno;
                numeroDeFilhos = hierarquiaDeQuadrilateros.hierarquia.get(contorno).size();
            }
        }

        if (desenharPreview) {
            Desenhista.desenharContornosRelevantes(imagemDePreview, hierarquiaDeQuadrilateros, contornoMaisProximoDoTabuleiro);
        }

        return contornoMaisProximoDoTabuleiro;
    }

    private boolean possivelContornoDoTabuleiroFoiDetectado(HierarquiaDeQuadrilateros hierarquiaDeQuadrilateros, MatOfPoint contornoMaisProximoDoTabuleiro) {
        if (hierarquiaDeQuadrilateros.externos.size() == 0 || contornoMaisProximoDoTabuleiro == null) {
            return false;
        }
        return true;
    }

    private List<ClusterDeVertices> detectarIntersecoes(List<MatOfPoint> quadrilateros, MatOfPoint quadrilateroDoTabuleiro) {
        HierarquiaDeQuadrilateros hierarquiaDeQuadrilateros = new HierarquiaDeQuadrilateros(quadrilateros);
        List<ClusterDeVertices> intersecoes = new ArrayList<>();

        // Agrupa os vértices dos quadriláteros internos para encontrar as interseções do tabuleiro
        for (MatOfPoint quadradoInterno : hierarquiaDeQuadrilateros.hierarquia.get(quadrilateroDoTabuleiro)) {
            List<Point> vertices = quadradoInterno.toList();
            boolean encontrouClusterProximo = false;
            for (Point vertice : vertices) {
                for (ClusterDeVertices intersecaoMedia : intersecoes) {
                    if (intersecaoMedia.distanceTo(vertice) < 20) {  // Valor totalmente arbitrário, apenas para testes
                        intersecaoMedia.combinarPontoSeEstaProximoOSuficiente(vertice, 20);
                        encontrouClusterProximo = true;
                        break;
                    }
                }
                if (!encontrouClusterProximo) {
                    ClusterDeVertices novoCluster = new ClusterDeVertices();
                    novoCluster.combinarPontoSeEstaProximoOSuficiente(vertice, 20);
                    intersecoes.add(novoCluster);
                }
            }
        }

        return intersecoes;
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
