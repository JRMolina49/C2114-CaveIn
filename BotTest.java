import java.util.Random;

/**
 * Tests for Bot, the computer opponent: one normal case and one
 * bad-input case per public method.
 *
 * @author Sathvik Verma
 * @version October 14, 2026
 */
public class BotTest extends student.TestCase
{
    private Bot bot;

    /**
     * Fresh bot with the default risk limit before each test.
     */
    public void setUp()
    {
        bot = new Bot("Bot 1");
    }

    /**
     * Builds a cup holding the given number of dice of one color.
     */
    private LinkedChain<Die> cupOf(Die.DieColor color, int count)
    {
        LinkedChain<Die> cup = new LinkedChain<Die>();
        for (int i = 0; i < count; i++)
        {
            cup.add(new Die(color));
        }
        return cup;
    }

    // ----- constructors -----

    /** A bot is a Player with a name, score 0 and the default limit. */
    public void testConstructorDefaults()
    {
        assertEquals("Bot 1", bot.getName());
        assertEquals(0, bot.getScore());
        assertEquals(Bot.DEFAULT_RISK_LIMIT, bot.getRiskLimit(), 0.0001);
        assertEquals(0.5, new Bot("Bold", 0.5).getRiskLimit(), 0.0001);
    }

    /** Blank names and limits outside 0..1 are rejected. */
    public void testConstructorRejectsBadInput()
    {
        assertThrowsType(IllegalArgumentException.class,
            () -> new Bot("  "));
        assertThrowsType(IllegalArgumentException.class,
            () -> new Bot("Bot", -0.1));
        assertThrowsType(IllegalArgumentException.class,
            () -> new Bot("Bot", 1.5));
        assertThrowsType(IllegalArgumentException.class,
            () -> new Bot("Bot", Double.NaN));
    }

    // ----- rockfallChance -----

    /**
     * The table matches Die itself: roll each face index 0-5 of each
     * color and count rockfalls.
     */
    public void testRockfallChanceMatchesDie()
    {
        for (Die.DieColor color : Die.DieColor.values())
        {
            Die die = new Die(color);
            int rockfalls = 0;
            for (int face = 0; face < 6; face++)
            {
                if (die.roll(new ScriptedRandom(face)) == Die.Face.ROCKFALL)
                {
                    rockfalls++;
                }
            }
            assertEquals(rockfalls / 6.0, Bot.rockfallChance(color), 0.0001);
        }
    }

    /** A null color is rejected. */
    public void testRockfallChanceNull()
    {
        assertThrowsType(IllegalArgumentException.class,
            () -> Bot.rockfallChance(null));
    }

    // ----- caveInRisk -----

    /** Green dice can't roll rockfalls, so the risk is 0. */
    public void testCaveInRiskAllGreenIsZero()
    {
        LinkedChain<Die> cup = cupOf(Die.DieColor.GREEN, 6);
        Turn turn = new Turn(bot, cup, new ScriptedRandom(0));
        turn.roll();
        assertEquals(0.0, Bot.caveInRisk(turn, cup), 0.0001);
    }

    /**
     * One rockfall so far, red dice left: need 2+ rockfalls from 3 reds
     * at 1/2 each = 3/8 + 1/8 = 0.5.
     */
    public void testCaveInRiskRedDice()
    {
        LinkedChain<Die> cup = cupOf(Die.DieColor.RED, 6);
        // draws 0,0,0 then faces: GEM, ROCKFALL, GEM
        Turn turn = new Turn(bot, cup, new ScriptedRandom(0, 0, 0, 0, 3, 0));
        turn.roll();
        assertEquals(1, turn.getRockfallCount());
        assertEquals(0.5, Bot.caveInRisk(turn, cup), 0.0001);
    }

    /** Fewer dice left than rockfalls needed means no risk. */
    public void testCaveInRiskNotEnoughDice()
    {
        LinkedChain<Die> cup = cupOf(Die.DieColor.RED, 3);
        Turn turn = new Turn(bot, cup, new ScriptedRandom(0));
        turn.roll(); // 3 gems, cup now empty
        assertEquals(0.0, Bot.caveInRisk(turn, cup), 0.0001);
    }

    /** Null turn or cup is rejected. */
    public void testCaveInRiskNulls()
    {
        LinkedChain<Die> cup = CaveInApp.createCup();
        Turn turn = new Turn(bot, cup, new Random(1));
        assertThrowsType(IllegalArgumentException.class,
            () -> Bot.caveInRisk(null, cup));
        assertThrowsType(IllegalArgumentException.class,
            () -> Bot.caveInRisk(turn, null));
    }

    // ----- chooseCommand -----

    /** Before the first roll the bot must dig. */
    public void testChooseDigBeforeRoll()
    {
        LinkedChain<Die> cup = CaveInApp.createCup();
        Turn turn = new Turn(bot, cup, new Random(1));
        assertEquals("dig", bot.chooseCommand(turn, cup));
    }

    /** Low risk: keeps digging. */
    public void testChooseDigWhenSafe()
    {
        LinkedChain<Die> cup = cupOf(Die.DieColor.GREEN, 6);
        Turn turn = new Turn(bot, cup, new ScriptedRandom(0));
        turn.roll();
        assertEquals("dig", bot.chooseCommand(turn, cup));
    }

    /** High risk: banks. */
    public void testChooseBankWhenRisky()
    {
        LinkedChain<Die> cup = cupOf(Die.DieColor.RED, 6);
        Turn turn = new Turn(bot, cup, new ScriptedRandom(0, 0, 0, 0, 3, 0));
        turn.roll(); // 1 rockfall, 50% risk > 25% limit
        assertEquals("bank", bot.chooseCommand(turn, cup));
    }

    /** A bold bot with limit 1.0 digs even at 50% risk. */
    public void testChooseBoldBotDigs()
    {
        Bot bold = new Bot("Bold", 1.0);
        LinkedChain<Die> cup = cupOf(Die.DieColor.RED, 6);
        Turn turn = new Turn(bold, cup, new ScriptedRandom(0, 0, 0, 0, 3, 0));
        turn.roll();
        assertEquals("dig", bold.chooseCommand(turn, cup));
    }

    /** Banks when banking would win, even if digging is safe. */
    public void testChooseBankToWin()
    {
        bot.addPoints(11);
        LinkedChain<Die> cup = cupOf(Die.DieColor.GREEN, 6);
        Turn turn = new Turn(bot, cup, new ScriptedRandom(0));
        turn.roll(); // 3 gems -> 14
        assertEquals("bank", bot.chooseCommand(turn, cup));
    }

    /** Banks when there are no dice left to dig with. */
    public void testChooseBankWhenNoDiceLeft()
    {
        LinkedChain<Die> cup = cupOf(Die.DieColor.GREEN, 3);
        Turn turn = new Turn(bot, cup, new ScriptedRandom(0));
        turn.roll();
        assertEquals("bank", bot.chooseCommand(turn, cup));
    }

    /** Asking after a cave-in, or with nulls, is rejected. */
    public void testChooseCommandBadInput()
    {
        LinkedChain<Die> cup = cupOf(Die.DieColor.RED, 3);
        Turn turn = new Turn(bot, cup, new ScriptedRandom(0, 0, 0, 3, 3, 3));
        turn.roll(); // caved in
        assertThrowsType(IllegalStateException.class,
            () -> bot.chooseCommand(turn, cup));
        assertThrowsType(IllegalArgumentException.class,
            () -> bot.chooseCommand(null, cup));
        assertThrowsType(IllegalArgumentException.class,
            () -> bot.chooseCommand(turn, null));
    }

    /**
     * Checks that running the action throws the given exception type.
     *
     * @param type   the exception class expected
     * @param action the code that should throw
     */
    private void assertThrowsType(Class<? extends Exception> type,
        Runnable action)
    {
        Exception thrown = null;
        try
        {
            action.run();
        }
        catch (Exception e)
        {
            thrown = e;
        }
        assertNotNull("expected " + type.getSimpleName(), thrown);
        assertTrue("expected " + type.getSimpleName() + " but got "
            + thrown.getClass().getSimpleName(), type.isInstance(thrown));
    }
}
