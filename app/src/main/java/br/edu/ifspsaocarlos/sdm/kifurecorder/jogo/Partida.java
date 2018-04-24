package br.edu.ifspsaocarlos.sdm.kifurecorder.jogo;

import android.util.Log;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import br.edu.ifspsaocarlos.sdm.kifurecorder.BuildConfig;
import br.edu.ifspsaocarlos.sdm.kifurecorder.TestesActivity;

/**
 * Representa uma partida completa, com a sequência de tabuleiros e jogadas que foram feitos.
 */
public class Partida implements Serializable {

    private String jogadorDePretas;
    private String jogadorDeBrancas;
    private String komi;
    private int dimensaoDoTabuleiro;
    private List<Jogada> jogadas;
    private List<Tabuleiro> tabuleiros;

    // Atributo para medir a precisão do sistema
    private int numeroDeVezesQueVoltou;
    private int numeroDeVezesQueTeveQueAdicionarManualmente;

    public Partida(int dimensaoDoTabuleiro, String jogadorDePretas, String jogadorDeBrancas, String komi) {
        this.dimensaoDoTabuleiro = dimensaoDoTabuleiro;
        this.jogadorDePretas = jogadorDePretas;
        this.jogadorDeBrancas = jogadorDeBrancas;
        this.komi = komi;
        jogadas = new ArrayList<>();
        tabuleiros = new ArrayList<>();

        Tabuleiro tabuleiroVazio = new Tabuleiro(dimensaoDoTabuleiro);
        tabuleiros.add(tabuleiroVazio);
        numeroDeVezesQueVoltou = 0;
        numeroDeVezesQueTeveQueAdicionarManualmente = 0;
    }

    public int getDimensaoDoTabuleiro() {
        return dimensaoDoTabuleiro;
    }

    public String getJogadorDePretas() {
        return jogadorDePretas;
    }

    public String getJogadorDeBrancas() {
        return jogadorDeBrancas;
    }

    public boolean adicionarJogadaSeForValida(Tabuleiro tabuleiro) {
        Jogada jogadaFeita = tabuleiro.diferenca(ultimoTabuleiro());

        if (jogadaFeita == null || repeteEstadoAnterior(tabuleiro) || !proximaJogadaPodeSer(jogadaFeita.cor)) {
            return false;
        }

        tabuleiros.add(tabuleiro);
        jogadas.add(jogadaFeita);
        Log.i(TestesActivity.TAG, "Adicionando tabuleiro " + tabuleiro + " (jogada " + jogadaFeita.sgf() + ") à partida.");
        return true;
    }

    /**
     * Retorna verdadeiro se o tabuleiroNovo repete algum dos tabuleiros anteriores da partida
     * (regra do superko).
     */
    private boolean repeteEstadoAnterior(Tabuleiro tabuleiroNovo) {
        for (Tabuleiro tabuleiro : tabuleiros) {
            if (tabuleiro.equals(tabuleiroNovo)) return true;
        }
        return false;
    }

    public boolean proximaJogadaPodeSer(int cor) {
        if (cor == Tabuleiro.PEDRA_PRETA)
            return ehPrimeiraJogada() || apenasPedrasPretasForamJogadas() || ultimaJogadaFoiBranca();
        else if (cor == Tabuleiro.PEDRA_BRANCA)
            return ultimaJogadaFoiPreta();
        return false;
    }

    private boolean ehPrimeiraJogada() {
        return jogadas.isEmpty();
    }

    /**
     * Este método é usado para verificar se as pedras de handicap estão sendo colocadas.
     */
    private boolean apenasPedrasPretasForamJogadas() {
        for (Jogada jogada : jogadas) {
            if (jogada.cor == Tabuleiro.PEDRA_BRANCA) return false;
        }
        return true;
    }

    private boolean ultimaJogadaFoiBranca() {
        return !ehPrimeiraJogada() && ultimaJogada().cor == Tabuleiro.PEDRA_BRANCA;
    }

    private boolean ultimaJogadaFoiPreta() {
        return !ehPrimeiraJogada() && ultimaJogada().cor == Tabuleiro.PEDRA_PRETA;
    }

    public Jogada ultimaJogada() {
        if (jogadas.isEmpty()) return null;
        return jogadas.get(jogadas.size() - 1);
    }

    public Tabuleiro ultimoTabuleiro() {
        return tabuleiros.get(tabuleiros.size() - 1);
    }

    /**
     * Desconsidera a última jogada feita e a retorna
     */
    public Jogada voltarUltimaJogada() {
        if (jogadas.isEmpty()) return null;
        tabuleiros.remove(tabuleiros.size() - 1);
        Jogada ultimaJogada = jogadas.remove(jogadas.size() - 1);
        numeroDeVezesQueVoltou++;
        return ultimaJogada;
    }

    public int numeroDeJogadasFeitas() {
        return jogadas.size();
    }

    public void adicionouJogadaManualmente() {
        numeroDeVezesQueTeveQueAdicionarManualmente++;
    }

    /**
     * Rotaciona todos os tabuleiros desta partida em sentido horário (direcao = 1) ou em sentido
     * anti-horário (direcao = -1).
     */
    public void rotacionar(int direcao) {
        if (direcao != -1 && direcao != 1) return;

        List<Tabuleiro> tabuleirosRotacionados = new ArrayList<>();
        for (Tabuleiro tabuleiro : tabuleiros) {
            tabuleirosRotacionados.add(tabuleiro.rotacionar(direcao));
        }

        List<Jogada> jogadasRotacionadas = new ArrayList<>();
        for (int i = 1; i < tabuleirosRotacionados.size(); ++i) {
            Tabuleiro ultimo = tabuleirosRotacionados.get(i);
            Tabuleiro penultimo = tabuleirosRotacionados.get(i - 1);
            jogadasRotacionadas.add(ultimo.diferenca(penultimo));
        }

        tabuleiros = tabuleirosRotacionados;
        jogadas = jogadasRotacionadas;
    }

    /**
     * Exporta a partida em formato SGF.
     * Referência: http://www.red-bean.com/sgf/
     */
    public String sgf() {
        StringBuilder sgf = new StringBuilder();
        escreverCabecalho(sgf);
        for (Jogada jogada : jogadas) {
            Log.i(TestesActivity.TAG, "construindo SGF - jogada " + jogada.sgf());
            sgf.append(jogada.sgf());
        }
        sgf.append(")");
        return sgf.toString();
    }

    private void escreverCabecalho(StringBuilder sgf) {
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        String data = sdf.format(new Date(c.getTimeInMillis()));

        sgf.append("(;");
        escreverProperiedade(sgf, "FF", "4");     // Versão do SGF
        escreverProperiedade(sgf, "GM", "1");     // Tipo de jogo (1 = Go)
        escreverProperiedade(sgf, "CA", "UTF-8");
        escreverProperiedade(sgf, "SZ", "" + ultimoTabuleiro().getDimensao());
        escreverProperiedade(sgf, "DT", data);
        escreverProperiedade(sgf, "AP", "Kifu Recorder v" + BuildConfig.VERSION_NAME);
        escreverProperiedade(sgf, "KM", komi);
        escreverProperiedade(sgf, "PW", jogadorDeBrancas);
        escreverProperiedade(sgf, "PB", jogadorDePretas);
        escreverProperiedade(sgf, "Z1", "" + numeroDeJogadasFeitas());
        escreverProperiedade(sgf, "Z2", "" + numeroDeVezesQueVoltou);
        escreverProperiedade(sgf, "Z3", "" + numeroDeVezesQueTeveQueAdicionarManualmente);
    }

    private void escreverProperiedade(StringBuilder sgf, String propriedade, String valor) {
        sgf.append(propriedade);
        sgf.append("[");
        sgf.append(valor);
        sgf.append("]");
    }

}
