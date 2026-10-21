/**
 * Human players may visit the shop at their first dig/bank choice after
 * the automatic opening roll. A gold die joins the active turn before its
 * next dig, and a repair removes a cracked die picked up from a cave-in.
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
        return canBuy(player, item, null);
    }

    /**
     * Whether a player can buy an item, including a cracked die borrowed
     * into their active turn.
     *
     * @param player the buyer
     * @param item   the item
     * @param turn   the active turn, or null outside a turn
     * @return true if the player can afford the item and meet its
     *         requirements
     */
    public boolean canBuy(Player player, Item item, Turn turn)
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
        if (item != Item.REPAIR
            || player.countOwnDice(Die.DieColor.CRACKED) > 0)
        {
            return true;
        }
        if (turn == null)
        {
            return false;
        }
        if (turn.getPlayer() != player)
        {
            throw new IllegalArgumentException(
                "The active turn does not belong to this player.");
        }
        for (Die die : turn.getBorrowedDice())
        {
            if (die.getColor() == Die.DieColor.CRACKED)
            {
                return true;
            }
        }
        return false;
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
        return buy(player, item, null);
    }

    /**
     * Buys an item, allowing repair of a cracked die already borrowed by
     * the active turn.
     *
     * @param player the buyer
     * @param item   the item
     * @param turn   the active turn, or null outside a turn
     * @return true if bought; false if the item cannot be bought
     */
    public boolean buy(Player player, Item item, Turn turn)
    {
        if (!canBuy(player, item, turn))
        {
            return false;
        }
        boolean repairBorrowedDie = item == Item.REPAIR
            && player.countOwnDice(Die.DieColor.CRACKED) == 0;
        if (item == Item.REPAIR
            && repairBorrowedDie
            && (turn == null
                || !turn.discardBorrowedDie(Die.DieColor.CRACKED)))
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
            if (!repairBorrowedDie
                && !removeOneCracked(player.getOwnDice()))
            {
                throw new IllegalStateException(
                    "The cracked die selected for repair was not found.");
            }
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
