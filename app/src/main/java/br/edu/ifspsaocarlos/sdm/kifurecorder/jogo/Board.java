package br.edu.ifspsaocarlos.sdm.kifurecorder.jogo;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Representa um estado de tabuleiro
 */
public class Board implements Serializable {

    public final static int VAZIO = 0;
    public final static int PEDRA_PRETA = 1;
    public final static int PEDRA_BRANCA = 2;

    private int dimensao;
    private Integer[][] tabuleiro;

    public Board(int dimensao) {
        this.dimensao = dimensao;
        this.tabuleiro = new Integer[dimensao][dimensao];
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                tabuleiro[i][j] = VAZIO;
            }
        }
    }

    public Board(Board board) {
        this.dimensao = board.dimensao;
        this.tabuleiro = new Integer[dimensao][dimensao];
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                this.tabuleiro[i][j] = board.tabuleiro[i][j];
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
    public Board rotacionar(int direcao) {
        if (direcao == -1) return rotacionarEmSentidoAntihorario();
        else if (direcao == 1) return rotacionarEmSentidoHorario();
        throw new RuntimeException("Direção de rotação inválida!");
    }

    private Board rotacionarEmSentidoHorario() {
        Board rotatedBoard = new Board(dimensao);
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                if (tabuleiro[dimensao - 1 - j][i] != Board.VAZIO) {
                    rotatedBoard.colocarPedra(i, j, tabuleiro[dimensao - 1 - j][i]);
                }
            }
        }
        return rotatedBoard;
    }

    private Board rotacionarEmSentidoAntihorario() {
        Board rotatedBoard = new Board(dimensao);
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                if (tabuleiro[j][dimensao - 1 - i] != Board.VAZIO) {
                    rotatedBoard.colocarPedra(i, j, tabuleiro[j][dimensao - 1 - i]);
                }
            }
        }
        return rotatedBoard;
    }

    public boolean identico(Board otherBoard) {
        if (dimensao != otherBoard.dimensao) return false;
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                if (getPosicao(i, j) != otherBoard.getPosicao(i, j)) return false;
            }
        }
        return true;
    }

    /**
     * Verifica se dois tabuleiros são iguais, incluindo se um tabuleiro é uma rotação do outro.
     */
    @Override
    public boolean equals(Object objeto) {
        if (!(objeto instanceof Board)) return false;
        Board otherBoard = (Board)objeto;
        if (dimensao != otherBoard.dimensao) return false;

        Board rotacao1 = otherBoard.rotacionarEmSentidoHorario();
        Board rotacao2 = rotacao1.rotacionarEmSentidoHorario();
        Board rotacao3 = rotacao2.rotacionarEmSentidoHorario();

        return this.identico(otherBoard) || this.identico(rotacao1) || this.identico(rotacao2) || this.identico(rotacao3);
    }

    /**
     * Retorna a jogada de diferença deste tabuleiro para o anterior. Se não for possível chegar no
     * estado do tabuleiro atual a partir do anterior, retorna nulo.
     */
    public Move diferenca(Board previousBoard) {
        int numeroDePedrasPretasAntes = previousBoard.numeroDePedrasDaCor(Board.PEDRA_PRETA);
        int numeroDePedrasBrancasAntes = previousBoard.numeroDePedrasDaCor(Board.PEDRA_BRANCA);
        int numeroDePedrasPretasDepois = numeroDePedrasDaCor(Board.PEDRA_PRETA);
        int numeroDePedrasBrancasDepois = numeroDePedrasDaCor(Board.PEDRA_BRANCA);
        int diferencaDePedrasPretas = numeroDePedrasPretasDepois - numeroDePedrasPretasAntes;
        int diferencaDePedrasBrancas = numeroDePedrasBrancasDepois - numeroDePedrasBrancasAntes;
        int corDaJogada;

        if (diferencaDePedrasPretas == 1) corDaJogada = Board.PEDRA_PRETA;
        else if (diferencaDePedrasBrancas == 1) corDaJogada = Board.PEDRA_BRANCA;
        else return null;
        Move movePlayed = jogadaDiferenteEntreTabuleiroAtualE(previousBoard, corDaJogada);

        if (previousBoard.gerarNovoTabuleiroComAJogada(movePlayed).identico(this)) {
            return movePlayed;
        }
        return null;
    }

    private int numeroDePedrasDaCor(int cor) {
        int numeroDePedrasDessaCor = 0;
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                if (tabuleiro[i][j] == cor) ++numeroDePedrasDessaCor;
            }
        }
        return numeroDePedrasDessaCor;
    }

    /**
     * Retorna a primeira pedra da cor especificada encontrada diferente entre os dois tabuleiros.
     */
    private Move jogadaDiferenteEntreTabuleiroAtualE(Board anterior, int cor) {
        for (int i = 0; i < anterior.getDimensao(); ++i) {
            for (int j = 0; j < anterior.getDimensao(); ++j) {
                if (tabuleiro[i][j] == cor && anterior.getPosicao(i, j) != tabuleiro[i][j]) {
                    return new Move(i, j, cor);
                }
            }
        }
        return null;
    }

    /**
     * Retorna um novo tabuleiro com a jogada passada como parâmetro feita. Se a jogada não for
     * válida, retorna o tabuleiro antigo.
     */
	public Board gerarNovoTabuleiroComAJogada(Move move) {
        if (move == null || tabuleiro[move.linha][move.coluna] != VAZIO) return this;

        Board newBoard = new Board(this);

        for (Group group : recuperaGruposAdjacentesA(move)) {
            if (group == null) continue;
            if (group.ehCapturadoPela(move)) newBoard.remove(group);
        }

        newBoard.tabuleiro[move.linha][move.coluna] = move.cor;

        Group groupOfMove = newBoard.grupoEm(move.linha, move.coluna);
        if (groupOfMove.naoTemLiberdades()) return this;

        return newBoard;
	}

    private Set<Group> recuperaGruposAdjacentesA(Move move) {
        Set<Group> gruposAdjacentesAJogada = new HashSet<>();
        gruposAdjacentesAJogada.add(grupoEm(move.linha - 1, move.coluna));
        gruposAdjacentesAJogada.add(grupoEm(move.linha + 1, move.coluna));
        gruposAdjacentesAJogada.add(grupoEm(move.linha, move.coluna - 1));
        gruposAdjacentesAJogada.add(grupoEm(move.linha, move.coluna + 1));
        return gruposAdjacentesAJogada;
    }

    private void remove(Group group) {
        for (Position position : group.getPosicoes()) {
            tabuleiro[position.linha][position.coluna] = VAZIO;
        }
    }

    /**
     * Retorna o grupo que está em determinada posição do tabuleiro ou nulo caso não haja nenhum
     * grupo.
     */
    public Group grupoEm(int linha, int coluna) {
        if (ehPosicaoInvalida(linha, coluna) || tabuleiro[linha][coluna] == Board.VAZIO) return null;

        boolean[][] posicoesVisitadas = new boolean[dimensao][dimensao];
        for (int i = 0; i < dimensao; ++i) {
            for (int j = 0; j < dimensao; ++j) {
                posicoesVisitadas[i][j] = false;
            }
        }

        int cor = tabuleiro[linha][coluna];
        Group group = new Group(cor);
        delimitarGrupo(linha, coluna, posicoesVisitadas, group);
        return group;
    }

    /**
     * Faz busca em profundidade para encontrar todas as pedras que fazem parte deste grupo e
     * suas liberdades.
     */
    private void delimitarGrupo(int linha, int coluna, boolean[][] posicoesVisitadas, Group group) {
        if (ehPosicaoInvalida(linha, coluna) || posicoesVisitadas[linha][coluna]) return;

        posicoesVisitadas[linha][coluna] = true;

        if (tabuleiro[linha][coluna] == Board.VAZIO) {
            group.adicionarLiberdade(new Position(linha, coluna));
        }
        else if (tabuleiro[linha][coluna] == group.getCor()) {
            group.adicionarPosicao(new Position(linha, coluna));

            delimitarGrupo(linha - 1, coluna, posicoesVisitadas, group);
            delimitarGrupo(linha + 1, coluna, posicoesVisitadas, group);
            delimitarGrupo(linha, coluna - 1, posicoesVisitadas, group);
            delimitarGrupo(linha, coluna + 1, posicoesVisitadas, group);
        }
    }

}
