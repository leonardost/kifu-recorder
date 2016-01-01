package br.edu.ifspsaocarlos.sdm.kifurecorder.jogo;

public class Posicao {
    public int linha;
    public int coluna;

    public Posicao(int linha, int coluna) {
        this.linha = linha;
        this.coluna = coluna;
    }

    @Override
    public String toString() {
        int l = 'a' + linha + (linha >= 8 ? 1 : 0);
        int c = 'a' + coluna + (coluna >= 8 ? 1 : 0);
        return "[" + (char)l + (char)c + "]";
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
