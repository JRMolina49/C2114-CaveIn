import java.util.Random;

/**
 * Runs one player's turn of Cave-In: draws dice from the cup, rolls them,
 * sets aside gems and rockfalls, and decides whether the player caved in
 * or banked.
 *
 * A Turn owns three LinkedChains of its own (hand, set-aside pile, face
 * tally) and borrows the shared cup from Game. Dice are only ever moved
 * between chains with remove/add, never copied, so the cup + hand +
 * set-aside pile always hold the same 13 dice.
 *
 * Validation that lives here: banking before rolling, rolling after the
 * turn is over, and rolling with no dice left all throw
 * IllegalStateException. CaveInApp checks canDig()/canBank() first, so a
 * player never sees these exceptions.
 *
 * @author Sathvik Verma
 * @version October 14, 2026
 */
public class Turn
{
    /**
     * The color and face produced by one die in the most recent roll.
     */
    public static final class RollResult
    {
        private final Die.DieColor color;
        private final Die.Face face;

        private RollResult(Die.DieColor color, Die.Face face)
        {
            this.color = color;
            this.face = face;
        }

        /**
         * @return the color of the die
         */
        public Die.DieColor getColor()
        {
            return color;
        }

        /**
         * @return the face rolled
         */
        public Die.Face getFace()
        {
            return face;
        }
    }

    /**
     * The stages a turn moves through.
     * READY -> ROLLED -> (ROLLED ...) -> CAVED_IN or BANKED
     */
    public enum TurnState
    {
        READY, ROLLED, CAVED_IN, BANKED
    }

    /** Dice drawn into the hand before each roll. */
    public static final int DICE_PER_ROLL = 3;

    /** Rockfalls that end the turn with 0 points. */
    public static final int ROCKFALLS_TO_CAVE_IN = 3;

    private Player player;
    private LinkedChain<Die> cup;
    private LinkedChain<Die> hand;
    private LinkedChain<Die> setAside;
    private LinkedChain<Die.Face> tally;
    private TurnState state;
    private Random rng;
    private RollResult[] latestRoll;

    /**
     * Starts a new turn in the READY state with an empty hand, set-aside
     * pile and tally.
     *
     * @param player the player taking this turn
     * @param cup    the shared cup of dice, owned by Game
     * @param rng    the random number generator used to draw and roll
     * @throws IllegalArgumentException if any argument is null
     */
    public Turn(Player player, LinkedChain<Die> cup, Random rng)
    {
        if (player == null || cup == null || rng == null)
        {
            throw new IllegalArgumentException(
                "Turn needs a player, a cup and a random generator.");
        }
        this.player = player;
        this.cup = cup;
        this.rng = rng;
        this.hand = new LinkedChain<Die>();
        this.setAside = new LinkedChain<Die>();
        this.tally = new LinkedChain<Die.Face>();
        this.state = TurnState.READY;
        this.latestRoll = new RollResult[0];
    }

    /**
     * Draws random dice from the cup until the hand holds 3 (or the cup
     * runs out), rolls every die in the hand, and sets aside each GEM or
     * ROCKFALL die. FOOTPRINT dice stay in the hand to be rolled again.
     * A third rockfall ends the turn in the CAVED_IN state.
     *
     * @return the faces rolled this time, one per die rolled
     * @throws IllegalStateException if canDig() is false (turn is over,
     *         or no dice are left in the hand or cup)
     */
    public Die.Face[] roll()
    {
        if (!canDig())
        {
            throw new IllegalStateException(isOver()
                ? "The turn is over; you cannot dig again."
                : "No dice left to roll; you must bank.");
        }

        // Draw back up to 3 dice, chosen at random from the cup.
        while (hand.size() < DICE_PER_ROLL && !cup.isEmpty())
        {
            hand.add(cup.removeRandom(rng));
        }

        // Roll every die in the hand. Footprints go back into the hand.
        int rolled = hand.size();
        Die.Face[] faces = new Die.Face[rolled];
        RollResult[] results = new RollResult[rolled];
        LinkedChain<Die> footprints = new LinkedChain<Die>();
        for (int i = 0; i < rolled; i++)
        {
            Die die = hand.remove();
            Die.Face face = die.roll(rng);
            faces[i] = face;
            results[i] = new RollResult(die.getColor(), face);
            if (face == Die.Face.FOOTPRINT)
            {
                footprints.add(die);
            }
            else
            {
                setAside.add(die);
                tally.add(face);
            }
        }
        while (!footprints.isEmpty())
        {
            hand.add(footprints.remove());
        }
        latestRoll = results;

        if (getRockfallCount() >= ROCKFALLS_TO_CAVE_IN)
        {
            state = TurnState.CAVED_IN;
        }
        else
        {
            state = TurnState.ROLLED;
        }
        return faces;
    }

    /**
     * Copies the color/face pairs from the most recent roll without
     * exposing the turn's internal snapshot.
     *
     * @return the most recent roll results, or an empty array before any
     *         roll
     */
    public RollResult[] getLatestRoll()
    {
        return latestRoll.clone();
    }

    /**
     * Whether the player may dig (roll) right now.
     *
     * @return true if the turn is not over and at least one die is in the
     *         hand or the cup
     */
    public boolean canDig()
    {
        return !isOver() && (!hand.isEmpty() || !cup.isEmpty());
    }

    /**
     * Whether the player may bank right now.
     *
     * @return true only after at least one roll and before the turn ends
     */
    public boolean canBank()
    {
        return state == TurnState.ROLLED;
    }

    /**
     * Ends the turn, adding this turn's gems to the player's score.
     *
     * @return the number of points banked
     * @throws IllegalStateException if canBank() is false (no roll yet,
     *         or the turn is already over)
     */
    public int bank()
    {
        if (!canBank())
        {
            throw new IllegalStateException(state == TurnState.READY
                ? "Roll at least once before banking."
                : "The turn is over; you cannot bank.");
        }
        int gems = getGemCount();
        player.addPoints(gems);
        state = TurnState.BANKED;
        return gems;
    }

    /**
     * Moves every die in the hand and set-aside pile back into the cup
     * and clears the face tally. Called once the turn ends.
     */
    public void returnDiceToCup()
    {
        while (!hand.isEmpty())
        {
            cup.add(hand.remove());
        }
        while (!setAside.isEmpty())
        {
            cup.add(setAside.remove());
        }
        tally.clear();
    }

    /**
     * @return gems collected so far this turn
     */
    public int getGemCount()
    {
        return tally.count(Die.Face.GEM);
    }

    /**
     * @return rockfalls collected so far this turn
     */
    public int getRockfallCount()
    {
        return tally.count(Die.Face.ROCKFALL);
    }

    /**
     * @return true once the player has caved in or banked
     */
    public boolean isOver()
    {
        return state == TurnState.CAVED_IN || state == TurnState.BANKED;
    }

    /**
     * @return true if this turn ended in a cave-in
     */
    public boolean isCavedIn()
    {
        return state == TurnState.CAVED_IN;
    }

    /**
     * @return the current state of the turn
     */
    public TurnState getState()
    {
        return state;
    }

    /**
     * @return the player taking this turn
     */
    public Player getPlayer()
    {
        return player;
    }

    /**
     * @return number of footprint dice waiting in the hand
     */
    public int getHandSize()
    {
        return hand.size();
    }

    /**
     * Copies the footprint dice waiting in the hand, so callers (the bot
     * and the risk meter) can see their colors without changing the hand.
     *
     * @return a new array of the dice in the hand; empty if none
     */
    public Die[] getHand()
    {
        return hand.toArray(new Die[hand.size()]);
    }

    /**
     * @return number of dice set aside (gems and rockfalls) this turn
     */
    public int getSetAsideSize()
    {
        return setAside.size();
    }

    /**
     * Total dice held by the cup, hand and set-aside pile. Used by tests
     * to prove no die is ever lost or copied (should always be 13).
     *
     * @return cup size + hand size + set-aside size
     */
    public int diceAccountedFor()
    {
        return cup.size() + hand.size() + setAside.size();
    }
}
