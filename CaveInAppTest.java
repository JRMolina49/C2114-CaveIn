import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Random;
import java.util.Scanner;

/**
 * Tests for CaveInApp. Input is fed from a String and output is captured,
 * so no keyboard is needed. ScriptedRandom(0) always draws the first die
 * and always rolls face 0, which is a GEM on every color.
 *
 * @author Sathvik Verma
 * @version October 14, 2026
 */
public class CaveInAppTest extends student.TestCase
{
    private ByteArrayOutputStream printed;

    /**
     * Makes an app that reads the given text as if it were typed.
     */
    private CaveInApp appWithInput(String input, Random rng)
    {
        printed = new ByteArrayOutputStream();
        return new CaveInApp(new Scanner(input), new PrintStream(printed),
            rng);
    }

    private CaveInApp appWithInput(String input)
    {
        return appWithInput(input, new ScriptedRandom(0));
    }

    /** How many times a message appears in what was printed. */
    private int timesPrinted(String message)
    {
        String text = printed.toString();
        int count = 0;
        int at = text.indexOf(message);
        while (at >= 0)
        {
            count++;
            at = text.indexOf(message, at + 1);
        }
        return count;
    }

    // ----- constructor -----

    /** A valid app has no game until setup. */
    public void testConstructor()
    {
        assertNull(appWithInput("").getGame());
    }

    /** Null arguments are rejected. */
    public void testConstructorRejectsNulls()
    {
        assertThrowsType(IllegalArgumentException.class,
            () -> new CaveInApp(null, System.out, new Random()));
        assertThrowsType(IllegalArgumentException.class,
            () -> new CaveInApp(new Scanner(""), null, new Random()));
        assertThrowsType(IllegalArgumentException.class,
            () -> new CaveInApp(new Scanner(""), System.out, null));
    }

    // ----- createCup -----

    /** The cup holds 6 green, 4 yellow, 3 red. */
    public void testCreateCup()
    {
        LinkedChain<Die> cup = CaveInApp.createCup();
        assertEquals(13, cup.size());
        int green = 0;
        int yellow = 0;
        int red = 0;
        for (Die die : cup.toArray(new Die[13]))
        {
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
        assertEquals(6, green);
        assertEquals(4, yellow);
        assertEquals(3, red);
    }

    /** Each call makes a separate cup. */
    public void testCreateCupIsFreshEachTime()
    {
        LinkedChain<Die> first = CaveInApp.createCup();
        first.clear();
        assertEquals(13, CaveInApp.createCup().size());
    }

    // ----- readPlayerCount -----

    /** A valid count is returned. */
    public void testReadPlayerCount()
    {
        assertEquals(3, appWithInput("3\n").readPlayerCount());
    }

    /** Words and out-of-range numbers are re-prompted. */
    public void testReadPlayerCountRepromptsBadInput()
    {
        CaveInApp app = appWithInput("five\n1\n5\n 4 \n");
        assertEquals(4, app.readPlayerCount());
        assertEquals(3, timesPrinted("Enter a number from 2 to 4."));
    }

    /** Running out of input stops cleanly instead of looping. */
    public void testReadPlayerCountNoInput()
    {
        assertThrowsType(IllegalStateException.class,
            () -> appWithInput("").readPlayerCount());
    }

    // ----- readName -----

    /** A name is returned trimmed. */
    public void testReadName()
    {
        assertEquals("Ana", appWithInput("  Ana  \n").readName(1));
    }

    /** Blank names are re-prompted. */
    public void testReadNameRepromptsBlank()
    {
        CaveInApp app = appWithInput("\n    \nAna\n");
        assertEquals("Ana", app.readName(1));
        assertEquals(2, timesPrinted("Name can't be blank."));
    }

    // ----- readCommand -----

    /** dig and bank are accepted in any case. */
    public void testReadCommandAnyCase()
    {
        CaveInApp app = appWithInput("DIG\nBank\n");
        Turn turn = new Turn(new Player("Ana"), CaveInApp.createCup(),
            new ScriptedRandom(0));
        assertEquals("dig", app.readCommand(turn));
        turn.roll();
        assertEquals("bank", app.readCommand(turn));
    }

    /** Other words are re-prompted. */
    public void testReadCommandRepromptsUnknownWord()
    {
        CaveInApp app = appWithInput("roll\n\ndig\n");
        Turn turn = new Turn(new Player("Ana"), CaveInApp.createCup(),
            new Random(1));
        assertEquals("dig", app.readCommand(turn));
        assertEquals(2, timesPrinted("Type dig or bank."));
    }

    /** Banking before rolling is re-prompted. */
    public void testReadCommandBankBeforeRoll()
    {
        CaveInApp app = appWithInput("bank\ndig\n");
        Turn turn = new Turn(new Player("Ana"), CaveInApp.createCup(),
            new Random(1));
        assertEquals("dig", app.readCommand(turn));
        assertEquals(1, timesPrinted("Roll at least once before banking."));
    }

    /** Digging with no dice left is re-prompted. */
    public void testReadCommandDigWithNoDice()
    {
        LinkedChain<Die> cup = new LinkedChain<Die>();
        for (int i = 0; i < 3; i++)
        {
            cup.add(new Die(Die.DieColor.GREEN));
        }
        Turn turn = new Turn(new Player("Ana"), cup, new ScriptedRandom(0));
        turn.roll(); // all gems, nothing left
        CaveInApp app = appWithInput("dig\nbank\n");
        assertEquals("bank", app.readCommand(turn));
        assertEquals(1, timesPrinted("No dice left - you must bank."));
    }

    // ----- setUpGame -----

    /** Players are created in the order entered, with a full cup. */
    public void testSetUpGame()
    {
        Game game = appWithInput("2\nAna\nBen\n").setUpGame();
        assertEquals(2, game.getPlayers().length);
        assertEquals("Ana", game.getPlayers()[0].getName());
        assertEquals("Ben", game.getPlayers()[1].getName());
        assertEquals(13, game.getCup().size());
        assertSame(game.getPlayers()[0], game.getCurrentPlayer());
    }

    /** Bad count and blank name are re-prompted during setup. */
    public void testSetUpGameRepromptsBadInput()
    {
        CaveInApp app = appWithInput("1\n2\n\nAna\nBen\n");
        Game game = app.setUpGame();
        assertEquals(2, game.getPlayers().length);
        assertEquals(1, timesPrinted("Enter a number from 2 to 4."));
        assertEquals(1, timesPrinted("Name can't be blank."));
    }

    // ----- playTurn -----

    /** dig then bank scores the gems and passes play on. */
    public void testPlayTurnBank()
    {
        CaveInApp app = appWithInput("2\nAna\nBen\ndig\nbank\n");
        Game game = app.setUpGame();
        app.playTurn();
        assertEquals(3, game.getPlayers()[0].getScore());
        assertEquals("Ben", game.getCurrentPlayer().getName());
        assertEquals(13, game.getCup().size());
        assertTrue(printed.toString().contains("Scores:"));
    }

    /** A cave-in scores nothing and passes play on. */
    public void testPlayTurnCaveIn()
    {
        // createCup adds reds last, so they sit at the front of the chain:
        // draw index 0 three times = three reds; face 3 = ROCKFALL.
        CaveInApp app = appWithInput("2\nAna\nBen\ndig\n",
            new ScriptedRandom(0, 0, 0, 3, 3, 3));
        Game game = app.setUpGame();
        app.playTurn();
        assertEquals(0, game.getPlayers()[0].getScore());
        assertEquals("Ben", game.getCurrentPlayer().getName());
        assertEquals(13, game.getCup().size());
        assertTrue(printed.toString().contains("CAVE-IN!"));
    }

    /** Banking to 13 or more ends the game. */
    public void testPlayTurnWins()
    {
        CaveInApp app = appWithInput("2\nAna\nBen\ndig\nbank\n");
        Game game = app.setUpGame();
        game.getPlayers()[0].addPoints(12);
        app.playTurn();
        assertTrue(game.isGameOver());
        assertEquals("Ana", game.getWinner().getName());
    }

    // ----- printRoll / printStatus -----

    /** The rolled faces are printed. */
    public void testPrintRoll()
    {
        CaveInApp app = appWithInput("");
        app.printRoll(new Die.Face[] {Die.Face.GEM, Die.Face.ROCKFALL});
        assertTrue(printed.toString().contains("Rolled: GEM ROCKFALL"));
    }

    /** An empty roll prints without crashing. */
    public void testPrintRollEmpty()
    {
        CaveInApp app = appWithInput("");
        app.printRoll(new Die.Face[0]);
        assertTrue(printed.toString().contains("Rolled:"));
    }

    /** Status shows the turn counts and every score. */
    public void testPrintStatus()
    {
        CaveInApp app = appWithInput("2\nAna\nBen\n");
        Game game = app.setUpGame();
        Turn turn = new Turn(game.getCurrentPlayer(), game.getCup(),
            new ScriptedRandom(0));
        turn.roll();
        app.printStatus(turn);
        String text = printed.toString();
        assertTrue(text.contains("Gems: 3"));
        assertTrue(text.contains("Ana 0"));
        assertTrue(text.contains("Ben 0"));
    }

    // ----- run -----

    /** A whole game plays to a winner, refusing a dig with no dice. */
    public void testRunFullGame()
    {
        // Every roll is 3 gems. Ana digs until the cup is empty
        // (3+3+3+3+1 = 13 gems), tries one more dig, then banks 13.
        CaveInApp app = appWithInput(
            "2\nAna\nBen\ndig\ndig\ndig\ndig\ndig\ndig\nbank\n");
        app.run();
        assertTrue(app.getGame().isGameOver());
        assertEquals("Ana", app.getGame().getWinner().getName());
        assertEquals(13, app.getGame().getWinner().getScore());
        assertTrue(printed.toString().contains("Ana wins with 13 points!"));
        assertEquals(1, timesPrinted("No dice left - you must bank."));
    }

    /** Input ending mid-game stops with an exception main() catches. */
    public void testRunInputEndsEarly()
    {
        CaveInApp app = appWithInput("2\nAna\n");
        assertThrowsType(IllegalStateException.class, () -> app.run());
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
