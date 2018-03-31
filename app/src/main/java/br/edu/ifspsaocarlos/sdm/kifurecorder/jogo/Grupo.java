package br.edu.ifspsaocarlos.sdm.kifurecorder.jogo;

import java.util.HashSet;
import java.util.Set;

/**
 * Representa um grupo em um tabuleiro.
 */
public class Grupo {

    private int cor;
    private Set<Posicao> posicoes;
    private Set<Posicao> liberdades;

    public Grupo(int cor) {
        this.cor = cor;
        posicoes = new HashSet<>();
        liberdades = new HashSet<>();
    }

    public int getCor() {
        return cor;
    }

    public Set<Posicao> getPosicoes() {
        return posicoes;
    }

    public void adicionarPosicao(Posicao posicao) {
        posicoes.add(posicao);
    }

    public void adicionarLiberdade(Posicao liberdade) {
        liberdades.add(liberdade);
    }

    public boolean estaEmAtari() {
        return liberdades.size() == 1;
    }

    public boolean ehCapturadoPela(Jogada jogada) {
        return jogada.cor != cor && estaEmAtari() && liberdades.contains(jogada.posicao());
    }

    public boolean naoTemLiberdades() {
        return liberdades.size() == 0;
    }

    @Override
    public boolean equals(Object outro) {
        if (!(outro instanceof Grupo)) {
            return false;
        }

        Grupo outroGrupo = (Grupo) outro;
        return cor == outroGrupo.cor &&
                posicoes.equals(outroGrupo.posicoes) &&
                liberdades.equals(outroGrupo.liberdades);
    }

    @Override
    public int hashCode() {
        return cor + posicoes.hashCode() + liberdades.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder string = new StringBuilder();
        string.append("Grupo de cor " + cor + ": ");
        for (Posicao posicao : posicoes) {
            string.append(posicao);
        }
        return string.toString();
    }

}
