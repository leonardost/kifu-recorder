package br.edu.ifspsaocarlos.sdm.kifurecorder;

import org.junit.Test;

import android.test.AndroidTestCase;

import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Board;
import br.edu.ifspsaocarlos.sdm.kifurecorder.jogo.Move;

public class JogadaTest extends AndroidTestCase {

    @Test
    public void testSgf() {
        Move move = new Move(0, 0, Board.PEDRA_PRETA);
        assertEquals("[aa]", move.sgf());
    }

}
