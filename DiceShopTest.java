/**
 * Tests for DiceShop: one normal case and one bad-input case per public
 * method.
 *
 * @author Sathvik Verma
 * @version October 21, 2026
 */
public class DiceShopTest extends student.TestCase
{
    private DiceShop shop;
    private Player ana;

    /**
     * A shop and a player with 5 points before each test.
     */
    public void setUp()
    {
        shop = new DiceShop();
        ana = new Player("Ana");
        ana.addPoints(5);
    }

    // ----- getItems / Item -----

    /** The shop sells a gold die (4) and a repair (2). */
    public void testGetItems()
    {
        DiceShop.Item[] items = shop.getItems();
        assertEquals(2, items.length);
        assertEquals(DiceShop.Item.GOLD_DIE, items[0]);
        assertEquals(4, items[0].getPrice());
        assertEquals(2, DiceShop.Item.REPAIR.getPrice());
        assertTrue(items[0].getDescription().contains("Gold"));
    }

    // ----- canBuy -----

    /** Enough points: can buy gold; no cracked dice: can't repair. */
    public void testCanBuy()
    {
        assertTrue(shop.canBuy(ana, DiceShop.Item.GOLD_DIE));
        assertFalse(shop.canBuy(ana, DiceShop.Item.REPAIR));
        ana.addOwnDie(new Die(Die.DieColor.CRACKED));
        assertTrue(shop.canBuy(ana, DiceShop.Item.REPAIR));
    }

    /** Too few points, or a null player or item. */
    public void testCanBuyBadInput()
    {
        Player broke = new Player("Ben");
        assertFalse(shop.canBuy(broke, DiceShop.Item.GOLD_DIE));
        assertTrue(rejects(null, DiceShop.Item.GOLD_DIE));
        assertTrue(rejects(ana, null));
    }

    // ----- buy -----

    /** Buying gold spends 4 points and adds a gold die. */
    public void testBuyGoldDie()
    {
        assertTrue(shop.buy(ana, DiceShop.Item.GOLD_DIE));
        assertEquals(1, ana.getScore());
        assertEquals(1, ana.countOwnDice(Die.DieColor.GOLD));
    }

    /** A repair removes exactly one cracked die. */
    public void testBuyRepair()
    {
        ana.addOwnDie(new Die(Die.DieColor.CRACKED));
        ana.addOwnDie(new Die(Die.DieColor.CRACKED));
        assertTrue(shop.buy(ana, DiceShop.Item.REPAIR));
        assertEquals(3, ana.getScore());
        assertEquals(1, ana.countOwnDice(Die.DieColor.CRACKED));
    }

    /** Can't afford, or nothing to repair: false and nothing changes. */
    public void testBuyFailsWithoutChanges()
    {
        shop.buy(ana, DiceShop.Item.GOLD_DIE); // 1 point left
        assertFalse(shop.buy(ana, DiceShop.Item.GOLD_DIE));
        assertFalse(shop.buy(ana, DiceShop.Item.REPAIR));
        assertEquals(1, ana.getScore());
        assertEquals(1, ana.getOwnDice().size());
    }

    /** A null player is rejected. */
    public void testBuyNullPlayer()
    {
        Exception thrown = null;
        try
        {
            shop.buy(null, DiceShop.Item.GOLD_DIE);
        }
        catch (IllegalArgumentException e)
        {
            thrown = e;
        }
        assertNotNull(thrown);
    }

    /**
     * @return true if canBuy throws IllegalArgumentException
     */
    private boolean rejects(Player player, DiceShop.Item item)
    {
        try
        {
            shop.canBuy(player, item);
            return false;
        }
        catch (IllegalArgumentException e)
        {
            return true;
        }
    }
}
