/**
 * Tests for Die: one normal case and one bad-input case per public
 * method. ScriptedRandom picks the face index, so each color's six faces
 * can be checked one by one.
 *
 * @author Sathvik Verma
 * @version October 14, 2026
 */
public class DieTest extends student.TestCase
{
    /**
     * Rolls a die once for each face index 0-5 and counts each face.
     */
    private int[] countFaces(Die die)
    {
        int[] counts = new int[3]; // GEM, FOOTPRINT, ROCKFALL
        for (int i = 0; i < 6; i++)
        {
            counts[die.roll(new ScriptedRandom(i)).ordinal()]++;
        }
        return counts;
    }

    // ----- constructor / getColor -----

    /** A die keeps the color it was made with. */
    public void testConstructorAndGetColor()
    {
        assertEquals(Die.DieColor.GREEN, new Die(Die.DieColor.GREEN).getColor());
        assertEquals(Die.DieColor.YELLOW,
            new Die(Die.DieColor.YELLOW).getColor());
        assertEquals(Die.DieColor.RED, new Die(Die.DieColor.RED).getColor());
    }

    /** A null color is rejected. */
    public void testConstructorRejectsNull()
    {
        Exception thrown = null;
        try
        {
            new Die(null);
        }
        catch (IllegalArgumentException e)
        {
            thrown = e;
        }
        assertNotNull(thrown);
    }

    // ----- roll -----

    /** Green: 5 gems, 1 footprint, no rockfalls. */
    public void testRollGreenFaces()
    {
        int[] c = countFaces(new Die(Die.DieColor.GREEN));
        assertEquals(5, c[Die.Face.GEM.ordinal()]);
        assertEquals(1, c[Die.Face.FOOTPRINT.ordinal()]);
        assertEquals(0, c[Die.Face.ROCKFALL.ordinal()]);
    }

    /** Yellow: 3 gems, 2 footprints, 1 rockfall. */
    public void testRollYellowFaces()
    {
        int[] c = countFaces(new Die(Die.DieColor.YELLOW));
        assertEquals(3, c[Die.Face.GEM.ordinal()]);
        assertEquals(2, c[Die.Face.FOOTPRINT.ordinal()]);
        assertEquals(1, c[Die.Face.ROCKFALL.ordinal()]);
    }

    /** Red: 3 gems, 0 footprints, 3 rockfalls. */
    public void testRollRedFaces()
    {
        int[] c = countFaces(new Die(Die.DieColor.RED));
        assertEquals(3, c[Die.Face.GEM.ordinal()]);
        assertEquals(0, c[Die.Face.FOOTPRINT.ordinal()]);
        assertEquals(3, c[Die.Face.ROCKFALL.ordinal()]);
    }

    /** Rolling with no random generator fails instead of guessing. */
    public void testRollNullRandom()
    {
        Die die = new Die(Die.DieColor.GREEN);
        Exception thrown = null;
        try
        {
            die.roll(null);
        }
        catch (NullPointerException e)
        {
            thrown = e;
        }
        assertNotNull(thrown);
    }
}
