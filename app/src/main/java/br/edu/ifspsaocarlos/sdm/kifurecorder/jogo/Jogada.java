package br.edu.ifspsaocarlos.sdm.kifurecorder.jogo;

public class Jogada {

    public boolean ehPasse;
    public int linha;
    public int coluna;
    public int cor;

    public Jogada() {}

    public Jogada(int linha, int coluna, int cor) {
        this.ehPasse = false;
        this.linha = linha;
        this.coluna = coluna;
        this.cor = cor;
    }

    public Jogada(int cor) {
        this.ehPasse = true;
        this.cor = cor;
    }

    /**
     * Retorna a posição em que uma jogada fio feita.
     * @return
     */
    public Posicao posicao() {
        return new Posicao(linha, coluna);
    }

    // TODO: Testar
    public String sgf() {
        // TODO: Verificar se é um passe ou não
        // Se a linha ou coluna forem maiores que 8 (coluna i), adiciona um porque não se usa o i.
        int l = 'a' + linha + (linha >= 8 ? 1 : 0);
        int c = 'a' + coluna + (coluna >= 8 ? 1 : 0);
        return "[" + (char)l + (char)c + "]";
    }
}
