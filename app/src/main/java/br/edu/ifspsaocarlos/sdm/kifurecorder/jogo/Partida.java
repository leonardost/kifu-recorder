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
        jogadas = new ArrayList<>();
        tabuleiros = new ArrayList<>();
        Tabuleiro vazio = new Tabuleiro(dimensaoDoTabuleiro);
        tabuleiros.add(vazio);
    }

    public void adicionarJogadaSeForValida(Tabuleiro tabuleiro) {
        Jogada jogadaFeita = tabuleiro.diferenca(ultimoTabuleiro());

        if (jogadaFeita == null) return;
        if (repeteEstadoAnterior(tabuleiro)) return;

        tabuleiros.add(tabuleiro);
        jogadas.add(jogadaFeita);
        Log.i(MainActivity.TAG, "Adicionando tabuleiro " + tabuleiro + " (jogada " + jogadaFeita.sgf() + ") à partida.");
    }

    public Tabuleiro ultimoTabuleiro() {
        return tabuleiros.get(tabuleiros.size() - 1);
    }

    private boolean repeteEstadoAnterior(Tabuleiro tabuleiroNovo) {
        for (Tabuleiro tabuleiro : tabuleiros) {
            if (tabuleiro.equals(tabuleiroNovo)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Desconsidera a última jogada feita
     */
    public void voltarUltimaJogada() {
        if (tabuleiros.size() == 1) {
            return;
        }
        tabuleiros.remove(tabuleiros.size() - 1);
        jogadas.remove(jogadas.size() - 1);
    }

    public int numeroDeJogadasFeitas() {
        return tabuleiros.size() - 1;
    }

    public String sgf() {
        StringBuilder sgf = new StringBuilder();
        // TODO: Escrever cabeçalho
        for (Jogada jogada : jogadas) {
            sgf.append(jogada.sgf());
        }
        // TODO: Escrever finalização (se hovuer)
        return sgf.toString();
    }

}
