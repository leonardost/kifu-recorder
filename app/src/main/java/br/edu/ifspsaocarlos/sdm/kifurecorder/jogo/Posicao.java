package br.edu.ifspsaocarlos.sdm.kifurecorder.jogo;

public class Posicao {
    public int linha;
    public int coluna;

    public Posicao() {
        linha = -1;
        coluna = -1;
    }

    public Posicao(int linha, int coluna) {
        this.linha = linha;
        this.coluna = coluna;
    }

    @Override
    public String toString() {
        // TODO: Implementar
        return "aa";
    }

    @Override
    public boolean equals(Object posicao) {
        if (!(posicao instanceof Posicao)) {
            return false;
        }

        return linha == ((Posicao)posicao).linha &&
                coluna == ((Posicao)posicao).coluna;
    }

    @Override
    public int hashCode() {
        return linha * 39 + coluna;
    }
}
