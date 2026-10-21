import java.util.Random;

/**
 * Test helper: a Random that returns a fixed, repeating list of values
 * from nextInt(bound), so tests can choose exactly which die is drawn and
 * which face is rolled.
 *
 * Reminder of the face layouts (index -> face):
 *   GREEN : 0-4 GEM, 5 FOOTPRINT
 *   YELLOW: 0-2 GEM, 3-4 FOOTPRINT, 5 ROCKFALL
 *   RED   : 0-2 GEM, 3-5 ROCKFALL
 *   GOLD  : 0-5 GEM
 *   CRACKED: 0 GEM, 1 FOOTPRINT, 2-5 ROCKFALL
 *
 * @author Sathvik Verma
 * @version October 14, 2026
 */
public class ScriptedRandom extends Random
{
    private static final long serialVersionUID = 1L;
    private int[] values;
    private int next;

    /**
     * @param values the values to hand out, in order, repeating forever
     */
    public ScriptedRandom(int... values)
    {
        this.values = values;
        this.next = 0;
    }

    @Override
    public int nextInt(int bound)
    {
        int value = values[next % values.length];
        next++;
        return value % bound;
    }
}
