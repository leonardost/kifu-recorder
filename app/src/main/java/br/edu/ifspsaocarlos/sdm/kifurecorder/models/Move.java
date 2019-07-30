package br.edu.ifspsaocarlos.sdm.kifurecorder.models;

import java.io.Serializable;

/**
 * Representa uma jogada feita em um jogo.
 */
public class Move implements Serializable {

    public boolean ehPasse;
    public int linha;
    public int coluna;
    public int cor;

    public Move(int linha, int coluna, int cor) {
        this.ehPasse = false;
        this.linha = linha;
        this.coluna = coluna;
        this.cor = cor;
    }

    public Position posicao() {
        return new Position(linha, coluna);
    }

    public String sgf() {
        int l = 'a' + linha;
        int c = 'a' + coluna;
        String coordenada = "" + (char)c + (char)l;
        if (ehPasse) coordenada = "";
        char cor = this.cor == Board.BLACK_STONE ? 'B' : 'W';
        return ";" + cor + "[" + coordenada + "]";
    }

    public String toString() {
        String nomeCor = cor == Board.EMPTY ? "Vazio" : cor == Board.BLACK_STONE ? "Preto" : "Branco";
        return "(" + linha + ", " + coluna + ", " + nomeCor + ")";
    }

}
