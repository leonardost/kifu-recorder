/**
 * Existem melhoramentos de código e melhoramentos de funcionalidade, os
 * segundos são muito, muito mais importantes que os primeiros. Que
 * funcionalidades ainda precisam ser melhoradas?
 *
 * - Detecção do tabuleiro
 *   - Detecção das bordas
 *   - Detecção das interseções
 * - Detecção de pedras no tabuleiro (cuidar de sombras, etc.)
 *   - Diferenciar as cores do tabuleiro das pedras
 *     - Clusterização?
 */
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

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Tabuleiro;

/**
 * Responsável por processar um frame de imagem da câmera e gerar um objeto
 * Tabuleiro correspondente.
 */
public class ProcessadorDeImagem {

    public static final String TAG = "RegistradorDePartidas";

    private int larguraImagemPreview = 500;
    private int alturaImagemPreview = 500;

    /**
     * Detecta um tabuleiro de Go e as pedras que nele estão a partir de uma imagem e retorna um
     * objeto Tabuleiro correspondente.
     *
     * @param imagem
     * @return Objeto Tabuleiro com as pedras detectadas
     */
    public Tabuleiro detectarTabuleiro(Mat imagem) {

        DetectorDeTabuleiro detectorDeTabuleiro = new DetectorDeTabuleiro(true);



        Mat original = imagem.clone();

        List<MatOfPoint> contornos = detectarBordasEContornos(imagem);
        if (contornos == null) {
            return null;
        }

        // Encontra os quadriláteros entre os contornos obtidos.
        // http://stackoverflow.com/questions/8667818/opencv-c-obj-c-detecting-a-sheet-of-paper-square-detection
        List<MatOfPoint> quadrilateros = new ArrayList<>();
        MatOfPoint maiorQuadrilatero = null;
        encontrarQuadrilateros(contornos, quadrilateros, maiorQuadrilatero);
        Log.d(TAG, "Número de quadriláteros encontrados: " + quadrilateros.size());

        // Constrói a hierarquiaDeQuadrilateros
        HierarquiaDeQuadrilateros hierarquiaDeQuadrilateros = new HierarquiaDeQuadrilateros(quadrilateros);

        MatOfPoint contornoDoTabuleiro =
                encontrarContornoMaisProximoDoTabuleiro(hierarquiaDeQuadrilateros);

        Desenhista.desenharContornosRelevantes(imagem, hierarquiaDeQuadrilateros, contornoDoTabuleiro);

        if (!possivelContornoDoTabuleiroFoiDetectado(hierarquiaDeQuadrilateros, contornoDoTabuleiro)) {
            return null;
        }
        return detectarInterseccoes(imagem, hierarquiaDeQuadrilateros, contornoDoTabuleiro, original);
    }

    private List<MatOfPoint> detectarBordasEContornos(Mat imagem) {
        Mat mIntermediateMat = new Mat();

        // Primeiro, é passado o detector de bordas Canny
        // TODO: Ajustar esses parâmetros
        //Imgproc.Canny(rgbaInnerWindow, mIntermediateMat, 80, 90);
        Imgproc.Canny(imagem, mIntermediateMat, 35, 70);
        Imgproc.dilate(mIntermediateMat, mIntermediateMat, new Mat());

        // Os contornos delimitados pelas linhas são encontrados
        List<MatOfPoint> contours = detectarContornos(mIntermediateMat);
        // A imagem é convertida para um formato colorido novamente
        Imgproc.cvtColor(mIntermediateMat, imagem, Imgproc.COLOR_GRAY2BGR, 4);

        return contours;
    }

    private List<MatOfPoint> detectarContornos(Mat imagemProcessadaPorFiltroCanny) {
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(imagemProcessadaPorFiltroCanny, contours, hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));
        Log.d(TAG, "Number of contours found: " + contours.size());
        return contours;
    }

    /**
     * Encontra todos os quadriláteros presentes entre os contornos fornecidos em 'contornos'.
     * Retorna a lista de todos os quadriláteros em 'quadrilateros' e o maior quadrilátero em
     * 'maiorQuadrilatero'.
     *
     * @param contornos
     * @param quadrilateros
     * @param maiorQuadrilatero
     */
    private void encontrarQuadrilateros(List<MatOfPoint> contornos, List<MatOfPoint> quadrilateros,
                                        MatOfPoint maiorQuadrilatero) {
        double largestArea = 0;

        for (MatOfPoint contour : contornos) {
            MatOfPoint2f contour2f = new MatOfPoint2f();
            MatOfPoint2f approx2f = new MatOfPoint2f();
            contour.convertTo(contour2f, CvType.CV_32FC2);
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

                if (contourArea > largestArea) {
                    largestArea = Math.abs(Imgproc.contourArea(approx2f));
                    maiorQuadrilatero = approx;
                }

            }
        }
    }

    /**
     * O contorno provavelmente mais próximo do tabuleiro é um quadrilátero
     * externo que tem somente quadriláteros folha dentro de si.
     *
     * TODO: Isto poderia ser movido para a classe HierarquiaDeQuadrilateros?
     */
    private MatOfPoint encontrarContornoMaisProximoDoTabuleiro(HierarquiaDeQuadrilateros hierarquiaDeQuadrilateros) {
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

        return contornoMaisProximoDoTabuleiro;
    }

    private boolean possivelContornoDoTabuleiroFoiDetectado(HierarquiaDeQuadrilateros hierarquiaDeQuadrilateros, MatOfPoint contornoMaisProximoDoTabuleiro) {
        if (hierarquiaDeQuadrilateros.externos.size() == 0 || contornoMaisProximoDoTabuleiro == null) {
            return false;
        }
        return true;
    }

    // Próxima etapa da detecção do tabuleiro, a detecção das intersecções das linhas encontradas
    private Tabuleiro detectarInterseccoes(Mat imagem, HierarquiaDeQuadrilateros hierarquiaDeQuadrilateros, MatOfPoint contornoMaisProximoDoTabuleiro, Mat imagemOriginal) {
        // Encontra as intersecções do tabuleiro
        List<ClusterDeVertices> intersecoesMedias =
                encontrarInterseccoes(hierarquiaDeQuadrilateros, contornoMaisProximoDoTabuleiro);

//        List<Point> cantos = ordenarCantos(contornoMaisProximoDoTabuleiro);
        List<Point> cantos = ordenarCantos2(contornoMaisProximoDoTabuleiro);

        Desenhista.desenhaInterseccoesECantosDoTabuleiro(imagem, intersecoesMedias, cantos);

        Mat tabuleiroCorrigido = transformaTabuleiroEDesenhaImagemDePreviewTransformada(imagem, imagemOriginal, cantos);

        return detectarPedrasNoTabuleiro(tabuleiroCorrigido);
    }

    private List<ClusterDeVertices> encontrarInterseccoes(HierarquiaDeQuadrilateros hierarquiaDeQuadrilateros, MatOfPoint contornoMaisProximoDoTabuleiro) {
        List<ClusterDeVertices> intersecoesMedias = new ArrayList<>();

        // Agrupa os vértices dos quadriláteros internos para encontrar as interseções do tabuleiro
        for (MatOfPoint quadradoInterno : hierarquiaDeQuadrilateros.hierarquia.get(contornoMaisProximoDoTabuleiro)) {
            List<Point> vertices = quadradoInterno.toList();
            boolean encontrouClusterProximo = false;
            for (Point vertice : vertices) {
                for (ClusterDeVertices intersecaoMedia : intersecoesMedias) {
                    if (intersecaoMedia.distanceTo(vertice) < 20) {  // Valor totalmente arbitrário, apenas para testes
                        intersecaoMedia.combinarPontoSeEstaProximoOSuficiente(vertice, 20);
                        encontrouClusterProximo = true;
                        break;
                    }
                }
                if (!encontrouClusterProximo) {
                    ClusterDeVertices novoCluster = new ClusterDeVertices();
                    novoCluster.combinarPontoSeEstaProximoOSuficiente(vertice, 20);
                    intersecoesMedias.add(novoCluster);
                }
            }
        }

        return intersecoesMedias;
    }

    private List<Point> ordenarCantos(MatOfPoint contorno) {

        Log.d(TAG, "Ordem original:");
        for (Point p : contorno.toList()) {
            Log.d(TAG, "" + p);
        }

        Point center = new Point();
        center.x = 0;
        center.y = 0;
        for (Point p : contorno.toList()) {
            center.x += p.x;
            center.y += p.y;
        }
        center.x /= contorno.toList().size();
        center.y /= contorno.toList().size();

        List<Point> top = new ArrayList<>();
        List<Point> bot = new ArrayList<>();

        // Ocorre um problema se os pontos estão exatamente na linha do centro
        for (int i = 0; i < contorno.toArray().length; i++) {
            if (contorno.toArray()[i].y < center.y)
                top.add(contorno.toArray()[i]);
            else
                bot.add(contorno.toArray()[i]);
        }

        Point tl = top.get(0).x > top.get(1).x ? top.get(1) : top.get(0);
        Point tr = top.get(0).x > top.get(1).x ? top.get(0) : top.get(1);
        Point bl = bot.get(0).x > bot.get(1).x ? bot.get(1) : bot.get(0);
        Point br = bot.get(0).x > bot.get(1).x ? bot.get(0) : bot.get(1);

        List<Point> cantos = new ArrayList<>();
        cantos.add(tl);
        cantos.add(tr);
        cantos.add(br);
        cantos.add(bl);

        return cantos;
    }

    /**
     * Parece que não precisa ordenar os vértices porque eles já vem em ordem. Só precisa inverter a
     * ordem, porque está no sentido anti-horário.
     */
    private List<Point> ordenarCantos2(MatOfPoint contornoMaisProximoDoTabuleiro) {
        Log.d(TAG, "Ordem original:");
        for (Point p : contornoMaisProximoDoTabuleiro.toList()) {
            Log.d(TAG, "" + p);
        }

        List<Point> cantos = new ArrayList<>();
        cantos.add(contornoMaisProximoDoTabuleiro.toArray()[0]);
        cantos.add(contornoMaisProximoDoTabuleiro.toArray()[3]);
        cantos.add(contornoMaisProximoDoTabuleiro.toArray()[2]);
        cantos.add(contornoMaisProximoDoTabuleiro.toArray()[1]);
        return cantos;
    }

    // TODO: Este método faz coisas demais, quebrar as duas funcionalidades dele
    private Mat transformaTabuleiroEDesenhaImagemDePreviewTransformada(Mat imagem, Mat imagemOriginal, List<Point> cantos) {
        Mat tabuleiroCorrigido = imagem.submat(0, alturaImagemPreview + 1, 0, larguraImagemPreview + 1);

        Mat cantosOriginais = new Mat(4, 1, CvType.CV_32FC2);
        Mat cantosTransformados = new Mat(4, 1, CvType.CV_32FC2);
        cantosOriginais.put(0, 0,
                (int) cantos.get(0).x, (int) cantos.get(0).y,
                (int) cantos.get(1).x, (int) cantos.get(1).y,
                (int) cantos.get(2).x, (int) cantos.get(2).y,
                (int) cantos.get(3).x, (int) cantos.get(3).y);
        cantosTransformados.put(0, 0,
                0, 0,
                larguraImagemPreview, 0,
                larguraImagemPreview, alturaImagemPreview,
                0, alturaImagemPreview);

        Mat matrizDeTransformacao = Imgproc.getPerspectiveTransform(cantosOriginais, cantosTransformados);

        // "Travar" a posição do tabuleiro quando tiver detectado corretamente
        Imgproc.warpPerspective(imagemOriginal, tabuleiroCorrigido, matrizDeTransformacao, tabuleiroCorrigido.size());

        return tabuleiroCorrigido;
    }

    /**
     * A partir da imagem do tabuleiro com a perspectiva corrigida, verifica a cor predominante em
     * cada intersecção e detecta se há uma pedra preta, branca, ou nenhuma.
     *
     * Recuperar cores predominantes em cada uma das intersecções.
     * TODO: Usar função gaussiana para dar mais importância para os valores que estõa próximos do
     *       centro.
     *
     * @param imagemTabuleiroCorrigido
     * @return
     */
    private Tabuleiro detectarPedrasNoTabuleiro(Mat imagemTabuleiroCorrigido) {
        Tabuleiro tabuleiro = new Tabuleiro(9);

        double[] corMediaDoTabuleiro = corMediaDoTabuleiro(imagemTabuleiroCorrigido);
        Log.d(TAG, "Cor média do tabuleiro: (" + corMediaDoTabuleiro[0] + ", " +
                corMediaDoTabuleiro[1] + ", " +
                corMediaDoTabuleiro[2] + ")");

        for (int i = 0; i < 9; ++i) {
            for (int j = 0; j < 9; ++j) {
                double[] color = recuperarCorPredominanteNaPosicao(
                        (i * alturaImagemPreview / 8),
                        (j * larguraImagemPreview / 8),
                        imagemTabuleiroCorrigido
                );
                int hipotese = hipoteseDeCor(color, corMediaDoTabuleiro);
                if (hipotese != Tabuleiro.VAZIO) {
                    tabuleiro.colocarPedra(i, j, hipotese);
                }
            }
        }

        Desenhista.desenhaLinhasNoPreview(imagemTabuleiroCorrigido, larguraImagemPreview, alturaImagemPreview);

        return tabuleiro;
    }

    // TODO: Transformar hipóteses de recuperação de cor em classes separadas
    private double[] recuperarCorPredominanteNaPosicao(int linha, int coluna, Mat imagem) {
        int hipotese = 2;
        double[] color = new double[4];
        switch (hipotese) {
            case 1:
                color = imagem.get(linha, coluna);
                break;
            case 2:
                color = recuperarMediaGaussianaDeCores(imagem, linha, coluna);
                break;
        }
        Log.i(TAG, "Pos(" + linha + ", " + coluna + ") = " + color[0] + ", " + color[1] + ", " + color[2] + ", " + color[3]);
        return color;
    }

    private double[] recuperarMediaGaussianaDeCores(Mat imagem, int x, int y) {
        double[] color = new double[] {0.0, 0.0, 0.0, 0.0};
        int radius = 10;
        int contador = 0;

        for (int yy = y - radius; yy <= y + radius; ++yy) {
            if (yy < 0 || yy >= imagem.height()) continue;
            for (int xx = x - radius; xx <= x + radius; ++xx) {
                if (xx < 0 || xx >= imagem.width()) continue;
                if (distance(xx, yy, x, y) < radius) {
                    double c[] = imagem.get(yy, xx);
                    color[0] += c[0];
                    color[1] += c[1];
                    color[2] += c[2];
                    color[3] += c[3];
                    ++contador;
                }
            }
        }

        color[0] /= contador;
        color[1] /= contador;
        color[2] /= contador;
        color[3] /= contador;

        return color;
    }

    private double distance(int x, int y, int x2, int y2) {
        return Math.sqrt((x - x2) * (x - x2) + (y - y2) * (y - y2));
    }

    /**
     * Verifica se a cor esta mais proxima de preto ou branco. Este metodo vai precisar de um
     * parametro de ajuste por causa da cor do tabuleiro e das condiçoes de iluminaçao do local.
     * O parametro poderia ser a cor media do tabuleiro todo.
     *
     * @param cor
     * @return
     */
    private int hipoteseDeCor(double[] cor, double[] corMediaDoTabuleiro) {
        double[] preto = {0.0, 0.0, 0.0, 255.0};
        double[] branco = {255.0, 255.0, 255.0, 255.0};
        double distanciaParaPreto = distanciaDeCor(cor, preto);
        double distanciaParaBranco = distanciaDeCor(cor, branco);
        double distanciaParaCorMedia = distanciaDeCor(cor, corMediaDoTabuleiro);

        Log.d(TAG, "distancia para preto = " + distanciaParaPreto);
        Log.d(TAG, "distancia para branco = " + distanciaParaBranco);
        Log.d(TAG, "distancia para media = " + distanciaParaCorMedia);

        // Se a distância para a média for menor que um certo threshold, muito provavelmente é uma
        // intersecção vazia
        if (distanciaParaCorMedia < 120) {
            return Tabuleiro.VAZIO;
        }

        if (distanciaParaPreto < distanciaParaBranco) {
            return Tabuleiro.PEDRA_PRETA;
        }
        else {
            return Tabuleiro.PEDRA_BRANCA;
        }

/*
        if (distanciaParaPreto < distanciaParaBranco && distanciaParaPreto < distanciaParaCorMedia) {
            return Tabuleiro.PEDRA_PRETA;
        }
        else if (distanciaParaBranco < distanciaParaPreto && distanciaParaBranco < distanciaParaCorMedia) {
            return Tabuleiro.PEDRA_BRANCA;
        }
        return Tabuleiro.VAZIO;
        */
    }

    private double distanciaDeCor(double[] cor1, double[] cor2) {
        double distancia = 0;
        for (int i = 0; i < 4; ++i) {
            distancia += Math.abs(cor1[i] - cor2[i]);
        }
        return distancia;
    }

    /**
     * Retorna a cor media do tabuleiro. Deve haver uma forma melhor de fazer isso.
     *
     * @param imagemDoTabuleiro
     * @return
     */
    private double[] corMediaDoTabuleiro(Mat imagemDoTabuleiro) {
        double media[] = new double[] {0.0, 0.0, 0.0, 0.0};

        for (int y = 0; y < imagemDoTabuleiro.height(); ++y) {
            for (int x = 0; x < imagemDoTabuleiro.width(); ++x) {
                double c[] = imagemDoTabuleiro.get(y, x);
                media[0] += c[0];
                media[1] += c[1];
                media[2] += c[2];
                media[3] += c[3];
            }
        }

        media[0] /= imagemDoTabuleiro.height() * imagemDoTabuleiro.width();
        media[1] /= imagemDoTabuleiro.height() * imagemDoTabuleiro.width();
        media[2] /= imagemDoTabuleiro.height() * imagemDoTabuleiro.width();
        media[3] /= imagemDoTabuleiro.height() * imagemDoTabuleiro.width();

        return media;
    }

}
