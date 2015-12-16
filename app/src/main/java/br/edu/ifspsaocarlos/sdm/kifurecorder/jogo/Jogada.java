package br.edu.ifspsaocarlos.sdm.kifurecorder.jogo;

public class Jogada {

    public int linha;
    public int coluna;
    public int pedra;

    public Jogada() {}

    public Jogada(int linha, int coluna, int pedra) {
        this.linha = linha;
        this.coluna = coluna;
        this.pedra = pedra;
    }

}
