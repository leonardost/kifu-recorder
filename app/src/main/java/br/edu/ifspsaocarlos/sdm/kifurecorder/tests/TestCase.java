package br.edu.ifspsaocarlos.sdm.kifurecorder.tests;

import br.edu.ifspsaocarlos.sdm.kifurecorder.models.Board;

/**
 * Representa a saída esperada de uma imagem de teste.
 */
public class TestCase {

    private int numeroDaImagem;
    private Board board;

    public int getNumeroDaImagem() {
        return numeroDaImagem;
    }

    public void setNumeroDaImagem(int numeroDaImagem) {
        this.numeroDaImagem = numeroDaImagem;
    }

    public Board getBoard() {
        return board;
    }

    public void criarTabuleiroComDimensao(int dimensao) {
        board = new Board(dimensao);
    }

}
