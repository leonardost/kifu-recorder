package br.edu.ifspsaocarlos.sdm.kifurecorder.testes;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Tabuleiro;

/**
 * Representa a saída esperada de uma imagem de teste.
 */
public class CasoDeTeste {

    private int numeroDaImagem;
    private Tabuleiro tabuleiro;

    public int getNumeroDaImagem() {
        return numeroDaImagem;
    }

    public void setNumeroDaImagem(int numeroDaImagem) {
        this.numeroDaImagem = numeroDaImagem;
    }

    public Tabuleiro getTabuleiro() {
        return tabuleiro;
    }

    public void criarTabuleiroComDimensao(int dimensao) {
        tabuleiro = new Tabuleiro(dimensao);
    }

}
