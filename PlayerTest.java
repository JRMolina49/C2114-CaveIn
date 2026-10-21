/**
 * Tests for Player: one normal case and one bad-input case per public
 * method.
 *
 * @author Sathvik Verma
 * @version October 14, 2026
 */
public class PlayerTest extends student.TestCase
{
    private Player ana;

    /**
     * Fresh player before each test.
     */
    public void setUp()
    {
        ana = new Player("Ana");
    }

    // ----- constructor / getName -----

    /** The name is stored as given. */
    public void testGetName()
    {
        assertEquals("Ana", ana.getName());
    }

    /** Names longer than 10 characters are cut to 10. */
    public void testLongNameIsCut()
    {
        assertEquals("Alexandria", new Player("Alexandriana").getName());
    }

    /** Null and blank names are rejected. */
    public void testConstructorRejectsBadNames()
    {
        assertTrue(rejects(null));
        assertTrue(rejects(""));
        assertTrue(rejects("    "));
    }

    // ----- getScore / addPoints -----

    /** A new player starts at 0. */
    public void testGetScoreStartsAtZero()
    {
        assertEquals(0, ana.getScore());
    }

    /** Points add up across banks; adding 0 is allowed. */
    public void testAddPoints()
    {
        ana.addPoints(3);
        ana.addPoints(0);
        ana.addPoints(5);
        assertEquals(8, ana.getScore());
    }

    /** Negative points are rejected and the score doesn't change. */
    public void testAddPointsRejectsNegative()
    {
        ana.addPoints(4);
        Exception thrown = null;
        try
        {
            ana.addPoints(-1);
        }
        catch (IllegalArgumentException e)
        {
            thrown = e;
        }
        assertNotNull(thrown);
        assertEquals(4, ana.getScore());
    }

    // ----- spendPoints -----

    /** Spending lowers the score; spending everything leaves 0. */
    public void testSpendPoints()
    {
        ana.addPoints(6);
        ana.spendPoints(4);
        assertEquals(2, ana.getScore());
        ana.spendPoints(2);
        assertEquals(0, ana.getScore());
    }

    /** Spending more than you have, or a negative amount, is rejected. */
    public void testSpendPointsRejectsBadAmounts()
    {
        ana.addPoints(3);
        assertTrue(spendRejects(4));
        assertTrue(spendRejects(-1));
        assertEquals(3, ana.getScore());
    }

    // ----- addOwnDie / getOwnDice / countOwnDice -----

    /** Own dice are stored and counted by color. */
    public void testAddOwnDie()
    {
        ana.addOwnDie(new Die(Die.DieColor.GOLD));
        ana.addOwnDie(new Die(Die.DieColor.CRACKED));
        ana.addOwnDie(new Die(Die.DieColor.CRACKED));
        assertEquals(3, ana.getOwnDice().size());
        assertEquals(1, ana.countOwnDice(Die.DieColor.GOLD));
        assertEquals(2, ana.countOwnDice(Die.DieColor.CRACKED));
    }

    /** A null die is rejected; a new player has no dice of any color. */
    public void testAddOwnDieRejectsNull()
    {
        Exception thrown = null;
        try
        {
            ana.addOwnDie(null);
        }
        catch (IllegalArgumentException e)
        {
            thrown = e;
        }
        assertNotNull(thrown);
        assertTrue(ana.getOwnDice().isEmpty());
        assertEquals(0, ana.countOwnDice(Die.DieColor.GOLD));
    }

    /**
     * @return true if spending this many points throws
     *         IllegalArgumentException
     */
    private boolean spendRejects(int points)
    {
        try
        {
            ana.spendPoints(points);
            return false;
        }
        catch (IllegalArgumentException e)
        {
            return true;
        }
    }

    /**
     * @return true if making a Player with this name throws
     *         IllegalArgumentException
     */
    private boolean rejects(String name)
    {
        try
        {
            new Player(name);
            return false;
        }
        catch (IllegalArgumentException e)
        {
            return true;
        }
    }
}
