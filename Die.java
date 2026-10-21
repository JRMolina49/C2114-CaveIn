import java.util.Random;
/*
 * This class constructs the die object used for the Cave In game.
 * @author Joshua Molina
 * @version October 13, 2026
 */
public class Die 
{

    /*
     * Enumerated type that holds the potential colors of the die.
     */
    public enum DieColor 
    {
        GREEN, YELLOW, RED, GOLD, CRACKED;
    }

    /*
     * Enumerated type that holds the potential faces of the die.
     */
    public enum Face 
    {
        GEM, FOOTPRINT, ROCKFALL
    }
 
    private DieColor color;
    private Face[] faces;

    /*
     * Constructor for the Die class.
     * @param color  the color of the die 
     */
    public Die(DieColor color) 
    {
        if (color == null) // throws an exception if the color is unexpected.
        {
            throw new IllegalArgumentException(
                "Die color must not be null.");
        }

        this.color = color;
        
        switch (color) // dictates the faces of the die based on its color.
        {
            case GREEN:
                faces = new Face[] 
                {
                    Face.GEM, Face.GEM, Face.GEM, 
                    Face.GEM, Face.GEM, Face.FOOTPRINT
                };
                break;
            case YELLOW:
                faces = new Face[] 
                {
                    Face.GEM, Face.GEM, Face.GEM, 
                    Face.FOOTPRINT, Face.FOOTPRINT, Face.ROCKFALL
                };
                break;
            case RED:
                faces = new Face[] 
                {
                    Face.GEM, Face.GEM, Face.GEM, 
                    Face.ROCKFALL, Face.ROCKFALL, Face.ROCKFALL
                };
                break;        
            case GOLD: // bought in the DiceShop: every face is a gem
                faces = new Face[] 
                {
                    Face.GEM, Face.GEM, Face.GEM, 
                    Face.GEM, Face.GEM, Face.GEM
                };
                break;
            case CRACKED: // added after a cave-in: mostly rockfalls
                faces = new Face[] 
                {
                    Face.GEM, Face.FOOTPRINT, Face.ROCKFALL, 
                    Face.ROCKFALL, Face.ROCKFALL, Face.ROCKFALL
                };
                break;
        }
    }
    
    /*
     * Getter method for the color of the die.
     * @return the die's color.
     */
    public DieColor getColor() 
    {
        return color;
    }

    /*
     * Rolls the die, calling a random face from the constructed 
     * array of faces.
     * @param rng  the random number generator to use
     * @return the face that was rolled.
     */
    public Face roll(Random rng) 
    {
        return faces[rng.nextInt(faces.length)]; // calls a random number from 0 to the length of the array.
    }
}
