/*
 * This class constructs the player object used for the Cave In game.
 * @author Joshua Molina
 * @version October 13, 2026
 */
public class Player 
{
    private String name = "";
    private int score = 0;

    /**
    * Constructor for the Player class.
    * @param name  the name of the player
    * @throws IllegalArgumentException if name is null or blank
    */
    public Player(String name) 
    {
        if (name == null || name.isBlank())
        {
            throw new IllegalArgumentException("Name cannot be null or empty.");
        }

        this.name = name.substring(0, Math.min(name.length(), 10)); // Cuts name if it extends past 10 characters.

    }

    /**
     * Getter method for the name of the player.
     * @return the player's name.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Getter method for the score of the player.
     * @return the player's score.
     */
    public int getScore()
    {
        return score;
    }
    
    /**
     * Adds the gained points to the player's score.
     * @param points  the points (gem count) to be added.
     * @throws IllegalArgumentException if points is negative
     */

    public void addPoints(int points)
    {
        if (points < 0)
        {
            throw new IllegalArgumentException("Points cannot be negative.");
        }

        score += points;
    }
}
