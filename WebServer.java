import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Executors;

/**
 * Serves the Cave-In browser interface and adapts its actions to the
 * existing Java game model.
 */
public class WebServer
{
    private static final int PORT = 8080;
    private final HttpServer server;
    private final Path webRoot;
    private final Object stateLock = new Object();
    private Game game;
    private Turn turn;
    private Random rng;
    private String[] playerColors = new String[0];
    private int round = 1;
    private int completedTurns;

    /**
     * Creates the local HTTP server.
     *
     * @param port local port to listen on
     * @param webRoot directory containing the static browser files
     * @throws IOException if the server cannot bind to the port
     */
    public WebServer(int port, Path webRoot) throws IOException
    {
        if (webRoot == null || !Files.isDirectory(webRoot))
        {
            throw new IllegalArgumentException(
                "The web directory does not exist: " + webRoot);
        }
        this.webRoot = webRoot.toAbsolutePath().normalize();
        this.server = HttpServer.create(
            new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api", this::handleApi);
        server.createContext("/", this::handleStatic);
        server.setExecutor(Executors.newCachedThreadPool());
    }

    /**
     * Starts serving the game.
     *
     * @param args not used
     * @throws IOException if the server cannot bind to its local port
     */
    public static void main(String[] args) throws IOException
    {
        Path webRoot = Paths.get("web").toAbsolutePath().normalize();
        WebServer server = new WebServer(PORT, webRoot);
        server.server.start();
        System.out.println("Cave-In is running at http://localhost:"
            + PORT + "/");
    }

    private void handleApi(HttpExchange exchange) throws IOException
    {
        try
        {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if ("GET".equals(method) && "/api/state".equals(path))
            {
                synchronized (stateLock)
                {
                    sendJson(exchange, 200, "{\"state\":"
                        + stateJson() + ",\"botEvents\":[]}");
                }
                return;
            }
            if (!"POST".equals(method))
            {
                sendJson(exchange, 405, errorJson("Method not allowed."));
                return;
            }

            Map<String, String> form = parseForm(exchange);
            synchronized (stateLock)
            {
                if ("/api/start".equals(path))
                {
                    startGame(form);
                    sendJson(exchange, 200, responseJson(null, null));
                }
                else if ("/api/begin-turn".equals(path))
                {
                    requireGame();
                    RollOutcome outcome = rollOnce();
                    sendJson(exchange, 200,
                        responseJson(outcome, playBotsUntilHuman()));
                }
                else if ("/api/action".equals(path))
                {
                    requireGame();
                    String action = form.get("action");
                    if ("dig".equals(action))
                    {
                        RollOutcome outcome = rollOnce();
                        sendJson(exchange, 200,
                            responseJson(outcome, playBotsUntilHuman()));
                    }
                    else if ("bank".equals(action))
                    {
                        bankCurrentTurn(exchange);
                    }
                    else
                    {
                        sendJson(exchange, 400,
                            errorJson("Action must be dig or bank."));
                    }
                }
                else
                {
                    sendJson(exchange, 404, errorJson("API route not found."));
                }
            }
        }
        catch (IllegalArgumentException e)
        {
            sendJson(exchange, 400, errorJson(e.getMessage()));
        }
        catch (IllegalStateException e)
        {
            sendJson(exchange, 409, errorJson(e.getMessage()));
        }
    }

    private void startGame(Map<String, String> form)
    {
        List<Player> players = new ArrayList<Player>();
        List<String> colors = new ArrayList<String>();
        List<String> usedColors = new ArrayList<String>();
        int humanNumber = 0;
        int botNumber = 0;

        if (!"red-human".equals(form.get("slot0")))
        {
            throw new IllegalArgumentException(
                "The first seat must be the red human player.");
        }

        for (int seat = 0; seat < 4; seat++)
        {
            String slot = form.get("slot" + seat);
            if (slot == null)
            {
                throw new IllegalArgumentException(
                    "All four player seats must be specified.");
            }
            String[] parts = slot.split("-", 2);
            if (parts.length != 2)
            {
                throw new IllegalArgumentException(
                    "Invalid player selection.");
            }
            String color = parts[0];
            String type = parts[1];
            if ("grey".equals(color) && "human".equals(type))
            {
                continue;
            }
            if (!isPlayableColor(color)
                || !("human".equals(type) || "bot".equals(type))
                || usedColors.contains(color))
            {
                throw new IllegalArgumentException(
                    "Player colors must be valid and unique.");
            }
            usedColors.add(color);
            colors.add(color);

            if ("human".equals(type))
            {
                String name = form.get("name" + humanNumber);
                if (name == null || name.trim().isEmpty())
                {
                    throw new IllegalArgumentException(
                        "Enter a name for each human player.");
                }
                players.add(new Player(name.trim()));
                humanNumber++;
            }
            else
            {
                botNumber++;
                players.add(new Bot("Bot " + botNumber));
            }
        }

        if (players.size() < 2 || players.size() > 4)
        {
            throw new IllegalArgumentException(
                "A game needs between two and four active players.");
        }
        if (humanNumber == 0)
        {
            throw new IllegalArgumentException(
                "At least one human player is required.");
        }

        game = new Game(players.toArray(new Player[players.size()]),
            CaveInApp.createCup());
        playerColors = colors.toArray(new String[colors.size()]);
        rng = new Random();
        round = 1;
        completedTurns = 0;
        turn = new Turn(game.getCurrentPlayer(), game.getCup(), rng);
    }

    private RollOutcome rollOnce()
    {
        if (turn == null || !turn.canDig())
        {
            throw new IllegalStateException(
                "There are no dice left to roll.");
        }
        turn.roll();
        Turn.RollResult[] roll = turn.getLatestRoll();
        RollOutcome outcome = new RollOutcome(
            roll, turn.isCavedIn(), turn.getGemCount());
        if (turn.isCavedIn())
        {
            finishTurn();
        }
        return outcome;
    }

    private void bankCurrentTurn(HttpExchange exchange) throws IOException
    {
        if (turn == null || !turn.canBank())
        {
            throw new IllegalStateException(
                "Roll at least once before banking.");
        }
        Player player = game.getCurrentPlayer();
        int banked = turn.bank();
        boolean won = player.getScore() >= CaveInApp.WINNING_SCORE;
        if (won)
        {
            game.endGame(player);
            finishTurnDiceOnly();
        }
        else
        {
            finishTurn();
        }
        List<BotEvent> events = won
            ? new ArrayList<BotEvent>() : playBotsUntilHuman();
        sendJson(exchange, 200, responseJson(
            new BankOutcome(player.getName(), banked, won), events));
    }

    private List<BotEvent> playBotsUntilHuman()
    {
        List<BotEvent> events = new ArrayList<BotEvent>();
        while (game != null && !game.isGameOver()
            && game.getCurrentPlayer() instanceof Bot)
        {
            Player player = game.getCurrentPlayer();
            Turn botTurn = new Turn(player, game.getCup(), rng);
            int collected;
            boolean cavedIn = false;
            int banked = 0;
            while (!botTurn.isOver())
            {
                String command = ((Bot)player).chooseCommand(
                    botTurn, game.getCup());
                if ("dig".equals(command))
                {
                    botTurn.roll();
                }
                else
                {
                    banked = botTurn.bank();
                    if (player.getScore() >= CaveInApp.WINNING_SCORE)
                    {
                        game.endGame(player);
                    }
                }
            }
            collected = botTurn.getGemCount();
            cavedIn = botTurn.isCavedIn();
            botTurn.returnDiceToCup();
            events.add(new BotEvent(
                player.getName(), playerColors[currentPlayerIndex()],
                round, collected, banked, cavedIn, player.getScore()));
            if (!game.isGameOver())
            {
                advanceToNextTurn();
            }
            else
            {
                turn = null;
            }
        }
        return events;
    }

    private void finishTurn()
    {
        if (turn != null)
        {
            turn.returnDiceToCup();
        }
        advanceToNextTurn();
    }

    private void finishTurnDiceOnly()
    {
        if (turn != null)
        {
            turn.returnDiceToCup();
            turn = null;
        }
    }

    private void advanceToNextTurn()
    {
        completedTurns++;
        if (completedTurns % game.getPlayers().length == 0)
        {
            round++;
        }
        game.nextTurn();
        turn = new Turn(game.getCurrentPlayer(), game.getCup(), rng);
    }

    private String stateJson()
    {
        if (game == null)
        {
            return "{\"started\":false}";
        }

        StringBuilder json = new StringBuilder();
        json.append("{\"started\":true,\"round\":").append(round);
        json.append(",\"gameOver\":").append(game.isGameOver());
        json.append(",\"currentPlayer\":");
        Player current = game.isGameOver() ? game.getWinner()
            : game.getCurrentPlayer();
        json.append(current == null ? "null" : quote(current.getName()));
        json.append(",\"players\":[");
        Player[] players = game.getPlayers();
        int currentIndex = currentPlayerIndex();
        for (int i = 0; i < players.length; i++)
        {
            if (i > 0)
            {
                json.append(',');
            }
            Player player = players[i];
            json.append("{\"name\":").append(quote(player.getName()));
            json.append(",\"score\":").append(player.getScore());
            json.append(",\"color\":").append(quote(playerColors[i]));
            json.append(",\"bot\":")
                .append(player instanceof Bot);
            json.append(",\"current\":")
                .append(!game.isGameOver() && i == currentIndex)
                .append('}');
        }
        json.append(']');
        json.append(",\"gemsInHand\":")
            .append(turn == null ? 0 : turn.getGemCount());
        json.append(",\"rockfalls\":")
            .append(turn == null ? 0 : turn.getRockfallCount());
        json.append(",\"riskPercent\":")
            .append(turn == null || !turn.canDig() ? 0
                : Math.round(Bot.caveInRisk(turn, game.getCup()) * 100));
        json.append(",\"cupCount\":").append(game.getCup().size());
        json.append(",\"canDig\":")
            .append(turn != null && turn.canDig());
        json.append(",\"canBank\":")
            .append(turn != null && turn.canBank());
        json.append(",\"latestRoll\":[");
        if (turn != null)
        {
            Turn.RollResult[] results = turn.getLatestRoll();
            for (int i = 0; i < results.length; i++)
            {
                if (i > 0)
                {
                    json.append(',');
                }
                json.append(rollResultJson(results[i]));
            }
        }
        json.append(']');
        json.append(",\"winner\":");
        Player winner = game.getWinner();
        json.append(winner == null ? "null" : quote(winner.getName()));
        json.append('}');
        return json.toString();
    }

    private String responseJson(RollOutcome outcome, List<BotEvent> events)
    {
        StringBuilder json = new StringBuilder("{\"state\":");
        json.append(stateJson()).append(",\"botEvents\":[");
        if (events != null)
        {
            for (int i = 0; i < events.size(); i++)
            {
                if (i > 0)
                {
                    json.append(',');
                }
                json.append(events.get(i).toJson());
            }
        }
        json.append(']');
        if (outcome != null)
        {
            json.append(",\"roll\":[");
            for (int i = 0; i < outcome.roll.length; i++)
            {
                if (i > 0)
                {
                    json.append(',');
                }
                json.append(rollResultJson(outcome.roll[i]));
            }
            json.append(']');
            json.append(",\"cavedIn\":").append(outcome.cavedIn);
            json.append(",\"gemsLost\":")
                .append(outcome.cavedIn ? outcome.gemsCollected : 0);
        }
        if (outcome instanceof BankOutcome)
        {
            BankOutcome bank = (BankOutcome)outcome;
            json.append(",\"banked\":").append(bank.banked);
            json.append(",\"bankedBy\":").append(quote(bank.playerName));
            json.append(",\"winner\":").append(bank.winner);
        }
        json.append('}');
        return json.toString();
    }

    private static String rollResultJson(Turn.RollResult result)
    {
        return "{\"color\":" + quote(result.getColor().name().toLowerCase())
            + ",\"face\":" + quote(result.getFace().name().toLowerCase())
            + "}";
    }

    private int currentPlayerIndex()
    {
        if (game == null || game.getCurrentPlayer() == null)
        {
            return -1;
        }
        Player[] players = game.getPlayers();
        for (int i = 0; i < players.length; i++)
        {
            if (players[i] == game.getCurrentPlayer())
            {
                return i;
            }
        }
        return -1;
    }

    private void requireGame()
    {
        if (game == null)
        {
            throw new IllegalStateException("Start a game first.");
        }
        if (game.isGameOver())
        {
            throw new IllegalStateException(
                "The game is over. Start a new game.");
        }
    }

    private static boolean isPlayableColor(String color)
    {
        return "red".equals(color) || "blue".equals(color)
            || "green".equals(color) || "yellow".equals(color);
    }

    private static Map<String, String> parseForm(HttpExchange exchange)
        throws IOException
    {
        String body = new String(
            exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> form = new HashMap<String, String>();
        if (body.isEmpty())
        {
            return form;
        }
        for (String pair : body.split("&"))
        {
            String[] parts = pair.split("=", 2);
            String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
            String value = parts.length == 2
                ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
            form.put(key, value);
        }
        return form;
    }

    private void handleStatic(HttpExchange exchange) throws IOException
    {
        if (!"GET".equals(exchange.getRequestMethod()))
        {
            sendText(exchange, 405, "Method not allowed.");
            return;
        }
        String requested = exchange.getRequestURI().getPath();
        if ("/".equals(requested))
        {
            requested = "/index.html";
        }
        Path file = webRoot.resolve(requested.substring(1)).normalize();
        if (!file.startsWith(webRoot))
        {
            sendText(exchange, 403, "Forbidden.");
            return;
        }
        if (!Files.isRegularFile(file))
        {
            sendText(exchange, 404, "File not found.");
            return;
        }
        byte[] contents = Files.readAllBytes(file);
        exchange.getResponseHeaders().set(
            "Content-Type", contentType(file));
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(200, contents.length);
        try (OutputStream output = exchange.getResponseBody())
        {
            output.write(contents);
        }
    }

    private static String contentType(Path file)
    {
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".html"))
        {
            return "text/html; charset=utf-8";
        }
        if (name.endsWith(".css"))
        {
            return "text/css; charset=utf-8";
        }
        if (name.endsWith(".js"))
        {
            return "application/javascript; charset=utf-8";
        }
        if (name.endsWith(".svg"))
        {
            return "image/svg+xml";
        }
        if (name.endsWith(".woff2"))
        {
            return "font/woff2";
        }
        if (name.endsWith(".ttf"))
        {
            return "font/ttf";
        }
        return "application/octet-stream";
    }

    private static String quote(String value)
    {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++)
        {
            char character = value.charAt(i);
            switch (character)
            {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (character < 0x20)
                    {
                        escaped.append(String.format("\\u%04x",
                            (int)character));
                    }
                    else
                    {
                        escaped.append(character);
                    }
                    break;
            }
        }
        return escaped.append('"').toString();
    }

    private static String errorJson(String message)
    {
        return "{\"error\":" + quote(
            message == null ? "Request failed." : message) + "}";
    }

    private static void sendJson(
        HttpExchange exchange, int status, String json) throws IOException
    {
        byte[] contents = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, contents.length);
        try (OutputStream output = exchange.getResponseBody())
        {
            output.write(contents);
        }
    }

    private static void sendText(
        HttpExchange exchange, int status, String text) throws IOException
    {
        byte[] contents = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
            "Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, contents.length);
        try (OutputStream output = exchange.getResponseBody())
        {
            output.write(contents);
        }
    }

    private static class RollOutcome
    {
        private final Turn.RollResult[] roll;
        private final boolean cavedIn;
        private final int gemsCollected;

        RollOutcome(
            Turn.RollResult[] roll, boolean cavedIn, int gemsCollected)
        {
            this.roll = roll;
            this.cavedIn = cavedIn;
            this.gemsCollected = gemsCollected;
        }
    }

    private static final class BankOutcome extends RollOutcome
    {
        private final String playerName;
        private final int banked;
        private final boolean winner;

        BankOutcome(String playerName, int banked, boolean winner)
        {
            super(new Turn.RollResult[0], false, 0);
            this.playerName = playerName;
            this.banked = banked;
            this.winner = winner;
        }
    }

    private static final class BotEvent
    {
        private final String name;
        private final String color;
        private final int round;
        private final int gemsCollected;
        private final int banked;
        private final boolean cavedIn;
        private final int score;

        BotEvent(
            String name, String color, int round, int gemsCollected,
            int banked, boolean cavedIn, int score)
        {
            this.name = name;
            this.color = color;
            this.round = round;
            this.gemsCollected = gemsCollected;
            this.banked = banked;
            this.cavedIn = cavedIn;
            this.score = score;
        }

        String toJson()
        {
            return "{\"name\":" + quote(name) + ",\"color\":"
                + quote(color) + ",\"round\":" + round
                + ",\"gemsCollected\":" + gemsCollected
                + ",\"banked\":" + banked
                + ",\"cavedIn\":" + cavedIn
                + ",\"score\":" + score + "}";
        }
    }
}
