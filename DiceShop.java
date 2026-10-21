/**
 * The dice shop (stretch goal from our scope). Between turns a player can
 * spend banked points on a gold die, which joins the cup on their own
 * turns, or pay to remove a cracked die they picked up from a cave-in.
 *
 * The shop holds no state of its own: a player's dice live in
 * Player.getOwnDice(), a LinkedChain&lt;Die&gt;. CaveInApp (console) and the
 * GUI both call the same methods here.
 *
 * @author Sathvik Verma
 * @version October 21, 2026
 */
public class DiceShop
{
    /**
     * What the shop sells, with its price in points.
     */
    public enum Item
    {
        /** A die whose six faces are all gems. */
        GOLD_DIE(4, "Gold die: all six faces are gems"),
        /** Removes one cracked die from the player's own dice. */
        REPAIR(2, "Repair: remove one cracked die");

        private final int price;
        private final String description;

        /**
         * @param price       cost in points
         * @param description text to show the player
         */
        Item(int price, String description)
        {
            this.price = price;
            this.description = description;
        }

        /**
         * @return cost in points
         */
        public int getPrice()
        {
            return price;
        }

        /**
         * @return text to show the player
         */
        public String getDescription()
        {
            return description;
        }
    }

    /**
     * @return every item the shop sells, in display order
     */
    public Item[] getItems()
    {
        return Item.values();
    }

    /**
     * Whether a player can buy an item right now.
     *
     * @param player the buyer
     * @param item   the item
     * @return true if the player has enough points, and for a repair,
     *         has at least one cracked die
     * @throws IllegalArgumentException if player or item is null
     */
    public boolean canBuy(Player player, Item item)
    {
        if (player == null || item == null)
        {
            throw new IllegalArgumentException(
                "The shop needs a player and an item.");
        }
        if (player.getScore() < item.getPrice())
        {
            return false;
        }
        return item != Item.REPAIR
            || player.countOwnDice(Die.DieColor.CRACKED) > 0;
    }

    /**
     * Buys an item: spends the points and changes the player's dice.
     *
     * @param player the buyer
     * @param item   the item
     * @return true if bought; false (nothing changes) if canBuy is false
     * @throws IllegalArgumentException if player or item is null
     */
    public boolean buy(Player player, Item item)
    {
        if (!canBuy(player, item))
        {
            return false;
        }
        player.spendPoints(item.getPrice());
        if (item == Item.GOLD_DIE)
        {
            player.addOwnDie(new Die(Die.DieColor.GOLD));
        }
        else
        {
            removeOneCracked(player.getOwnDice());
        }
        return true;
    }

    /**
     * Removes one cracked die from a chain of dice.
     *
     * @param dice the chain to remove from
     * @return true if a cracked die was removed
     */
    private boolean removeOneCracked(LinkedChain<Die> dice)
    {
        for (Die die : dice.toArray(new Die[dice.size()]))
        {
            if (die.getColor() == Die.DieColor.CRACKED)
            {
                return dice.remove(die);
            }
        }
        return false;
    }
}
