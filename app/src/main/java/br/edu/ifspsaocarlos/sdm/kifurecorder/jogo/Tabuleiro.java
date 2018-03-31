package br.edu.ifspsaocarlos.sdm.kifurecorder.jogo;

import android.util.Log;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import br.edu.ifspsaocarlos.sdm.kifurecorder.TestesActivity;

/**
 * Representa um estado de tabuleiro
 */
public class Tabuleiro implements Serializable {

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

    /**
     * Constutor de cópia
     * @param tabuleiro
     */
    public Tabuleiro(Tabuleiro tabuleiro) {
        dimensao = tabuleiro.dimensao;
        this.tabuleiro = new Integer[dimensao][dimensao];
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                this.tabuleiro[i][j] = tabuleiro.tabuleiro[i][j];
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
        if (tabuleiro[linha][coluna] != VAZIO) {
			throw new RuntimeException("Já existe uma pedra nessa posição!");
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
     * @return boolean
     */
    @Override
    public boolean equals(Object objeto) {
        if (!(objeto instanceof Tabuleiro)) return false;
        Tabuleiro outroTabuleiro = (Tabuleiro)objeto;
        if (dimensao != outroTabuleiro.dimensao) return false;

        Tabuleiro rotacao1 = outroTabuleiro.rotacionarEmSentidoHorario();
        Tabuleiro rotacao2 = rotacao1.rotacionarEmSentidoHorario();
        Tabuleiro rotacao3 = rotacao2.rotacionarEmSentidoHorario();

        return this.identico(outroTabuleiro) || this.identico(rotacao1) || this.identico(rotacao2) || this.identico(rotacao3);
    }

    /**
     * Retorna a jogada de diferença deste tabuleiro para outro. Se houver mais de
     * uma jogada de diferença, retorna nulo.
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
        if (diferencaDePedrasPretas >= 1 && diferencaDePedrasBrancas >= 1) return null;

        if (!todosOsGruposDoTabuleiroTemLiberdade()) return null;

        int corDaJogada = Tabuleiro.VAZIO;
        boolean houveCaptura = false;
        if (diferencaDePedrasPretas == 1) {
            corDaJogada = Tabuleiro.PEDRA_PRETA;
            if (diferencaDePedrasBrancas < 0) houveCaptura = true;
        } else if (diferencaDePedrasBrancas == 1) {
            corDaJogada = Tabuleiro.PEDRA_BRANCA;
            if (diferencaDePedrasPretas < 0) houveCaptura = true;
        } else return null;

        Jogada jogadaFeita = pedraDiferente(tabuleiroAnterior, corDaJogada);
        if (!houveCaptura && !todasAsPedrasEstaoNoMesmoLugar(tabuleiroAnterior)) return null;
        if (houveCaptura && !capturaFoiValida(jogadaFeita, tabuleiroAnterior)) return null;
        return jogadaFeita;
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

    private boolean todosOsGruposDoTabuleiroTemLiberdade() {
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                Grupo grupo = grupoEm(i, j);
                if (grupo != null && grupo.naoTemLiberdades()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Veifica se todas as pedras do tabuleiro antigo estão no novo.
     * @param anterior
     * @return
     */
    private boolean todasAsPedrasEstaoNoMesmoLugar(Tabuleiro anterior) {
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
                if (tabuleiro[i][j] == cor && anterior.getPosicao(i, j) != tabuleiro[i][j]) {
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
        Set<Grupo> gruposQueDevemSerCapturados = recuperarGruposQueDevemSerCapturadosApos(jogadaFeita, anterior);

        if (!gruposForamRealmenteCapturados(gruposQueDevemSerCapturados)) return false;

        Set<Posicao> posicoesJaVerificadas = new HashSet<>();
        for (Grupo grupo : gruposQueDevemSerCapturados) {
            posicoesJaVerificadas.addAll(grupo.getPosicoes());
        }
        posicoesJaVerificadas.add(jogadaFeita.posicao());

        return demaisPosicoesEstaoIguais(posicoesJaVerificadas);
    }

    private Set<Grupo> recuperarGruposQueDevemSerCapturadosApos(Jogada jogada, Tabuleiro tabuleiro) {
        Set<Grupo> gruposQueDevemSerCapturados = new HashSet<>();
        for (Grupo grupo : recuperaGruposAdjacentesA(jogada)) {
            if (grupo == null) continue;
            if (grupo.ehCapturadoPela(jogada)) {
                gruposQueDevemSerCapturados.add(grupo);
                Log.i(TestesActivity.TAG, "Grupo " + grupo + " será capturado.");
            }
        }
        return gruposQueDevemSerCapturados;
    }

    private boolean gruposForamRealmenteCapturados(Set<Grupo> gruposQueDevemSerCapturados) {
        for (Grupo grupo : gruposQueDevemSerCapturados) {
            for (Posicao posicao : grupo.getPosicoes()) {
                if (tabuleiro[posicao.linha][posicao.coluna] != Tabuleiro.VAZIO) {
                    Log.i(TestesActivity.TAG, "Captura não foi feita corretamente na posição " + posicao);
                    return false;
                }
            }
        }
    }

    private boolean demaisPosicoesEstaoIguais(Set<Posicao> posicoesJaVerificadas) {
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                if (posicoesJaVerificadas.contains(new Posicao(i, j))) continue;
                if (tabuleiro[i][j] != anterior.getPosicao(i, j)) {
                    Log.i(TestesActivity.TAG, "Problema na posição (" + i + ", " + j + ")");
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Retorna o grupo que está em determinada posição do tabuleiro ou nulo caso não haja nenhum
     * grupo.
     * @param linha
     * @param coluna
     * @return
     */
    public Grupo grupoEm(int linha, int coluna) {
        if (ehPosicaoInvalida(linha, coluna)) return null;
        if (tabuleiro[linha][coluna] == Tabuleiro.VAZIO) return null;

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

    private boolean ehPosicaoInvalida(int linha, int coluna) {
        return linha < 0 || coluna < 0 || linha >= dimensao || coluna >= dimensao;
    }

    /**
     * Faz busca em profundidade para encontrar todas as pedras que fazem parte deste grupo.
     */
    private void delimitarGrupo(int linha, int coluna, boolean[][] posicoesVisitadas, Grupo grupo) {
        if (ehPosicaoInvalida(linha, coluna)) return;
        if (posicoesVisitadas[linha][coluna]) return;

        posicoesVisitadas[linha][coluna] = true;

        if (tabuleiro[linha][coluna] == Tabuleiro.VAZIO) {
            grupo.adicionarLiberdade(new Posicao(linha, coluna));
        }
        else if (tabuleiro[linha][coluna] == grupo.getCor()) {
            grupo.adicionarPosicao(new Posicao(linha, coluna));

            delimitarGrupo(linha - 1, coluna, posicoesVisitadas, grupo);
            delimitarGrupo(linha + 1, coluna, posicoesVisitadas, grupo);
            delimitarGrupo(linha, coluna - 1, posicoesVisitadas, grupo);
            delimitarGrupo(linha, coluna + 1, posicoesVisitadas, grupo);
        }
        // Se não entrou em nenhuma das condições, encontrou uma pedra de cor diferente da do grupo
    }

	/**
	 * Retorna um novo tabuleiro com a jogada passada como parâmetro feita. Se a jogada não for
     * válida, retorna o tabuleiro antigo.
	 */
	public Tabuleiro gerarNovoTabuleiroComAJogada(Jogada jogada) {
        if (jogada == null) return this;
        if (tabuleiro[jogada.linha][jogada.coluna] != VAZIO) return this;

        Tabuleiro novoTabuleiro = new Tabuleiro(this);

        for (Grupo grupo : recuperaGruposAdjacentesA(jogada)) {
            if (grupo == null) continue;
            if (grupo.ehCapturadoPela(jogada)) {
                novoTabuleiro.remove(grupo);
            }
        }

        novoTabuleiro.tabuleiro[jogada.linha][jogada.coluna] = jogada.cor;

        Grupo grupoDaJogada = novoTabuleiro.grupoEm(jogada.linha, jogada.coluna);
        if (grupoDaJogada.naoTemLiberdades()) return this;

		return novoTabuleiro;
	}

    private Set<Grupo> recuperaGruposAdjacentesA(Jogada jogada) {
        Set<Grupo> gruposAdjacentesAJogada = new HashSet<>();
        gruposAdjacentesAJogada.add(grupoEm(jogada.linha - 1, jogada.coluna));
        gruposAdjacentesAJogada.add(grupoEm(jogada.linha + 1, jogada.coluna));
        gruposAdjacentesAJogada.add(grupoEm(jogada.linha, jogada.coluna - 1));
        gruposAdjacentesAJogada.add(grupoEm(jogada.linha, jogada.coluna + 1));
        return gruposAdjacentesAJogada;
    }

    private void remove(Grupo grupo) {
        for (Posicao posicao : grupo.getPosicoes()) {
            tabuleiro[posicao.linha][posicao.coluna] = VAZIO;
        }
    }

}
