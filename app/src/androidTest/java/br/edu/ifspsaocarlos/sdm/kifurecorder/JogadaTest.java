package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.test.AndroidTestCase;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Move;
import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Tabuleiro;

public class JogadaTest extends AndroidTestCase {

    public void testSgf() {
        Move move = new Move(0, 0, Tabuleiro.PEDRA_PRETA);
        assertEquals("[aa]", move.sgf());
    }

}
