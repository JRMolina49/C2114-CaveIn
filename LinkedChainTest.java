import java.util.Random;

/**
 * Tests for the parts of LinkedChain our game relies on, focused on the
 * removeRandom() method we added, plus the empty-chain and
 * not-there cases our spec named.
 *
 * @author Sathvik Verma
 * @version October 14, 2026
 */
public class LinkedChainTest extends student.TestCase
{
    private LinkedChain<String> chain;

    /**
     * A chain holding a, b, c (first to last) before each test.
     */
    public void setUp()
    {
        chain = new LinkedChain<String>();
        chain.addToEnd("a");
        chain.addToEnd("b");
        chain.addToEnd("c");
    }

    // ----- removeRandom -----

    /** removeRandom removes the item at the index the generator picks. */
    public void testRemoveRandomPicksIndex()
    {
        assertEquals("b", chain.removeRandom(new ScriptedRandom(1)));
        assertEquals(2, chain.size());
        assertFalse(chain.contains("b"));
        assertEquals("a", chain.removeRandom(new ScriptedRandom(0)));
        assertEquals("c", chain.removeRandom(new ScriptedRandom(0)));
        assertTrue(chain.isEmpty());
    }

    /** removeRandom can remove the last node too. */
    public void testRemoveRandomLastItem()
    {
        assertEquals("c", chain.removeRandom(new ScriptedRandom(2)));
        assertEquals("[a, b]", chain.toString());
    }

    /** Removing randomly until empty returns every item exactly once. */
    public void testRemoveRandomEmptiesChain()
    {
        Random rng = new Random(7);
        String removed = "";
        while (!chain.isEmpty())
        {
            removed += chain.removeRandom(rng);
        }
        assertEquals(3, removed.length());
        assertTrue(removed.contains("a"));
        assertTrue(removed.contains("b"));
        assertTrue(removed.contains("c"));
    }

    /** removeRandom on an empty chain returns null. */
    public void testRemoveRandomEmpty()
    {
        LinkedChain<String> empty = new LinkedChain<String>();
        assertNull(empty.removeRandom(new Random(1)));
        assertEquals(0, empty.size());
    }

    // ----- other methods the game uses -----

    /** Removing something that isn't there returns false. */
    public void testRemoveMissingItem()
    {
        assertFalse(chain.remove("z"));
        assertEquals(3, chain.size());
        assertTrue(chain.remove("b"));
        assertEquals(2, chain.size());
    }

    /** remove() on an empty chain returns null. */
    public void testRemoveFirstOnEmpty()
    {
        assertNull(new LinkedChain<String>().remove());
    }

    /** count counts duplicates; absent items count 0. */
    public void testCount()
    {
        chain.add("a");
        assertEquals(2, chain.count("a"));
        assertEquals(0, chain.count("z"));
    }

    /** Adding null is rejected. */
    public void testAddNullRejected()
    {
        Exception thrown = null;
        try
        {
            chain.add(null);
        }
        catch (IllegalArgumentException e)
        {
            thrown = e;
        }
        assertNotNull(thrown);
        assertEquals(3, chain.size());
    }
}
