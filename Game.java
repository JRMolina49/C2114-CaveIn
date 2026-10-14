
/*
 * This class constructs the game object used for the Cave In game.
 * @author Joshua Molina
 * @version October 13, 2026
 */
public class Game 
{
    private Player[] players;
    private LinkedChain<Die> cup;
    private int currentTurn;
    private Player winner;
    private boolean gameOver;

    /*
     * Constructs a new Game object with the players and dice cup.
     * @param players  array containing player names
     * @param cup  LinkedChain of die
     */
    public Game(Player[] players, LinkedChain<Die> cup) 
    {
        if (players == null || players.length == 0) 
        {
            throw new IllegalArgumentException("Invalid game setup");
        }

        if (cup == null || cup.isEmpty()) 
        {
            throw new IllegalArgumentException("Invalid game setup");
        }

        this.players = players;
        this.cup = cup;
        this.currentTurn = 0;
        this.winner = null;
        this.gameOver = false;
    }

    /* 
     * Getter method for the current player.
     * @return the player whose turn it is.
    */
    public Player getCurrentPlayer() 
    {
        return players[currentTurn];
    }

    /*
     * If the game is not over, calculates the next player's turn.
     */
    public void nextTurn() 
    {
        if(!gameOver)
        {
            currentTurn = (currentTurn + 1) % players.length; //Bounds the turn by the number of players.
        }

    }

    /*
     * Ends the game and sets the winner.
     * @param winner  the player who won the game.
     */
    public void endGame(Player winner) 
    {
        this.winner = winner;
        this.gameOver = true;
    }
    
    /*
     * Checks if the game is over.
     * @return true if endGame() has been called.
     */
    public boolean isGameOver() 
    {
        return gameOver;
    }

    /*
     * Getter method for winner.
     * @return the player who won the game.
     */
    public Player getWinner() 
    {
        return winner;
    }
    
    /*
     * Getter method for players.
     * @return the number of players in the game.
     */
    public Player[] getPlayers() 
    {
        return players;
    }

    /*
     * Getter method for the cup of dice.
     * @return the LinkedChain of die (aka. the cup).
     */
    public LinkedChain<Die> getCup() 
    {
        return cup;
    }
}
