# C2114-CaveIn
CS 2114 Project 2 Repository 

Instructions for Compiling and Running:
> 


Reflection:
- What changed from our specifications?
  > While we tried to adhere to the specifications as faithfully as possible, we found ourselves needing to make minor tweaks to the method listing and definitions in order to achieve the vision of what this application is supposed to do. For example, while the specs list what methods are supposed to be found within the Game.java class, an addition class, endGame(), was included because the application had no way to influence the private variables of winner and gameOver. There were also changes with fleshing out the scope of our application. In our previous documentation, the color of the die didn't really indicate anything. Now, they specifically affect the probability of gaining either a GEM, FOOTPRINT, or ROCKFALL result, which is reflected in the layout of the enumerated Face class.
- What changed in the LinkedChain?
  > The LinkedChain.java class was modified slightly to include a randomRemove() class that would allow us to randomly remove Die objects from the LinkedChain created to hold them, a crucial functionality for the game to work.
- What happened to your unknowns?
  > Some of our unknowns listed in the project scope, such as the ability to randomly choose from a LinkedList, have been answered by researching and experiment with code and AI assistance to understand how to implement into our application. Others have yet to be answered but are on the work and will be fulfilled by the final presentation. 
- What doesn't work yet?
  > TBD (Needs full code)

Next Steps:
- What is left to finish the MVP?
  > Our MVP is by this point accomplished. The players are able to be initialized and start a game, points (gems) are award or discarded based on the roll of their die, giving them the option to bank or keep on rolling. Rockfalls are implemented to result in a reset of points, and an end goal of 13 points are needed to end the game. With this base prototype of our application done, progress will shift towards stretch goal integration. 
- Which stretch goals will you attempt?
  > Our major stretch goal that we hope to achieve by the final presentation is the Dice Shop. This will allow for a fun twist in our game as players are able to, between rounds, upgrade and tactfully gear their dice loadout to give them a better shot at winning the game. A more minor stretch goal on the works as well is a GUI integration, which will make the application more appealing to look at, as well as allow for button interaction and handling, turning our application into a full-fledged game. 
- What new tests will you need?
  > Method testing will definitely need to expand to handle the test cases of both DiceShop.java and CaveInAppGUI.java classes. This can included situation handling for user input on which dice they would want, button calling, and retrieval and appending into existing player hands. Methods tests like these will need to be able to check for correct input, as well as return IllegalArgumentExceptions for when an unexpected input is passed into them. 
- Who does what?
  > We have approached the first two deliverables with task allocation based on what is needed from which member in order to fill in required work. While a simple allocation could be Josh on GUI integration, Sathvik on the Dice Shop, and Matthew on test cases, the actual task distribution may differ depending on who feels more comfortable doing what or the time resources available.  

Attribution Table:

| Class              | Contributor(s)         | Process                                                                                                      |
|--------------------|------------------------|--------------------------------------------------------------------------------------------------------------|
| LinkedList.java    | Josh (Edited)          | Edited to include a method needed for the application (AI suggestions on formatting).                        |
| Game.java          | Josh                   | Code crafted by hand and then refined with use of AI suggestions/error catching.                             |
| Die.java           | Josh                   | Code crafted by hand and then refined with use of AI suggestions/error catching.                             |
| Player.java        | Josh                   | Code crafted by hand and then refined with use of AI suggestions/error catching.                             |
| Turn.java          | Sathvik                |                                                                                                              |
| CaveInApp.java     | Sathvik                |                                                                                                              |
| GameTest.java      | Sathvik                |                                                                                                              |
| DieTest.java       | Mattew                 |                                                                                                              | 
| PlayerTest.java    | Matthew                |                                                                                                              |
| TurnTest.java      | Sathvik                |                                                                                                              |
| CaveInApp.java     | Matthew                |                                                                                                              |         
