import java.io.PrintStream;
import java.util.Random;
import java.util.Scanner;

/**
 * Console front end for Cave-In. This is the only class that reads input
 * or prints. It validates everything the players type and re-prompts on
 * bad input, so the game never crashes on a typo.
 *
 * Bad input handled here (from our scope):
 *   - player count that is not a number from 2 to 4  -> re-prompt
 *   - blank name                                      -> re-prompt
 *   - anything other than "dig" or "bank"             -> re-prompt
 *   - "bank" before the first roll                    -> re-prompt
 *   - "dig" when no dice are left                     -> re-prompt
 *   - computer-player count outside 0 to player count -> re-prompt
 * A cave-in ends the turn automatically, so the player is never asked to
 * dig after one.
 *
 * Stretch goals: computer players (Bot) take their own turns, and
 * human players see a cave-in risk meter before each choice.
 *
 * Input and output are passed in so tests can feed a Scanner built from
 * a String and read what was printed.
 *
 * @author Sathvik Verma
 * @version October 14, 2026
 */
public class CaveInApp
{
    /** Points needed to win. */
    public static final int WINNING_SCORE = 13;

    /** Fewest players allowed. */
    public static final int MIN_PLAYERS = 2;

    /** Most players allowed. */
    public static final int MAX_PLAYERS = 4;

    private Scanner in;
    private PrintStream out;
    private Random rng;
    private Game game;

    /**
     * Creates an app that reads from the given Scanner and prints to the
     * given stream.
     *
     * @param in  where player input comes from
     * @param out where the board is printed
     * @param rng random generator for drawing and rolling dice
     * @throws IllegalArgumentException if any argument is null
     */
    public CaveInApp(Scanner in, PrintStream out, Random rng)
    {
        if (in == null || out == null || rng == null)
        {
            throw new IllegalArgumentException(
                "CaveInApp needs input, output and a random generator.");
        }
        this.in = in;
        this.out = out;
        this.rng = rng;
    }

    /**
     * Starts a game in the console.
     *
     * @param args not used
     */
    public static void main(String[] args)
    {
        CaveInApp app = new CaveInApp(new Scanner(System.in), System.out,
            new Random());
        try
        {
            app.run();
        }
        catch (IllegalStateException e)
        {
            // Input ended (for example Ctrl+D) before the game finished.
            System.out.println("\nGame ended early. Goodbye!");
        }
    }

    /**
     * Builds the starting cup: 6 green, 4 yellow and 3 red dice.
     *
     * @return a new cup holding 13 dice
     */
    public static LinkedChain<Die> createCup()
    {
        LinkedChain<Die> cup = new LinkedChain<Die>();
        for (int i = 0; i < 6; i++)
        {
            cup.add(new Die(Die.DieColor.GREEN));
        }
        for (int i = 0; i < 4; i++)
        {
            cup.add(new Die(Die.DieColor.YELLOW));
        }
        for (int i = 0; i < 3; i++)
        {
            cup.add(new Die(Die.DieColor.RED));
        }
        return cup;
    }

    /**
     * Plays a full game: set up, then turns until someone reaches 13.
     */
    public void run()
    {
        out.println("=== CAVE-IN ===");
        out.println("Dig for gems. Three rockfalls and you lose the turn.");
        out.println("First to " + WINNING_SCORE + " points wins.\n");
        setUpGame();
        while (!game.isGameOver())
        {
            playTurn();
        }
        out.println("\n*** " + game.getWinner().getName() + " wins with "
            + game.getWinner().getScore() + " points! ***");
    }

    /**
     * Asks for the player count, how many are computer players, and the
     * human players' names, then creates the Game with a full cup.
     * Humans come first in the order entered, then bots ("Bot 1", ...).
     * Players are collected in a LinkedChain<Player>.
     *
     * @return the new game
     */
    public Game setUpGame()
    {
        int count = readPlayerCount();
        int bots = readBotCount(count);
        LinkedChain<Player> joined = new LinkedChain<Player>();
        for (int i = 1; i <= count - bots; i++)
        {
            joined.addToEnd(new Player(readName(i)));
        }
        for (int i = 1; i <= bots; i++)
        {
            joined.addToEnd(new Bot("Bot " + i));
        }
        Player[] players = joined.toArray(new Player[joined.size()]);
        game = new Game(players, createCup());
        return game;
    }

    /**
     * Plays one turn for the current player, then passes play on.
     * Ends the game if the player banks their way to 13 or more.
     */
    public void playTurn()
    {
        Player player = game.getCurrentPlayer();
        Turn turn = new Turn(player, game.getCup(), rng);
        out.println("\n--- " + player.getName() + "'s turn ---");

        while (!turn.isOver())
        {
            String command;
            if (player instanceof Bot)
            {
                command = ((Bot)player).chooseCommand(turn, game.getCup());
                out.println(player.getName() + " chooses to " + command
                    + ".");
            }
            else
            {
                if (turn.canBank() && turn.canDig())
                {
                    printRisk(turn);
                }
                command = readCommand(turn);
            }
            if (command.equals("dig"))
            {
                Die.Face[] faces = turn.roll();
                printRoll(faces);
                printStatus(turn);
                if (turn.isCavedIn())
                {
                    out.println("CAVE-IN! " + player.getName()
                        + " loses this turn's gems.");
                }
            }
            else
            {
                int points = turn.bank();
                out.println(player.getName() + " banks " + points
                    + " point(s).");
                if (player.getScore() >= WINNING_SCORE)
                {
                    game.endGame(player);
                }
            }
        }

        turn.returnDiceToCup();
        game.nextTurn();
    }

    /**
     * Reads the number of players, asking again until it is a whole
     * number from 2 to 4.
     *
     * @return the number of players
     */
    public int readPlayerCount()
    {
        while (true)
        {
            String line = prompt("How many players (2-4)? ");
            try
            {
                int count = Integer.parseInt(line);
                if (count >= MIN_PLAYERS && count <= MAX_PLAYERS)
                {
                    return count;
                }
            }
            catch (NumberFormatException e)
            {
                // not a number; fall through to the error message
            }
            out.println("Enter a number from 2 to 4.");
        }
    }

    /**
     * Reads how many of the players are computer players, asking again
     * until it is a whole number from 0 to the player count.
     *
     * @param playerCount the total number of players
     * @return the number of computer players
     */
    public int readBotCount(int playerCount)
    {
        while (true)
        {
            String line = prompt("How many of them are computer players (0-"
                + playerCount + ")? ");
            try
            {
                int bots = Integer.parseInt(line);
                if (bots >= 0 && bots <= playerCount)
                {
                    return bots;
                }
            }
            catch (NumberFormatException e)
            {
                // not a number; fall through to the error message
            }
            out.println("Enter a number from 0 to " + playerCount + ".");
        }
    }

    /**
     * Reads one player's name, asking again until it is not blank.
     *
     * @param playerNumber which player is being asked (1-based)
     * @return the trimmed name
     */
    public String readName(int playerNumber)
    {
        while (true)
        {
            String name = prompt("Name for player " + playerNumber + ": ");
            if (!name.isEmpty())
            {
                return name;
            }
            out.println("Name can't be blank.");
        }
    }

    /**
     * Reads "dig" or "bank" (any case), asking again on anything else or
     * on a move the turn does not allow right now.
     *
     * @param turn the turn in progress, used to check what is legal
     * @return "dig" or "bank", lower case
     */
    public String readCommand(Turn turn)
    {
        while (true)
        {
            String command = prompt("dig or bank? ").toLowerCase();
            if (command.equals("dig"))
            {
                if (turn.canDig())
                {
                    return command;
                }
                out.println("No dice left - you must bank.");
            }
            else if (command.equals("bank"))
            {
                if (turn.canBank())
                {
                    return command;
                }
                out.println("Roll at least once before banking.");
            }
            else
            {
                out.println("Type dig or bank.");
            }
        }
    }

    /**
     * Prints the faces from one roll.
     *
     * @param faces the faces rolled
     */
    public void printRoll(Die.Face[] faces)
    {
        StringBuilder sb = new StringBuilder("Rolled:");
        for (Die.Face face : faces)
        {
            sb.append(" ").append(face);
        }
        out.println(sb.toString());
    }

    /**
     * Prints this turn's gems, rockfalls and dice left, then every
     * player's banked score.
     *
     * @param turn the turn in progress
     */
    public void printStatus(Turn turn)
    {
        out.println("Gems: " + turn.getGemCount() + "  Rockfalls: "
            + turn.getRockfallCount() + "/" + Turn.ROCKFALLS_TO_CAVE_IN
            + "  Footprints in hand: " + turn.getHandSize()
            + "  Dice in cup: " + game.getCup().size());
        StringBuilder sb = new StringBuilder("Scores:");
        for (Player p : game.getPlayers())
        {
            sb.append("  ").append(p.getName()).append(" ")
                .append(p.getScore());
        }
        out.println(sb.toString());
    }

    /**
     * Prints the risk meter: the chance that digging again causes a
     * cave-in, as a percentage and a 10-block bar.
     *
     * @param turn the turn in progress
     */
    public void printRisk(Turn turn)
    {
        double risk = Bot.caveInRisk(turn, game.getCup());
        int percent = (int)Math.round(risk * 100);
        int blocks = (int)Math.round(risk * 10);
        StringBuilder bar = new StringBuilder("[");
        for (int i = 0; i < 10; i++)
        {
            bar.append(i < blocks ? '#' : '-');
        }
        bar.append("]");
        out.println("Cave-in risk if you dig: " + bar + " " + percent + "%");
    }

    /**
     * @return the game being played, or null before setUpGame()
     */
    public Game getGame()
    {
        return game;
    }

    /**
     * Prints a prompt and reads one trimmed line.
     *
     * @param message the prompt to show
     * @return the line typed, trimmed
     * @throws IllegalStateException if input has ended
     */
    private String prompt(String message)
    {
        out.print(message);
        if (!in.hasNextLine())
        {
            throw new IllegalStateException("No more input.");
        }
        return in.nextLine().trim();
    }
}
