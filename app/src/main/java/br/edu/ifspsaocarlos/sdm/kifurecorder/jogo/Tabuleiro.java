package br.edu.ifspsaocarlos.sdm.kifurecorder.jogo;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import br.edu.ifspsaocarlos.sdm.kifurecorder.MainActivity;

/**
 * Representa um estado de tabuleiro
 */
public class Tabuleiro {

    public final static int VAZIO = 0;
    public final static int PEDRA_PRETA = 1;
    public final static int PEDRA_BRANCA = 2;

    private int dimensao;
    private Integer[][] tabuleiro;

    public Tabuleiro(int dimensao) {
        this.dimensao = dimensao;
        this.tabuleiro = new Integer[dimensao][dimensao];
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                tabuleiro[i][j] = VAZIO;
            }
        }
    }

    public int getDimensao() {
        return dimensao;
    }

    public void colocarPedra(int linha, int coluna, int pedra) {
        if (pedra != PEDRA_PRETA && pedra != PEDRA_BRANCA) {
            throw new RuntimeException("Pedra inválida!");
        }
        if (linha < 0 || coluna < 0 || linha >= dimensao || coluna >= dimensao) {
            throw new RuntimeException("Posição inválida!");
        }
        tabuleiro[linha][coluna] = pedra;
    }

    public int getPosicao(int linha, int coluna) {
        return tabuleiro[linha][coluna];
    }

    public String toString() {
        StringBuilder saida = new StringBuilder();

        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                if (tabuleiro[i][j] == VAZIO) {
                    saida.append('.');
                }
                else if (tabuleiro[i][j] == PEDRA_PRETA) {
                    saida.append('P');
                }
                else if (tabuleiro[i][j] == PEDRA_BRANCA) {
                    saida.append('B');
                }
            }
            saida.append(System.getProperty("line.separator"));
        }

        return saida.toString();
    }

    /**
     * Retorna um novo tabuleiro que correponde ao tabuleiro atual rotacionado em sentido horário
     * (direcao = 1) ou anti-horário (direcao = -1).
     * @param direcao
     * @return Novo tabuleiro rotacionado.
     */
    public Tabuleiro rotacionar(int direcao) {
        if (direcao == -1) {
            return rotacionarEmSentidoAntihorario();
        }
        else if (direcao == 1) {
            return rotacionarEmSentidoHorario();
        }
        throw new RuntimeException("Direção de rotação inválida!");
    }

    private Tabuleiro rotacionarEmSentidoHorario() {
        Tabuleiro tabuleiroRotacionado = new Tabuleiro(dimensao);
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                if (tabuleiro[dimensao - 1 - j][i] != Tabuleiro.VAZIO) {
                    tabuleiroRotacionado.colocarPedra(i, j, tabuleiro[dimensao - 1 - j][i]);
                }
            }
        }
        return tabuleiroRotacionado;
    }

    private Tabuleiro rotacionarEmSentidoAntihorario() {
        Tabuleiro tabuleiroRotacionado = new Tabuleiro(dimensao);
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                if (tabuleiro[j][dimensao - 1 - i] != Tabuleiro.VAZIO) {
                    tabuleiroRotacionado.colocarPedra(i, j, tabuleiro[j][dimensao - 1 - i]);
                }
            }
        }
        return tabuleiroRotacionado;
    }

    /**
     * Verifica se dois tabuleiros são exatamente iguais.
     *
     * @return Verdadeiro se os dois tabuleiros são exatamente iguais, falso
     *         caso contrário
     */
    public boolean identico(Tabuleiro outroTabuleiro) {
        if (dimensao != outroTabuleiro.dimensao) return false;

        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                if (getPosicao(i, j) != outroTabuleiro.getPosicao(i, j)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Verifica se dois tabuleiros são iguais, incluindo se um tabuleiro é uma
     * rotação do outro.
     *
     * @return Verdadeiro se os dois tabuleiros são iguais, falso caso
     *         contrário
     */
    @Override
    public boolean equals(Object objeto) {
        if (!(objeto instanceof Tabuleiro)) {
            return false;
        }
        Tabuleiro outroTabuleiro = (Tabuleiro)objeto;

        if (dimensao != outroTabuleiro.dimensao) {
            return false;
        }

        Tabuleiro rotacao1 = outroTabuleiro.rotacionarEmSentidoHorario();
        Tabuleiro rotacao2 = rotacao1.rotacionarEmSentidoHorario();
        Tabuleiro rotacao3 = rotacao2.rotacionarEmSentidoHorario();

//        Log.d(MainActivity.TAG, "Comparando tabuleiro " + this + " com tabuleiros " + outroTabuleiro + ", " + rotacao1 + ", " + rotacao2 + " e " + rotacao3);

        if (this.identico(outroTabuleiro) || this.identico(rotacao1)
                || this.identico(rotacao2) || this.identico(rotacao3)) {
            return true;
        }

        return false;
    }

    /**
     * Retorna a jogada de diferença deste tabuleiro para o tabuleiro anteiror. Se houver mais de
     * uma jogada de diferença, retorna nulo.
     * TODO: TESTAR!
     *
     * @param tabuleiroAnterior
     * @return
     */
    public Jogada diferenca(Tabuleiro tabuleiroAnterior) {

        int numeroDePedrasPretasAntes = tabuleiroAnterior.numeroDePedras(Tabuleiro.PEDRA_PRETA);
        int numeroDePedrasBrancasAntes = tabuleiroAnterior.numeroDePedras(Tabuleiro.PEDRA_BRANCA);
        int numeroDePedrasPretasDepois = numeroDePedras(Tabuleiro.PEDRA_PRETA);
        int numeroDePedrasBrancasDepois = numeroDePedras(Tabuleiro.PEDRA_BRANCA);
        int diferencaDePedrasPretas = numeroDePedrasPretasDepois - numeroDePedrasPretasAntes;
        int diferencaDePedrasBrancas = numeroDePedrasBrancasDepois - numeroDePedrasBrancasAntes;

        // Ha mais de uma jogada de diferença
        if (diferencaDePedrasPretas >= 1 && diferencaDePedrasBrancas >= 1) {
            return null;
        }

        // Verifica se todos os grupos do tabuleiro tem pelo menos uma liberdade
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                Grupo grupo = grupoEm(i, j);
                if (grupo != null && grupo.getLiberdades().size() == 0) {
                    return null;
                }
            }
        }

        // Jogada das pretas
        if (diferencaDePedrasPretas == 1) {
            Jogada jogadaFeita = pedraDiferente(tabuleiroAnterior, Tabuleiro.PEDRA_PRETA);

            // Não houve captura
            if (diferencaDePedrasBrancas == 0) {
                if (!estaTudoNoMesmoLugar(tabuleiroAnterior)) {
                    return null;
                }
            }
            // Houve captura
            else if (diferencaDePedrasBrancas < 0) {
                if (!capturaFoiValida(jogadaFeita, tabuleiroAnterior)) {
                    return null;
                }
            }
            return jogadaFeita;

        }
        // Jogada das brancas
        else if (diferencaDePedrasBrancas == 1) {
            Jogada jogadaFeita = pedraDiferente(tabuleiroAnterior, Tabuleiro.PEDRA_BRANCA);

            // Não houve captura
            if (diferencaDePedrasPretas == 0) {
                if (!estaTudoNoMesmoLugar(tabuleiroAnterior)) {
                    return null;
                }
            }
            // Houve captura
            else if (diferencaDePedrasPretas < 0) {
                if (!capturaFoiValida(jogadaFeita, tabuleiroAnterior)) {
                    return null;
                }
            }
            return jogadaFeita;

        }

        // Se nao entrou em nenhuma condiçao, ha mais de uma jogada de diferença
        return null;
    }

    /**
     * Retorna a quantidade de determinada pedra (preta ou branca) neste tabuleiro.
     */
    private int numeroDePedras(int pedra) {
        int numeroDePedras = 0;
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                if (tabuleiro[i][j] == pedra) {
                    ++numeroDePedras;
                }
            }
        }
        return numeroDePedras;
    }

    /**
     * Veifica se todas as pedras do tabuleiro antigo estão no novo.
     * @param anterior
     * @return
     */
    private boolean estaTudoNoMesmoLugar(Tabuleiro anterior) {
        for (int i = 0; i < anterior.getDimensao(); ++i) {
            for (int j = 0; j < anterior.getDimensao(); ++j) {
                int posicaoAntiga = anterior.getPosicao(i, j);
                if (posicaoAntiga != Tabuleiro.VAZIO && posicaoAntiga != tabuleiro[i][j]) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Retorna a primeira pedra diferente da cor especificada encontrada entre os dois tabuleiros.
     * @param anterior
     * @return
     */
    private Jogada pedraDiferente(Tabuleiro anterior, int cor) {
        for (int i = 0; i < anterior.getDimensao(); ++i) {
            for (int j = 0; j < anterior.getDimensao(); ++j) {
                if (tabuleiro[i][j] == cor &&
                        anterior.getPosicao(i, j) != tabuleiro[i][j]) {
                    return new Jogada(i, j, cor);
                }
            }
        }
        return null;
    }

    /**
     * Verifica se a jogada que foi feita sobre o tabuleiro anterior condiz com o tabuleiro atual.
     * @param jogadaFeita
     * @param anterior
     * @return
     */
    private boolean capturaFoiValida(Jogada jogadaFeita, Tabuleiro anterior) {
        Set<Grupo> devemSerCapturados = new HashSet<>();
        Set<Posicao> posicoes = new HashSet<>();
        posicoes.add(new Posicao(jogadaFeita.linha - 1, jogadaFeita.coluna));
        posicoes.add(new Posicao(jogadaFeita.linha + 1, jogadaFeita.coluna));
        posicoes.add(new Posicao(jogadaFeita.linha, jogadaFeita.coluna - 1));
        posicoes.add(new Posicao(jogadaFeita.linha, jogadaFeita.coluna + 1));

        // Recupera os grupos do adversário que estavam em atari (com apenas uma liberdade) ao redor
        // da jogada que foi feita
        for (Posicao posicao : posicoes) {
            Grupo grupo = anterior.grupoEm(posicao);
            if (grupo == null || grupo.getCor() == jogadaFeita.cor) continue;
            Set<Posicao> liberdades = grupo.getLiberdades();
            if (liberdades.size() == 1 && liberdades.contains(jogadaFeita.posicao())) {
                devemSerCapturados.add(grupo);
                Log.i(MainActivity.TAG, "Grupo " + grupo + " será capturado.");
            }
        }

        // Verifica se esses grupos foram efetivamente capturados
        for (Grupo grupo : devemSerCapturados) {
            for (Posicao posicao : grupo.getPosicoes()) {
                // Se alguma das posições ocupadas anteriormente pelo grupo não estiver vazia, a
                // captura não foi feita corretamente
                if (tabuleiro[posicao.linha][posicao.coluna] != Tabuleiro.VAZIO) {
                    Log.i(MainActivity.TAG, "Captura não foi feita corretamente na posição " + posicao);
                    return false;
                }
            }
        }

        Set<Posicao> posicoesQueDevemEstarVazias = new HashSet<>();
        for (Grupo grupo : devemSerCapturados) {
            posicoesQueDevemEstarVazias.addAll(grupo.getPosicoes());
        }
        Log.i(MainActivity.TAG, "Posições que devem estar vazias:");
        for (Posicao posicao : posicoesQueDevemEstarVazias) {
            Log.i(MainActivity.TAG, "" + posicao);
        }

        // Verifica se todas as demais pedras, com exceção das que foram capturadas e da jogada que
        // foi feita, estão nos mesmos lugares
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                // Estas posições já foram verificadas
                if (posicoesQueDevemEstarVazias.contains(new Posicao(i, j))) continue;

                if (i == jogadaFeita.linha && j == jogadaFeita.coluna) {
                    if (tabuleiro[i][j] != jogadaFeita.cor) {
                        Log.i(MainActivity.TAG, "Jogada feita não está no tabuleiro novo");
                        return false;
                    }
                    continue;
                }

                if (tabuleiro[i][j] != anterior.getPosicao(i, j)) {
                    Log.i(MainActivity.TAG, "Problema na posição (" + i + ", " + j + ")");
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Retorna o grupo que está em determinada posição do tabuleiro ou nulo caso não haja nenhum
     * grupo.
     * TODO: TESTAR!
     * @param linha
     * @param coluna
     * @return
     */
    public Grupo grupoEm(int linha, int coluna) {
        if (linha < 0 || coluna < 0 || linha >= dimensao || coluna >= dimensao) {
            return null;
        }
        if (tabuleiro[linha][coluna] == Tabuleiro.VAZIO) {
            return null;
        }

        boolean[][] posicoesVisitadas = new boolean[dimensao][dimensao];
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                posicoesVisitadas[i][j] = false;
            }
        }

        int cor = tabuleiro[linha][coluna];
        Grupo grupo = new Grupo(cor);
        delimitarGrupo(linha, coluna, posicoesVisitadas, grupo);
        return grupo;
    }

    public Grupo grupoEm(Posicao posicao) {
        return grupoEm(posicao.linha, posicao.coluna);
    }

    private void delimitarGrupo(int linha, int coluna, boolean[][] posicoesVisitadas, Grupo grupo) {
        if (linha < 0 || coluna < 0 || linha >= dimensao || coluna >= dimensao) return;
        if (posicoesVisitadas[linha][coluna]) return;

        posicoesVisitadas[linha][coluna] = true;

        // Encontrou uma das liberdades do grupo
        if (tabuleiro[linha][coluna] == Tabuleiro.VAZIO) {
            grupo.adicionarLiberdade(new Posicao(linha, coluna));
        }
        // Encontrou uma extensão do grupo
        else if (tabuleiro[linha][coluna] == grupo.getCor()) {
            grupo.adicionarPosicao(new Posicao(linha, coluna));

            delimitarGrupo(linha - 1, coluna, posicoesVisitadas, grupo);
            delimitarGrupo(linha + 1, coluna, posicoesVisitadas, grupo);
            delimitarGrupo(linha, coluna - 1, posicoesVisitadas, grupo);
            delimitarGrupo(linha, coluna + 1, posicoesVisitadas, grupo);
        }
        // Se não entrou em nenhuma das condições, encontrou uma pedra de cor diferente da do grupo
    }

}
