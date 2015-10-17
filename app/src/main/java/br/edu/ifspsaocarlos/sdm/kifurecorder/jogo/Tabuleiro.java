package br.edu.ifspsaocarlos.sdm.kifurecorder.jogo;

/**
 * Representa uma configuração específica de tabuleiro
 */
public class Tabuleiro {

    public final static int VAZIO = 0;
    public final static int PEDRA_PRETA = 1;
    public final static int PEDRA_BRANCA = 2;

    private int dimensao;
    private Integer[][] tabuleiro = new Integer[9][9];

    public Tabuleiro() {}

    public Tabuleiro(int dimensao) {
        this.dimensao = dimensao;
        this.tabuleiro = new Integer[dimensao][dimensao];
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
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

    public int getPosicao(int linha, int coluna) {
        return tabuleiro[linha][coluna];
    }

    public int getDimensao() {
        return dimensao;
    }

}
