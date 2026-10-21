/*
 * This class constructs the player object used for the Cave In game.
 * @author Joshua Molina
 * @version October 13, 2026
 */
public class Player 
{
    private String name = "";
    private int score = 0;
    private LinkedChain<Die> ownDice = new LinkedChain<Die>(); // bought or cracked dice that belong to this player


    /*
    * Constructor for the Player class.
    * @param name  the name of the player
    */
    public Player(String name) 
    {
        if (name == null || name.isBlank())
        {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }

        this.name = name.substring(0, Math.min(name.length(), 10)); // Cuts name if it extends past 10 characters.

    }

    /*
     * Getter method for the name of the player.
     * @return the player's name.
     */
    public String getName()
    {
        return name;
    }

    /*
     * Getter method for the score of the player.
     * @return the player's score.
     */
    public int getScore()
    {
        return score;
    }
    
    /*
     * Adds the gained points to the player's score.
     * @param points  the points (gem count) to be added.
     */

    public void addPoints(int points)
    {
        if (points < 0)
        {
            throw new IllegalArgumentException("Points cannot be negative.");
        }

        score += points;
    }
    
    /**
     * Spends banked points, for example in the DiceShop.
     * @param points  the number of points to spend.
     * @throws IllegalArgumentException if points is negative or more
     *         than the player's score
     */
    public void spendPoints(int points)
    {
        if (points < 0 || points > score)
        {
            throw new IllegalArgumentException(
                "Cannot spend " + points + " points with a score of " + score + ".");
        }

        score -= points;
    }

    /**
     * Gives this player a die of their own (a gold die from the shop, or
     * a cracked die after a cave-in). It joins the cup on their turns.
     * @param die  the die to add.
     * @throws IllegalArgumentException if die is null
     */
    public void addOwnDie(Die die)
    {
        if (die == null)
        {
            throw new IllegalArgumentException("Die cannot be null.");
        }

        ownDice.add(die);
    }

    /**
     * Getter method for this player's own dice.
     * @return the LinkedChain holding the player's own dice.
     */
    public LinkedChain<Die> getOwnDice()
    {
        return ownDice;
    }

    /**
     * Counts this player's own dice of one color.
     * @param color  the color to count.
     * @return how many of the player's own dice have that color.
     */
    public int countOwnDice(Die.DieColor color)
    {
        int count = 0;
        for (Die die : ownDice.toArray(new Die[ownDice.size()]))
        {
            if (die.getColor() == color)
            {
                count++;
            }
        }
        return count;
    }
}
