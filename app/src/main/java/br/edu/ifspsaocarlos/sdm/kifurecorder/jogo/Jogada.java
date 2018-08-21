package br.edu.ifspsaocarlos.sdm.kifurecorder.jogo;

import java.io.Serializable;

/**
 * Representa uma jogada feita em um jogo.
 */
public class Jogada implements Serializable {

    public boolean ehPasse;
    public int linha;
    public int coluna;
    public int cor;

    public Jogada(int linha, int coluna, int cor) {
        this.ehPasse = false;
        this.linha = linha;
        this.coluna = coluna;
        this.cor = cor;
    }

    public Posicao posicao() {
        return new Posicao(linha, coluna);
    }

    public String sgf() {
        int l = 'a' + linha;
        int c = 'a' + coluna;
        String coordenada = "" + (char)c + (char)l;
        if (ehPasse) coordenada = "";
        char cor = this.cor == Tabuleiro.PEDRA_PRETA ? 'B' : 'W';
        return ";" + cor + "[" + coordenada + "]";
    }

    public String toString() {
        String nomeCor = cor == Tabuleiro.VAZIO ? "Vazio" : cor == Tabuleiro.PEDRA_PRETA ? "Preto" : "Branco";
        return "(" + linha + ", " + coluna + ", " + nomeCor + ")";
    }

}
