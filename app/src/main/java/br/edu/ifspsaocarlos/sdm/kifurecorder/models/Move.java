package br.edu.ifspsaocarlos.sdm.kifurecorder.models;

import java.io.Serializable;

/**
 * Represents a move made in a game.
 */
public class Move implements Serializable {

    public boolean isPass;
    public int row;
    public int column;
    public int color;

    public Move(int row, int column, int color) {
        this.isPass = false;
        this.row = row;
        this.column = column;
        this.color = color;
    }

    public Position posicao() {
        return new Position(row, column);
    }

    public String sgf() {
        int l = 'a' + row;
        int c = 'a' + column;
        String coordenada = "" + (char)c + (char)l;
        if (isPass) coordenada = "";
        char cor = this.color == Board.BLACK_STONE ? 'B' : 'W';
        return ";" + cor + "[" + coordenada + "]";
    }

    public String toString() {
        String nomeCor = color == Board.EMPTY ? "Vazio" : color == Board.BLACK_STONE ? "Preto" : "Branco";
        return "(" + row + ", " + column + ", " + nomeCor + ")";
    }

}
