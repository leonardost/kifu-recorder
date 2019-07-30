package br.edu.ifspsaocarlos.sdm.kifurecorder.testes;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Board;

/**
 * Representa a saída esperada de uma imagem de teste.
 */
public class CasoDeTeste {

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
