package br.edu.ifspsaocarlos.sdm.kifurecorder;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

/**
 * Created by leo on 30/07/15.
 */
public class Tabuleiro {

    public final static int VAZIO = 0;
    public final static int PEDRA_PRETA = 1;
    public final static int PEDRA_BRANCA = 2;

    private int tamanho;
    private Integer[][] tabuleiro = new Integer[9][9];

    private Scalar mBlack = new Scalar(0, 0, 0);
    private Scalar mWhite = new Scalar(255, 255, 255);

    public Tabuleiro() {
    }

    public Tabuleiro(int tamanho) {
        this.tamanho = tamanho;
        this.tabuleiro = new Integer[tamanho][tamanho];
        for (int i = 0; i < tamanho; ++i) {
            for (int j = 0; j < tamanho; ++j) {
                tabuleiro[i][j] = VAZIO;
            }
        }

//        tabuleiro[2][3] = 2;
//        tabuleiro[4][4] = 1;
    }

    public void colocarPedra(int linha, int coluna, int pedra) {
        if (pedra != PEDRA_PRETA && pedra != PEDRA_BRANCA) {
            // Problema
            throw new RuntimeException("Pedra inválida!");
        }
        tabuleiro[linha][coluna] = pedra;
    }

    /**
     * Desenha o tabuleiro sobre a matriz 'imagem' com a origem nas coordenadas 'x' e 'y' passadas
     * como parâmetro e com tamanho 'tamanhoImagem'.
     *
     * @param imagem
     * @param x
     * @param y
     * @param tamanhoImagem
     */
    public void desenhar(Mat imagem, int x, int y, int tamanhoImagem) {
        Point p1 = new Point();
        Point p2 = new Point();
        p1.x = x;
        p1.y = y;
        p2.x = x + tamanhoImagem;
        p2.y = y + tamanhoImagem;

        Core.rectangle(imagem, p1, p2, mWhite, -1);

        for (int i = 0; i < tamanho; ++i) {
            Point inicio = new Point();
            Point fim = new Point();
            inicio.x = x + (tamanhoImagem / 10);
            inicio.y = y + (tamanhoImagem / 10) + (tamanhoImagem / 10) * i;
            fim.x = x + (tamanhoImagem * 0.9);
            fim.y = y + (tamanhoImagem / 10) + (tamanhoImagem / 10) * i;
            Core.line(imagem, inicio, fim, mBlack);
        }

        for (int i = 0; i < tamanho; ++i) {
            Point inicio = new Point();
            Point fim = new Point();
            inicio.x = x + (tamanhoImagem / 10) + (tamanhoImagem / 10) * i;
            inicio.y = y + (tamanhoImagem / 10);
            fim.x = x + (tamanhoImagem / 10) + (tamanhoImagem / 10) * i;
            fim.y = y + (tamanhoImagem * 0.9);
            Core.line(imagem, inicio, fim, mBlack);
        }

        for (int i = 0; i < tamanho; ++i) {
            for (int j = 0; j < tamanho; ++j) {
                Point centro = new Point();
                centro.x = x + (tamanhoImagem / 10) + i * (tamanhoImagem / 10);
                centro.y = y + (tamanhoImagem / 10) + j * (tamanhoImagem / 10);
                if (tabuleiro[i][j] == PEDRA_PRETA) {
                    Core.circle(imagem, centro, (tamanhoImagem / 20), mBlack, -1);
                }
                else if (tabuleiro[i][j] == PEDRA_BRANCA) {
                    Core.circle(imagem, centro, (tamanhoImagem / 20), mWhite, -1);
                    Core.circle(imagem, centro, (tamanhoImagem / 20), mBlack);
                }
            }
        }

    }

}
