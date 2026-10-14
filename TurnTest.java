import java.util.IdentityHashMap;
import java.util.Random;

/**
 * Tests for Turn: one normal case and one bad-input case per public
 * method, plus the 13-dice invariant from our scope's biggest risk.
 *
 * Draw order inside roll(): first one nextInt per die drawn
 * (removeRandom), then one nextInt(6) per die rolled.
 *
 * @author Sathvik Verma
 * @version October 14, 2026
 */
public class TurnTest extends student.TestCase
{
    private Player ana;

    /**
     * Fresh player before each test.
     */
    public void setUp()
    {
        ana = new Player("Ana");
    }

    /**
     * Builds a cup holding the given number of dice of one color.
     */
    private LinkedChain<Die> cupOf(Die.DieColor color, int count)
    {
        LinkedChain<Die> cup = new LinkedChain<Die>();
        for (int i = 0; i < count; i++)
        {
            cup.add(new Die(color));
        }
        return cup;
    }

    // ----- constructor -----

    /** A new turn starts READY with nothing rolled. */
    public void testConstructorStartsReady()
    {
        Turn turn = new Turn(ana, CaveInApp.createCup(), new Random(1));
        assertEquals(Turn.TurnState.READY, turn.getState());
        assertSame(ana, turn.getPlayer());
        assertEquals(0, turn.getGemCount());
        assertEquals(13, turn.diceAccountedFor());
    }

    /** Null arguments are rejected. */
    public void testConstructorRejectsNulls()
    {
        LinkedChain<Die> cup = CaveInApp.createCup();
        Random rng = new Random(1);
        assertThrowsType(IllegalArgumentException.class,
            () -> new Turn(null, cup, rng));
        assertThrowsType(IllegalArgumentException.class,
            () -> new Turn(ana, null, rng));
        assertThrowsType(IllegalArgumentException.class,
            () -> new Turn(ana, cup, null));
    }

    // ----- roll -----

    /** A first roll uses 3 dice and loses none. */
    public void testRollDrawsThreeAndKeepsThirteen()
    {
        Turn turn = new Turn(ana, CaveInApp.createCup(), new Random(42));
        Die.Face[] faces = turn.roll();
        assertEquals(3, faces.length);
        assertEquals(13, turn.diceAccountedFor());
        assertEquals(turn.getSetAsideSize(),
            turn.getGemCount() + turn.getRockfallCount());
    }

    /** Three red rockfalls cave in the turn. */
    public void testRollThreeRockfallsCavesIn()
    {
        Turn turn = new Turn(ana, cupOf(Die.DieColor.RED, 3),
            new ScriptedRandom(0, 0, 0, 3, 4, 5));
        turn.roll();
        assertEquals(3, turn.getRockfallCount());
        assertTrue(turn.isCavedIn());
        assertTrue(turn.isOver());
        assertFalse(turn.canDig());
        assertFalse(turn.canBank());
    }

    /** Digging after a cave-in is not allowed. */
    public void testRollAfterCaveInThrows()
    {
        Turn turn = new Turn(ana, cupOf(Die.DieColor.RED, 6),
            new ScriptedRandom(0, 0, 0, 3, 3, 3));
        turn.roll();
        assertThrowsType(IllegalStateException.class, () -> turn.roll());
    }

    /** Digging after banking is not allowed. */
    public void testRollAfterBankThrows()
    {
        Turn turn = new Turn(ana, cupOf(Die.DieColor.GREEN, 6),
            new ScriptedRandom(0));
        turn.roll();
        turn.bank();
        assertThrowsType(IllegalStateException.class, () -> turn.roll());
    }

    /** A footprint stays in the hand and is rolled again. */
    public void testRollFootprintStaysInHand()
    {
        // draws 0,0,0 then faces 5 (FOOTPRINT), 0 (GEM), 0 (GEM)
        Turn turn = new Turn(ana, cupOf(Die.DieColor.GREEN, 3),
            new ScriptedRandom(0, 0, 0, 5, 0, 0));
        turn.roll();
        assertEquals(1, turn.getHandSize());
        assertEquals(2, turn.getGemCount());
        // cup is empty, so the next roll only rolls the footprint die
        Die.Face[] faces = turn.roll();
        assertEquals(1, faces.length);
        assertEquals(3, turn.diceAccountedFor());
    }

    /** With no dice in hand or cup, digging is refused. */
    public void testRollWithNoDiceLeftThrows()
    {
        Turn turn = new Turn(ana, cupOf(Die.DieColor.GREEN, 3),
            new ScriptedRandom(0));
        turn.roll(); // all three gems, cup now empty
        assertFalse(turn.canDig());
        assertTrue(turn.canBank());
        assertThrowsType(IllegalStateException.class, () -> turn.roll());
    }

    // ----- canDig / canBank -----

    /** At the start you can dig but not bank. */
    public void testCanDigCanBankBeforeRoll()
    {
        Turn turn = new Turn(ana, CaveInApp.createCup(), new Random(1));
        assertTrue(turn.canDig());
        assertFalse(turn.canBank());
    }

    /** An empty cup with an empty hand means no digging. */
    public void testCanDigFalseWithEmptyCup()
    {
        Turn turn = new Turn(ana, new LinkedChain<Die>(), new Random(1));
        assertFalse(turn.canDig());
        assertFalse(turn.canBank());
    }

    // ----- bank -----

    /** Banking adds the gems to the score. */
    public void testBankAddsGems()
    {
        Turn turn = new Turn(ana, cupOf(Die.DieColor.GREEN, 6),
            new ScriptedRandom(0));
        turn.roll();
        assertEquals(3, turn.bank());
        assertEquals(3, ana.getScore());
        assertEquals(Turn.TurnState.BANKED, turn.getState());
    }

    /** Banking before the first roll is refused. */
    public void testBankBeforeRollThrows()
    {
        Turn turn = new Turn(ana, CaveInApp.createCup(), new Random(1));
        assertThrowsType(IllegalStateException.class, () -> turn.bank());
        assertEquals(0, ana.getScore());
    }

    /** Banking after a cave-in is refused and scores nothing. */
    public void testBankAfterCaveInThrows()
    {
        Turn turn = new Turn(ana, cupOf(Die.DieColor.RED, 3),
            new ScriptedRandom(0, 0, 0, 3, 3, 3));
        turn.roll();
        assertThrowsType(IllegalStateException.class, () -> turn.bank());
        assertEquals(0, ana.getScore());
    }

    // ----- gem / rockfall counts -----

    /** Counts match the faces rolled. */
    public void testCountsAfterMixedRoll()
    {
        // three reds: GEM, ROCKFALL, GEM
        Turn turn = new Turn(ana, cupOf(Die.DieColor.RED, 3),
            new ScriptedRandom(0, 0, 0, 0, 3, 1));
        turn.roll();
        assertEquals(2, turn.getGemCount());
        assertEquals(1, turn.getRockfallCount());
        assertFalse(turn.isOver());
    }

    /** Nothing rolled means zero of each. */
    public void testCountsBeforeRollAreZero()
    {
        Turn turn = new Turn(ana, CaveInApp.createCup(), new Random(1));
        assertEquals(0, turn.getGemCount());
        assertEquals(0, turn.getRockfallCount());
    }

    // ----- getHand -----

    /** The hand copy holds the footprint die and doesn't empty the hand. */
    public void testGetHandReturnsFootprintDice()
    {
        Turn turn = new Turn(ana, cupOf(Die.DieColor.GREEN, 3),
            new ScriptedRandom(0, 0, 0, 5, 0, 0));
        turn.roll();
        Die[] hand = turn.getHand();
        assertEquals(1, hand.length);
        assertEquals(Die.DieColor.GREEN, hand[0].getColor());
        assertEquals(1, turn.getHandSize());
    }

    /** Before any roll the hand is an empty array, not null. */
    public void testGetHandEmptyBeforeRoll()
    {
        Turn turn = new Turn(ana, CaveInApp.createCup(), new Random(1));
        assertNotNull(turn.getHand());
        assertEquals(0, turn.getHand().length);
    }

    // ----- returnDiceToCup -----

    /** Every die goes back after a turn. */
    public void testReturnDiceToCupRefillsCup()
    {
        LinkedChain<Die> cup = CaveInApp.createCup();
        Turn turn = new Turn(ana, cup, new Random(7));
        turn.roll();
        turn.returnDiceToCup();
        assertEquals(13, cup.size());
        assertEquals(0, turn.getHandSize());
        assertEquals(0, turn.getSetAsideSize());
        assertEquals(0, turn.getGemCount());
    }

    /** Returning dice when none are out changes nothing. */
    public void testReturnDiceToCupOnFreshTurn()
    {
        LinkedChain<Die> cup = CaveInApp.createCup();
        Turn turn = new Turn(ana, cup, new Random(7));
        turn.returnDiceToCup();
        assertEquals(13, cup.size());
    }

    // ----- the 13-dice invariant -----

    /**
     * Plays 500 random turns. After every roll there must be 13 dice
     * between cup, hand and pile; after every turn the cup must hold the
     * same 13 dice objects (6 green, 4 yellow, 3 red, none copied).
     */
    public void testThirteenDiceNeverLostOrCopied()
    {
        LinkedChain<Die> cup = CaveInApp.createCup();
        Die[] original = cup.toArray(new Die[13]);
        Random rng = new Random(2114);

        for (int t = 0; t < 500; t++)
        {
            Turn turn = new Turn(ana, cup, rng);
            while (!turn.isOver())
            {
                if (turn.canDig() && (!turn.canBank() || rng.nextBoolean()))
                {
                    turn.roll();
                    assertEquals(13, turn.diceAccountedFor());
                }
                else
                {
                    turn.bank();
                }
            }
            turn.returnDiceToCup();
            assertEquals(13, cup.size());
        }

        IdentityHashMap<Die, Boolean> seen = new IdentityHashMap<>();
        int green = 0;
        int yellow = 0;
        int red = 0;
        for (Die die : cup.toArray(new Die[13]))
        {
            assertNull("die appears twice", seen.put(die, true));
            if (die.getColor() == Die.DieColor.GREEN)
            {
                green++;
            }
            else if (die.getColor() == Die.DieColor.YELLOW)
            {
                yellow++;
            }
            else
            {
                red++;
            }
        }
        for (Die die : original)
        {
            assertTrue("original die went missing", seen.containsKey(die));
        }
        assertEquals(6, green);
        assertEquals(4, yellow);
        assertEquals(3, red);
    }

    /**
     * Checks that running the action throws the given exception type.
     *
     * @param type   the exception class expected
     * @param action the code that should throw
     */
    private void assertThrowsType(Class<? extends Exception> type,
        Runnable action)
    {
        Exception thrown = null;
        try
        {
            action.run();
        }
        catch (Exception e)
        {
            thrown = e;
        }
        assertNotNull("expected " + type.getSimpleName(), thrown);
        assertTrue("expected " + type.getSimpleName() + " but got "
            + thrown.getClass().getSimpleName(), type.isInstance(thrown));
    }
}
