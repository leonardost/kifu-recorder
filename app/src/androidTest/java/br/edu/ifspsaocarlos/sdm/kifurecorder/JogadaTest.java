package br.edu.ifspsaocarlos.sdm.kifurecorder;

import android.test.AndroidTestCase;

import br.edu.ifspsaocarlos.sdm.kifurecorder.models.Board;
import br.edu.ifspsaocarlos.sdm.kifurecorder.models.Move;

public class JogadaTest extends AndroidTestCase {

    public void testSgf() {
        Move move = new Move(0, 0, Board.PEDRA_PRETA);
        assertEquals("[aa]", move.sgf());
    }

}
