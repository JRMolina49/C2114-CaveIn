/**
 * A computer opponent for Cave-In (stretch goal from our scope).
 *
 * A Bot is a Player, so Game stores it in the same Player[] as humans.
 * Before each decision it checks the dice left in its hand and in the
 * cup, estimates the chance that digging again causes a cave-in, and
 * banks if that chance is above its risk limit.
 *
 * caveInRisk() is also used by CaveInApp to show human players a risk
 * meter before they choose.
 *
 * @author Sathvik Verma
 * @version October 14, 2026
 */
public class Bot extends Player
{
    /** Risk limit used when none is given: bank above a 25% chance. */
    public static final double DEFAULT_RISK_LIMIT = 0.25;

    /** Points needed to win; a bot banks as soon as banking would win. */
    public static final int WINNING_SCORE = 13;

    private double riskLimit;

    /**
     * Creates a bot with the default risk limit.
     *
     * @param name the bot's display name
     * @throws IllegalArgumentException if name is null or blank
     */
    public Bot(String name)
    {
        this(name, DEFAULT_RISK_LIMIT);
    }

    /**
     * Creates a bot that banks once the chance of a cave-in is above
     * riskLimit.
     *
     * @param name      the bot's display name
     * @param riskLimit a probability from 0.0 to 1.0
     * @throws IllegalArgumentException if name is blank or riskLimit is
     *         outside 0.0 to 1.0
     */
    public Bot(String name, double riskLimit)
    {
        super(name);
        if (riskLimit < 0.0 || riskLimit > 1.0 || Double.isNaN(riskLimit))
        {
            throw new IllegalArgumentException(
                "Risk limit must be between 0.0 and 1.0.");
        }
        this.riskLimit = riskLimit;
    }

    /**
     * @return the chance of cave-in above which this bot banks
     */
    public double getRiskLimit()
    {
        return riskLimit;
    }

    /**
     * Decides whether to dig or bank.
     * Rules, in order:
     *   1. Hasn't rolled yet -> dig (banking isn't allowed).
     *   2. No dice left to roll -> bank.
     *   3. Banking now would reach 13 -> bank.
     *   4. Cave-in risk above the risk limit -> bank, otherwise dig.
     *
     * @param turn the bot's turn in progress
     * @param cup  the shared cup, used to see which dice are left
     * @return "dig" or "bank"
     * @throws IllegalArgumentException if turn or cup is null
     * @throws IllegalStateException if the turn is already over
     */
    public String chooseCommand(Turn turn, LinkedChain<Die> cup)
    {
        if (turn == null || cup == null)
        {
            throw new IllegalArgumentException(
                "Bot needs a turn and a cup to decide.");
        }
        if (turn.isOver())
        {
            throw new IllegalStateException(
                "The turn is over; there is nothing to decide.");
        }
        if (!turn.canBank())
        {
            return "dig";
        }
        if (!turn.canDig())
        {
            return "bank";
        }
        if (getScore() + turn.getGemCount() >= WINNING_SCORE)
        {
            return "bank";
        }
        return caveInRisk(turn, cup) > riskLimit ? "bank" : "dig";
    }

    /**
     * Estimates the chance that the next dig ends the turn in a cave-in.
     *
     * The dice rolled next are the footprint dice already in the hand
     * (colors known) plus enough dice drawn from the cup to make 3. Each
     * drawn die is treated as having the cup's average rockfall chance.
     * The method then adds up the chance of rolling at least as many
     * rockfalls as are still needed to reach 3.
     *
     * @param turn the turn in progress
     * @param cup  the cup the next dice will be drawn from
     * @return a probability from 0.0 to 1.0
     * @throws IllegalArgumentException if turn or cup is null
     */
    public static double caveInRisk(Turn turn, LinkedChain<Die> cup)
    {
        if (turn == null || cup == null)
        {
            throw new IllegalArgumentException(
                "Risk needs a turn and a cup.");
        }
        int needed = Turn.ROCKFALLS_TO_CAVE_IN - turn.getRockfallCount();
        if (needed <= 0)
        {
            return 1.0;
        }

        Die[] hand = turn.getHand();
        int draws = Math.min(Turn.DICE_PER_ROLL - hand.length, cup.size());
        int diceToRoll = hand.length + draws;
        if (diceToRoll < needed)
        {
            return 0.0;
        }

        double cupAverage = 0.0;
        if (!cup.isEmpty())
        {
            Die[] inCup = cup.toArray(new Die[cup.size()]);
            for (Die die : inCup)
            {
                cupAverage += rockfallChance(die.getColor());
            }
            cupAverage /= inCup.length;
        }

        // chances[k] = probability of exactly k rockfalls so far
        double[] chances = new double[diceToRoll + 1];
        chances[0] = 1.0;
        int rolled = 0;
        for (Die die : hand)
        {
            addDie(chances, rolled, rockfallChance(die.getColor()));
            rolled++;
        }
        for (int i = 0; i < draws; i++)
        {
            addDie(chances, rolled, cupAverage);
            rolled++;
        }

        double risk = 0.0;
        for (int k = needed; k <= diceToRoll; k++)
        {
            risk += chances[k];
        }
        return Math.min(1.0, risk);
    }

    /**
     * Chance that one die of the given color rolls a ROCKFALL. Matches
     * the face layouts in Die: green 0 of 6, yellow 1 of 6, red 3 of 6,
     * gold 0 of 6 and cracked 4 of 6.
     * (BotTest checks these numbers against Die itself.)
     *
     * @param color the die color
     * @return the rockfall chance for that color
     */
    public static double rockfallChance(Die.DieColor color)
    {
        if (color == null)
        {
            throw new IllegalArgumentException("Color cannot be null.");
        }
        switch (color)
        {
            case YELLOW:
                return 1.0 / 6;
            case RED:
                return 3.0 / 6;
            case CRACKED:
                return 4.0 / 6;
            default:
                return 0.0;
        }
    }

    /**
     * Updates the rockfall-count chances after rolling one more die.
     *
     * @param chances chance of each rockfall count so far
     * @param rolled  how many dice have been added already
     * @param p       this die's chance of a rockfall
     */
    private static void addDie(double[] chances, int rolled, double p)
    {
        for (int k = rolled + 1; k >= 1; k--)
        {
            chances[k] = chances[k] * (1 - p) + chances[k - 1] * p;
        }
        chances[0] = chances[0] * (1 - p);
    }
}
