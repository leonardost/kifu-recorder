package br.edu.ifspsaocarlos.sdm.kifurecorder.processing;

public class MoveHypothesis {
    public int linha;
    public int coluna;
    public int cor;
    public double confianca;
    
    public MoveHypothesis(int cor, double confianca) {
        this.cor = cor;
        this.confianca = confianca;
    }
}
