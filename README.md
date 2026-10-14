# C2114-CaveIn
CS 2114 Project 2 Repository 

Instructions for Compiling and Running:
- If running in an IDE:
   > This application can be run on OS with a JDK installed. Make sure that CS 2114's CS2-Support student.jar is in the build path. You can pull the git repository down with $ git clone <HTTP-url>. The application can then be played with a simple run of the CaveInApp file. JUnit tests can be run via a Run As -> JUnit Test.
- If running on shell:
  > Make sure that CS 2114's CS2-Support student.jar is in the build path. Pull down the repository with compile with $ git clone <HTTP-url>, change directory into your local repository file, and compile via a javac *.java call. Running the application requires a run command of java CaveInApp within the shell, which is when the application will become runnable and ready for play. 


Reflection:
- What changed from our specifications?
  > While we tried to adhere to the specifications as faithfully as possible, we found ourselves needing to make minor tweaks to the method listing and definitions in order to achieve the vision of what this application is supposed to do. For example, while the specs list what methods are supposed to be found within the Game.java class, an addition method, endGame(), was included because the application had no way to influence the private variables of winner and gameOver. There were also changes with fleshing out the scope of our application. In our previous documentation, the color of the die didn't really indicate anything. Now, they specifically affect the probability of gaining either a GEM, FOOTPRINT, or ROCKFALL result, which is reflected in the layout of the enumerated Face class.
- What changed in the LinkedChain?
  > The LinkedChain.java class was modified slightly to include a randomRemove() method that would allow us to randomly remove Die objects from the LinkedChain created to hold them, a crucial functionality for the game to work.
- What happened to your unknowns?
  > Some of our unknowns listed in the project scope, such as the ability to randomly choose from a LinkedList, have been answered by researching and experiment with code and AI assistance to understand how to implement into our application. Others have yet to be answered but are on the work and will be fulfilled by the final presentation. 
- What doesn't work yet?
  > Throughout repeated testing, the MVP components of this application, such as the game functionality, bot opponent, and test cases, are all working. What is left to be included is the stretch goal integration. 

Next Steps:
- What is left to finish the MVP?
  > Our MVP is by this point accomplished. The players are able to be initialized and start a game, points (gems) are award or discarded based on the roll of their die, giving them the option to bank or keep on rolling. Rockfalls are implemented to result in a reset of points, and an end goal of 13 points are needed to end the game. With this base prototype of our application done, progress will shift towards stretch goal integration. 
- Which stretch goals will you attempt?
  > Our major stretch goal that we hope to achieve by the final presentation is the Dice Shop. This will allow for a fun twist in our game as players are able to, between rounds, upgrade and tactfully gear their dice loadout to give them a better shot at winning the game. A more minor stretch goal on the works as well is a GUI integration, which will make the application more appealing to look at, as well as allow for button interaction and handling, turning our application into a full-fledged game. It's important to note that a computer opponent and risk meter were both stretch goals, which have been integrated into this first prototype.
- What new tests will you need?
  > Method testing will definitely need to expand to handle the test cases of both DiceShop.java and CaveInAppGUI.java classes. This can included situation handling for user input on which dice they would want, button calling, and retrieval and appending into existing player hands. Methods tests like these will need to be able to check for correct input, as well as return IllegalArgumentExceptions for when an unexpected input is passed into them. 
- Who does what?
  > We have approached the first two deliverables with task allocation based on what is needed from which member in order to fill in required work. For the upcoming final presentation, each major aspect of the stretch goal integration will be divided evenly amongst the group members, with Sathvik on Dice Shop, Josh on GUI framework, and Matthew on the test cases for those two components. 

Attribution Table:

| Class              | Contributor(s)         | Process                                                                                                                                                    |
|--------------------|------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------|
| LinkedChain.java   | Josh (Edited)          | Edited to include a method needed for the application (AI suggestions on formatting).                                                                      |
| Game.java          | Josh                   | Code crafted by hand and then refined with use of AI suggestions/error catching.                                                                           |
| Die.java           | Josh                   | Code crafted by hand and then refined with use of AI suggestions/error catching.                                                                           |
| Player.java        | Josh                   | Code crafted by hand and then refined with use of AI suggestions/error catching.                                                                           |
| Turn.java          | Sathvik                | Implemented turn logic based on specs and Josh's Die/Game/Player classes; refined with AI assistance, integrated, and fully tested.                        |
| CaveInApp.java     | Sathvik                | Built main application flow and console interface from specs; Integrated components and play-tested in terminal                                            |
| Bot.java           | Sathvik                | Designed and implemented computer-opponent AI stretch goal with AI assistance; refined state logic and play-tested.                                        |
| ScriptedRandom.java| Sathvik                | Built deterministic mock RNG helper to ensure predictable dice rolls in automated tests.                                                                   |
| GameTest.java      | Matthew/Sathvik        | Designed through test cases for game calls on turn management and application lifetime.                                                                    |
| DieTest.java       | Matthew/Sathvik        | Tested and configured die creation with enumerated value capture and contraints with Face[] construction.                                                  |
| PlayerTest.java    | Matthew/Sathvik        | Tested player profile creation and return methods for player data.                                                                                         |
| TurnTest.java      | Sathvik                | Authored unit test suite from spec test plan; verified edge cases and test suite execution.                                                                |
| CaveInAppTest.java | Sathvik                | Developed integration tests based on spec test plan; validated app flow and test harness.                                                                  |
| BotTest.java       | Sathvik                | Created test scenarios for bot behavior; validated outcomes across                                                                                         |
|LinkedChainTest.java| Matthew/Sathvik        | Created test scenarios for bot behavior; validated outcomes across                                                                                         |
