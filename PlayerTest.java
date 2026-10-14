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
