package br.edu.ifspsaocarlos.sdm.kifurecorder.jogo;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import br.edu.ifspsaocarlos.sdm.kifurecorder.MainActivity;

/**
 * Guarda a sequência de jogadas que foram feitas durante uma partida.
 */
public class Partida {

    List<Jogada> jogadas;
    List<Tabuleiro> tabuleiros;

    public Partida(int dimensaoDoTabuleiro) {
        tabuleiros = new ArrayList<>();
        Tabuleiro vazio = new Tabuleiro(dimensaoDoTabuleiro);
        tabuleiros.add(vazio);
    }

    public void adicionarJogadaSeForValida(Tabuleiro tabuleiro) {
        // TODO: Implementar
        int pedra = tabuleiros.size() % 2 == 0 ? Tabuleiro.PEDRA_BRANCA : Tabuleiro.PEDRA_PRETA;

        if (tabuleiro.podeVirDe(ultimoTabuleiro(), pedra)) {
            tabuleiros.add(tabuleiro);
            Log.d(MainActivity.TAG, "Adicioanndo tabuleiro " + tabuleiro + " à partida.");
        }
    }

    public Tabuleiro ultimoTabuleiro() {
        return tabuleiros.get(tabuleiros.size() - 1);
    }

    /**
     * Desconsidera a última jogada feita
     */
    public void voltarUltimaJogada() {
        if (tabuleiros.size() == 1) {
            return;
        }
        tabuleiros.remove(tabuleiros.size() - 1);
    }

    public int numeroDeJogadasFeitas() {
        return tabuleiros.size() - 1;
    }

    public String exportarParaSGF() {
        // TODO: Implementar
        return "";
    }

}
