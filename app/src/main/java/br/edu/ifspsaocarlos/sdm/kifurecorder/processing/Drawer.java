package br.edu.ifspsaocarlos.sdm.kifurecorder.processing;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

import br.edu.ifspsaocarlos.sdm.kifurecorder.models.Board;
import br.edu.ifspsaocarlos.sdm.kifurecorder.models.Move;

/**
 * Classe responsável por colocar a imagem de saída nas matrizes de imagem.
 */
public class Drawer {

    private static Scalar mBlack = new Scalar(  0,   0,   0);
    private static Scalar mWhite = new Scalar(255, 255, 255);
    private static Scalar mRed   = new Scalar(255,   0,   0);
    private static Scalar mGreen = new Scalar(  0, 255,   0);
    private static Scalar mBlue  = new Scalar(  0,   0, 255);
    private static Scalar mBoardBrown = new Scalar(219, 176, 105);

    private static Scalar[] colors = new Scalar[] {
        mRed, mGreen, mBlue, mWhite
    };

    public static void desenharContornosRelevantes(Mat imagem, QuadrilateralHierarchy quadrilateralHierarchy, MatOfPoint contornoMaisProximoDoTabuleiro) {
        // Desenha os quadriláteros folha em verde
        if (quadrilateralHierarchy.folhas.size() > 0) {
            Imgproc.drawContours(imagem, quadrilateralHierarchy.folhas, -1, mGreen, 2);
        }

        // Desenha os quadriláteros externos em azul
        if (quadrilateralHierarchy.externos.size() > 0) {
            Imgproc.drawContours(imagem, quadrilateralHierarchy.externos, -1, mBlue, 2);
        }

        // Desenha o contorno do tabuleiro em vermelho
        if (contornoMaisProximoDoTabuleiro != null) {
            desenharContorno(imagem, contornoMaisProximoDoTabuleiro, mRed);
        }
    }

    private static void desenharContorno(Mat imagem, MatOfPoint contorno, Scalar cor) {
        List<MatOfPoint> listaContorno = new ArrayList<MatOfPoint>();
        listaContorno.add(contorno);
        Imgproc.drawContours(imagem, listaContorno, -1, cor, 2);
    }

    public static void desenharContornoDoTabuleiro(Mat imagem, MatOfPoint contornoDoTabuleiro) {
        List<MatOfPoint> listaContorno = new ArrayList<MatOfPoint>();
        listaContorno.add(contornoDoTabuleiro);
        Imgproc.drawContours(imagem, listaContorno, -1, mRed, 6);
    }

    public static void drawLostBoardContour(Mat imagem, MatOfPoint boardContour) {
        List<MatOfPoint> contour = new ArrayList<MatOfPoint>();
        contour.add(boardContour);
        Imgproc.drawContours(imagem, contour, -1, mBlue, 6);
    }

    /**
     * Desenha o tabuleiro sobre a matriz 'imagem' com a origem nas coordenadas 'x' e 'y' passadas
     * como parâmetro e com tamanho 'tamanhoImagem'. O desenho é feito respeitando a dimensão do
     * tabuleiro, ou seja, se o tabuleiro tem dimensão maior, o preview fica menor. A última jogada
     * é marcada com um círculo sobre a última pedra que foi colocada. Se o parâmetro ultimaJogada
     * for nulo, não marca a última jogada.
     *
     * @param imagem
     * @param board
     * @param x
     * @param y
     * @param tamanhoImagem
     * @param lastMove
     */
    public static void desenharTabuleiro(Mat imagem, Board board, int x, int y, int tamanhoImagem, Move lastMove) {
        Point p1 = new Point();
        Point p2 = new Point();
        double distanciaEntreLinhas = tamanhoImagem / (board.getDimensao() + 1);
        double fimDasLinhas = tamanhoImagem - distanciaEntreLinhas;
        int raioDaPedra = 29 - board.getDimensao(); // estava usando tamanhoImagem / 20 para o 9x9
        p1.x = x;
        p1.y = y;
        p2.x = x + tamanhoImagem;
        p2.y = y + tamanhoImagem;

        Imgproc.rectangle(imagem, p1, p2, mBoardBrown, -1);

        // Desenha linhas horizontais
        for (int i = 0; i < board.getDimensao(); ++i) {
            Point inicio = new Point();
            Point fim = new Point();
            inicio.x = x + distanciaEntreLinhas;
            inicio.y = y + distanciaEntreLinhas + distanciaEntreLinhas * i;
            fim.x = x + fimDasLinhas;
            fim.y = inicio.y;
            Imgproc.line(imagem, inicio, fim, mBlack);
        }

        // Desenha linhas verticais
        for (int i = 0; i < board.getDimensao(); ++i) {
            Point inicio = new Point();
            Point fim = new Point();
            inicio.x = x + distanciaEntreLinhas + distanciaEntreLinhas * i;
            inicio.y = y + distanciaEntreLinhas;
            fim.x = inicio.x;
            fim.y = y + fimDasLinhas;
            Imgproc.line(imagem, inicio, fim, mBlack);
        }

        // Desenha pedras
        for (int i = 0; i < board.getDimensao(); ++i) {
            for (int j = 0; j < board.getDimensao(); ++j) {
                Point centro = new Point();
                centro.x = x + distanciaEntreLinhas + j * distanciaEntreLinhas;
                centro.y = y + distanciaEntreLinhas + i * distanciaEntreLinhas;
                if (board.getPosicao(i, j) == Board.PEDRA_PRETA) {
                    Imgproc.circle(imagem, centro, raioDaPedra, mBlack, -1);
                } else if (board.getPosicao(i, j) == Board.PEDRA_BRANCA) {
                    Imgproc.circle(imagem, centro, raioDaPedra, mWhite, -1);
                    Imgproc.circle(imagem, centro, raioDaPedra, mBlack);
                }
            }
        }

        // Marca a última jogada feita
        if (lastMove != null) {
            Point centro = new Point();
            centro.x = x + distanciaEntreLinhas + lastMove.coluna * distanciaEntreLinhas;
            centro.y = y + distanciaEntreLinhas + lastMove.linha * distanciaEntreLinhas;
            Scalar corDaMarcacao = lastMove.cor == Board.PEDRA_PRETA ? mWhite : mBlack;
            Imgproc.circle(imagem, centro, (int)(raioDaPedra * 0.6), corDaMarcacao, 1);
            Imgproc.circle(imagem, centro, raioDaPedra, mBlue, -1);
        }
    }

}
