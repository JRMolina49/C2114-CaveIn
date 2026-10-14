/**
 * Tests for Game: one normal case and one bad-input case per public
 * method.
 *
 * @author Sathvik Verma
 * @version October 14, 2026
 */
public class GameTest extends student.TestCase
{
    private Player ana;
    private Player ben;
    private Player cal;
    private LinkedChain<Die> cup;
    private Game game;

    /**
     * A 3-player game with a full cup before each test.
     */
    public void setUp()
    {
        ana = new Player("Ana");
        ben = new Player("Ben");
        cal = new Player("Cal");
        cup = CaveInApp.createCup();
        game = new Game(new Player[] {ana, ben, cal}, cup);
    }

    // ----- constructor -----

    /** A new game starts with player 1, no winner, not over. */
    public void testConstructor()
    {
        assertSame(ana, game.getCurrentPlayer());
        assertFalse(game.isGameOver());
        assertNull(game.getWinner());
    }

    /** Null or empty players, and a null or empty cup, are rejected. */
    public void testConstructorRejectsBadSetup()
    {
        Player[] players = {ana, ben};
        assertTrue(rejects(null, cup));
        assertTrue(rejects(new Player[0], cup));
        assertTrue(rejects(players, null));
        assertTrue(rejects(players, new LinkedChain<Die>()));
    }

    // ----- getCurrentPlayer / nextTurn -----

    /** Turns go in order and wrap back to the first player. */
    public void testNextTurnWrapsAround()
    {
        game.nextTurn();
        assertSame(ben, game.getCurrentPlayer());
        game.nextTurn();
        assertSame(cal, game.getCurrentPlayer());
        game.nextTurn();
        assertSame(ana, game.getCurrentPlayer());
    }

    /** Once the game is over, nextTurn does nothing. */
    public void testNextTurnAfterGameOver()
    {
        game.nextTurn(); // Ben
        game.endGame(ben);
        game.nextTurn();
        assertSame(ben, game.getCurrentPlayer());
    }

    // ----- endGame / isGameOver / getWinner -----

    /** endGame records the winner and ends the game. */
    public void testEndGame()
    {
        game.endGame(cal);
        assertTrue(game.isGameOver());
        assertSame(cal, game.getWinner());
    }

    /** Before endGame there is no winner. */
    public void testNoWinnerBeforeEndGame()
    {
        game.nextTurn();
        assertFalse(game.isGameOver());
        assertNull(game.getWinner());
    }

    // ----- getPlayers / getCup -----

    /** getPlayers returns all players in order. */
    public void testGetPlayers()
    {
        Player[] players = game.getPlayers();
        assertEquals(3, players.length);
        assertSame(ana, players[0]);
        assertSame(cal, players[2]);
    }

    /** getCup returns the same 13-die cup the game was given. */
    public void testGetCup()
    {
        assertSame(cup, game.getCup());
        assertEquals(13, game.getCup().size());
    }

    /**
     * @return true if building a Game with these arguments throws
     *         IllegalArgumentException
     */
    private boolean rejects(Player[] players, LinkedChain<Die> aCup)
    {
        try
        {
            new Game(players, aCup);
            return false;
        }
        catch (IllegalArgumentException e)
        {
            return true;
        }
    }
}
