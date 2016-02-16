package br.edu.ifspsaocarlos.sdm.kifurecorder.jogo;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import br.edu.ifspsaocarlos.sdm.kifurecorder.TestesActivity;

/**
 * Guarda a sequência de jogadas que foram feitas durante uma partida.
 */
public class Partida {

    private String jogadorDePretas;
    private String jogadorDeBrancas;
    private String komi;

    private List<Jogada> jogadas;
    private List<Tabuleiro> tabuleiros;

    public Partida(int dimensaoDoTabuleiro) {
        jogadas = new ArrayList<>();
        tabuleiros = new ArrayList<>();
        Tabuleiro vazio = new Tabuleiro(dimensaoDoTabuleiro);
        tabuleiros.add(vazio);
    }

    public Partida(int dimensaoDoTabuleiro, String jogadorDePretas, String jogadorDeBrancas, String komi) {
        jogadas = new ArrayList<>();
        tabuleiros = new ArrayList<>();
        Tabuleiro vazio = new Tabuleiro(dimensaoDoTabuleiro);
        tabuleiros.add(vazio);
        this.jogadorDePretas = jogadorDePretas;
        this.jogadorDeBrancas = jogadorDeBrancas;
        this.komi = komi;
    }

    public void setJogadorDePretas(String jogadorDePretas) {
        this.jogadorDePretas = jogadorDePretas;
    }

    public String getJogadorDePretas() {
        return jogadorDePretas;
    }

    public void setJogadorDeBrancas(String jogadorDeBrancas) {
        this.jogadorDeBrancas = jogadorDeBrancas;
    }

    public String getJogadorDeBrancas() {
        return jogadorDeBrancas;
    }

    public void setKomi(String komi) {
        this.komi = komi;
    }

    public String getKomi() {
        return komi;
    }

    public boolean adicionarJogadaSeForValida(Tabuleiro tabuleiro) {
        Jogada jogadaFeita = tabuleiro.diferenca(ultimoTabuleiro());

        if (jogadaFeita == null) return false;
        // Não pode repetir nenhum estado anterior de jogo (regra do super ko)
        if (repeteEstadoAnterior(tabuleiro)) return false;
        // Tem que começar com uma pedra preta
        if (comecouComPedraBranca(jogadaFeita)) return false;
        // Não pode colocar pedra da mesma cor em seguida da outra, exceto se são pedras pretas para
        // handicap no início da partida
        if (!ehJogadaDeCorDiferenteOuSaoPedrasPretasDeHandicap(jogadaFeita)) return false;

        tabuleiros.add(tabuleiro);
        jogadas.add(jogadaFeita);
        Log.i(TestesActivity.TAG, "Adicionando tabuleiro " + tabuleiro + " (jogada " + jogadaFeita.sgf() + ") à partida.");
        return true;
    }

    private boolean comecouComPedraBranca(Jogada jogadaFeita) {
        return ultimaJogada() == null && jogadaFeita.cor == Tabuleiro.PEDRA_BRANCA;
    }

    private boolean ehJogadaDeCorDiferenteOuSaoPedrasPretasDeHandicap(Jogada jogadaFeita) {
        // É uma jogada de cor diferente da última jogada e, portanto, válida
        if (ehJogadaDeCorDiferente(jogadaFeita)) return true;
        // Se for uma pedra preta e apenas pedras pretas tiverem sido joagdas, está colocando pedras de handicap
        if (jogadaFeita.cor == Tabuleiro.PEDRA_PRETA && apenasPedrasPretasForamJogadas()) return true;
        // Senão, está repetindo a cor de uma pedra e não são pedras pretas de handicap
        return false;
    }

    private boolean ehJogadaDeCorDiferente(Jogada jogadaFeita) {
        if (ultimaJogada() == null) return true;
        return ultimaJogada().cor != jogadaFeita.cor;
    }

    /**
     * Este método é útil para verificar se as pedras de handicap estão sendo
     * colocadas.
     */
    private boolean apenasPedrasPretasForamJogadas() {
        if (tabuleiros.size() == 1) return true;

        for (int i = 1; i < tabuleiros.size(); ++i) {
            Jogada jogada = tabuleiros.get(i).diferenca(tabuleiros.get(i - 1));
            if (jogada.cor == Tabuleiro.PEDRA_BRANCA) return false;
        }

        return true;
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
        if (tabuleiros.size() == 1) return;
        tabuleiros.remove(tabuleiros.size() - 1);
        jogadas.remove(jogadas.size() - 1);
    }

    public int numeroDeJogadasFeitas() {
        return tabuleiros.size() - 1;
    }

    /**
     * Exporta uma partida em formato SGF.
     * Referência: http://www.red-bean.com/sgf/
     *
     * @return
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
        // TODO: Mudar o nome da aplicação por uma constante ou string no strings.xml

        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd");
        Calendar c = Calendar.getInstance();
        String data = sdf.format(new Date(c.getTimeInMillis()));

        sgf.append("(;");
        escreverProperiedade(sgf, "FF", "4");     // Versão do SGF
        escreverProperiedade(sgf, "GM", "1");     // Tipo de jogo (1 = Go)
        escreverProperiedade(sgf, "CA", "UTF-8");
        escreverProperiedade(sgf, "SZ", "" + ultimoTabuleiro().getDimensao());
        escreverProperiedade(sgf, "DT", data);
        escreverProperiedade(sgf, "AP", "Kifu Recorder 0.1");
        escreverProperiedade(sgf, "KM", komi);
        escreverProperiedade(sgf, "PW", jogadorDeBrancas);
        escreverProperiedade(sgf, "PB", jogadorDePretas);
    }

    private void escreverProperiedade(StringBuilder sgf, String propriedade, String valor) {
        sgf.append(propriedade);
        sgf.append("[");
        sgf.append(valor);
        sgf.append("]");
    }

    /**
     * Rotaciona todos os tabuleiros desta partida em sentido horário (direcao = 1) ou em sentido
     * anti-horário (direcao = -1).
     * @param direcao
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
     * Retorna a última jogada que foi feita na partida.
     */
    public Jogada ultimaJogada() {
        if (tabuleiros.size() == 1) return null;
        return ultimoTabuleiro().diferenca(penultimoTabuleiro());
    }

    private Tabuleiro penultimoTabuleiro() {
        if (tabuleiros.size() == 1) {
            return null;
        }
        return tabuleiros.get(tabuleiros.size() - 2);
    }

}
