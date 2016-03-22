package br.edu.ifspsaocarlos.sdm.kifurecorder.processamento;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Jogada;

public class HipoteseDeJogada {
    public int linha;
    public int coluna;
    public int cor;
    public double confianca;
    
    public HipoteseDeJogada(int cor, double confianca) {
        this.cor = cor;
        this.confianca = confianca;
    }
}
