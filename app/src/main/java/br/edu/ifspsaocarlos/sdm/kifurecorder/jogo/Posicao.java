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
        int l = 'a' + adicionarUmParaPularLetraI(linha);
        int c = 'a' + adicionarUmParaPularLetraI(coluna);
        return "[" + (char)l + (char)c + "]";
    }

    private char adicionarUmParaPularLetraI(int indice) {
        final int INDICE_LETRA_I = 8;
        return indice + (indice >= INDICE_LETRA_I ? 1 : 0);
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
