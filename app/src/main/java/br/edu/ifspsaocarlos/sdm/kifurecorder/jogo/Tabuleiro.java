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

    public Tabuleiro(Tabuleiro tabuleiro) {
        this.dimensao = tabuleiro.dimensao;
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
        if (ehPosicaoInvalida(linha, coluna)) {
            throw new RuntimeException("Posição inválida!");
        }
        if (tabuleiro[linha][coluna] != VAZIO) {
			throw new RuntimeException("Já existe uma pedra nessa posição!");
		}
        tabuleiro[linha][coluna] = pedra;
    }

    private boolean ehPosicaoInvalida(int linha, int coluna) {
        return linha < 0 || coluna < 0 || linha >= dimensao || coluna >= dimensao;
    }

    public int getPosicao(int linha, int coluna) {
        return tabuleiro[linha][coluna];
    }

    public String toString() {
        StringBuilder saida = new StringBuilder();

        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                if (tabuleiro[i][j] == VAZIO) saida.append('.');
                else if (tabuleiro[i][j] == PEDRA_PRETA) saida.append('P');
                else if (tabuleiro[i][j] == PEDRA_BRANCA) saida.append('B');
            }
            saida.append(System.getProperty("line.separator"));
        }

        return saida.toString();
    }

    /**
     * Retorna um novo tabuleiro que correponde ao tabuleiro atual rotacionado em sentido horário
     * (direcao = 1) ou anti-horário (direcao = -1).
     */
    public Tabuleiro rotacionar(int direcao) {
        if (direcao == -1) return rotacionarEmSentidoAntihorario();
        else if (direcao == 1) return rotacionarEmSentidoHorario();
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
                if (getPosicao(i, j) != outroTabuleiro.getPosicao(i, j)) return false;
            }
        }
        return true;
    }

    /**
     * Verifica se dois tabuleiros são iguais, incluindo se um tabuleiro é uma rotação do outro.
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
     * Retorna a jogada de diferença deste tabuleiro para o anterior. Se não for possível chegar no
     * estado do tabuleiro atual a partir do anterior, retorna nulo.
     */
    public Jogada diferenca(Tabuleiro tabuleiroAnterior) {
        int numeroDePedrasPretasAntes = tabuleiroAnterior.numeroDePedrasDaCor(Tabuleiro.PEDRA_PRETA);
        int numeroDePedrasBrancasAntes = tabuleiroAnterior.numeroDePedrasDaCor(Tabuleiro.PEDRA_BRANCA);
        int numeroDePedrasPretasDepois = numeroDePedrasDaCor(Tabuleiro.PEDRA_PRETA);
        int numeroDePedrasBrancasDepois = numeroDePedrasDaCor(Tabuleiro.PEDRA_BRANCA);
        int diferencaDePedrasPretas = numeroDePedrasPretasDepois - numeroDePedrasPretasAntes;
        int diferencaDePedrasBrancas = numeroDePedrasBrancasDepois - numeroDePedrasBrancasAntes;
        int corDaJogada;

        if (diferencaDePedrasPretas == 1) corDaJogada = Tabuleiro.PEDRA_PRETA;
        else if (diferencaDePedrasBrancas == 1) corDaJogada = Tabuleiro.PEDRA_BRANCA;
        else return null;
        Jogada jogadaFeita = jogadaDiferenteEntreTabuleiroAtualE(tabuleiroAnterior, corDaJogada);

        if (tabuleiroAnterior.gerarNovoTabuleiroComAJogada(jogadaFeita).identico(this)) {
            return jogadaFeita;
        }
        return null;
    }

    private int numeroDePedrasDaCor(int cor) {
        int numeroDePedrasDessaCor = 0;
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                if (tabuleiro[i][j] == cor) {
                    ++numeroDePedrasDessaCor;
                }
            }
        }
        return numeroDePedrasDessaCor;
    }

    /**
     * Retorna a primeira pedra da cor especificada encontrada diferente entre os dois tabuleiros.
     */
    private Jogada jogadaDiferenteEntreTabuleiroAtualE(Tabuleiro anterior, int cor) {
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
     * Retorna um novo tabuleiro com a jogada passada como parâmetro feita. Se a jogada não for
     * válida, retorna o tabuleiro antigo.
     */
	public Tabuleiro gerarNovoTabuleiroComAJogada(Jogada jogada) {
        if (jogada == null || tabuleiro[jogada.linha][jogada.coluna] != VAZIO) return this;

        Tabuleiro novoTabuleiro = new Tabuleiro(this);

        for (Grupo grupo : recuperaGruposAdjacentesA(jogada)) {
            if (grupo == null) continue;
            if (grupo.ehCapturadoPela(jogada)) novoTabuleiro.remove(grupo);
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

    /**
     * Retorna o grupo que está em determinada posição do tabuleiro ou nulo caso não haja nenhum
     * grupo.
     */
    public Grupo grupoEm(int linha, int coluna) {
        if (ehPosicaoInvalida(linha, coluna) || tabuleiro[linha][coluna] == Tabuleiro.VAZIO) return null;

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

    /**
     * Faz busca em profundidade para encontrar todas as pedras que fazem parte deste grupo e
     * suas liberdades.
     */
    private void delimitarGrupo(int linha, int coluna, boolean[][] posicoesVisitadas, Grupo grupo) {
        if (ehPosicaoInvalida(linha, coluna) || posicoesVisitadas[linha][coluna]) return;

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
    }

}
