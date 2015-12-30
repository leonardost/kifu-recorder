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

    /**
     * Retorna verdadeiro se o tabuleiroNovo repete algum dos tabuleiros anteriores da partida
     * (regra do superko).
     * @param tabuleiroNovo
     * @return
     */
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

    /**
     * Rotaciona todos os tabuleiros desta partida em sentido horário (direcao = 1) ou em sentido
     * anti-horário (direcao = -1).
     * TODO: TESTAR ESTE MÉTODO!
     * @param direcao
     */
    public void rotacionar(int direcao) {
        if (direcao != -1 && direcao != 1) return;

        List<Tabuleiro> tabuleirosRotacionados = new ArrayList<>();
        List<Jogada> jogadasRotacionadas = new ArrayList<>();

        for (Tabuleiro tabuleiro : tabuleiros) {
            tabuleirosRotacionados.add(tabuleiro.rotacionar(direcao));
            if (tabuleirosRotacionados.size() >= 2) {
                jogadasRotacionadas.add(tabuleiro.rotacionar(direcao).diferenca(
                        tabuleirosRotacionados.get(tabuleirosRotacionados.size() - 1)
                ));
            }
        }

        tabuleiros = tabuleirosRotacionados;
        jogadas = jogadasRotacionadas;
    }

}
