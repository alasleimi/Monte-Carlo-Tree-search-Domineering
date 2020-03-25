package Domineering.Mcts;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/*
 * You have to implement an AI for Domineering(https://en.wikipedia.org/wiki/Domineering)
 *
 * Rules:
 * Domineering is played by two players on a rectangular board.
 * Our board is a square of the size 12 * 12.
 * One player places the domino pieces (size 1 by 2) vertically, the other one horizontally on the board.
 * The vertical player starts.
 * The first player that is unable to place a piece looses.
 * You also loose the game if you return a move that is not valid.
 * Time is another limitation, you only have about half a second for a move (more on that in the specifications)
 * Don't take too long, otherwise the current games count as lost!
 * You won't be asked to return a move if none is possible.
 * You may disconnect and reconnect at any time, but you may miss some games.
 *
 * How to implement the Client:
 * The communication with the server is already implemented.
 * You may change/improve that, but you may break something while doing so...
 * Right now the code expects you to implement the method ai() in AI, but you may change that or call other functions/classes.
 *
 * Before connecting the first time:
 * Change the two variables NAME (in Main) and IP_OF_JS (in Client).
 * Using the same names for two different AIs is not supported by the server, so use a unique (and creative?) name.
 * The name may not contain ":", "/" or ";" to make it easier for the clients (and the server).
 * It may also not exceed 30 characters, everything beyond that is cut away.
 *
 * Round Structure:
 * Every 5 minutes (if the last round has already finished) a new round starts and everybody plays new games.
 * You play against every other player two times per round. One time as vertical, the other time as the horizontal player.
 * The results are printed after each round, sorting the players by their overall points.
 * Additionally the points of the last round and of the last 10% of the rounds are printed. (Maybe someone is catching up...)
 *
 * Parallelization limitation:
 * To even the field a bit, you may only use a set number of threads for your ai (communication etc. may run in another).
 * It is determined by the maximum (boost) frequency of the CPU.
 * The number of threads multiplied by the boost frequency may not exceed 10!
 * stream().parallel() is allowed as long as you don't use it excessively.
 *
 * Some hints:
 * MinMax can help a lot if you got a good evaluation function.
 * AlphaBeta-Pruning can help, too (look it up, that are only 5 additional lines to MinMax)
 * Another possibility is letting your AI play with different settings against itself to improve it and choose the best settings.
 * You could analyze the communication a bit further and optimize for the behavior of the server...
 * The strategy pattern could help with different strategies, if you're into that
 * Jonas failed to implement efficient Monte-Carlo-Tree-Search in Haskell, so he would like to see one in Java
 * (to his defense: nobody had much success, as objects in Haskell are immutable.)
 *
 * I wouldn't suggest using "real" AI, since it will take to long to evolve and others may be ahead too far by then...
 * (if you want to do it anyway, you can ask Jonas for training data at the end of the first day)
 *
 * Some more facts:
 * It has been proven that the first player can always win on a board of the size of n x n up to the size 11.
 * Up to the size of 8 the game has been analyzed quite well. But for such big boards, you are on your own ;)
 *
 * For further reading you can look up: https://core.ac.uk/download/pdf/82783103.pdf
 *
 * Specifications for own clients and adaptations of this client:
 * All Messages are terminated by \n (Since I use readline \r\n should work too)
 * The communication starts with you printing your name
 * Then the server sends a "Welcome" or an error message
 * Before each round the server sends a "Ping" and expects an answer to measure the latency and adapt the timeout.
 * The average of all clients is used so delaying doesn't help here!
 * After that the Server sends move requests of the following form:
 * <PlayerMode>;<RoundID>;<GameID>;<NameOfOtherPlayer>;<GameBoard>
 * The PlayerMode is either a capital "H" or "V"
 * The board is printed starting from top left and iterating through all rows ("horizontally")
 * Every pair of two coordinates is encrypted by a number.
 * An empty Spot has the value 0, a piece of player V has 1 and a piece of player H 2.
 * The left spot of the pair gets its value multiplied by 3.
 * Multiple requests are separated by "/"
 * The response to that is in the form:
 * <RoundID>;<GameID>;<CoordinateOfMove>
 * Where the coordinate "(x, y)" is the top/left spot of the piece you want to play.
 * x represents the column (left/right) and y the row (up/down), both start from 0.
 * Multiple responses are also separated by "/"
 * For every move that is requested you get 0.5 seconds. But at least one second per communication.
 * So a request with 3 boards (and the other stuff) would grant you 1.5 seconds.
 * But watch out, the communication may take a while too!
 */

public class Client implements Runnable { // Only implements Runnable for testing multiple Clients, you may remove that
    private static final int GAMEPORT = 1337;
    private static final int RESULTPORT = 1338;
    private static final int SCOREPORT = 1339; // Just sends the score board without any input needed
    private static final String IP_OF_JS = "79.195.232.49"; // FIXME: Change me to the correct IP
    private final String NAME;
    private BufferedReader bR;
    private PrintWriter pW;
    private AI ai;

    public Client(AI ai, String name) throws IOException {
        NAME = name;
        connect();
        this.ai = ai;
    }

    /*
     * For getting the games that were played in the last round. Syntax for requesting:
     * 1) "NameOfPlayer", For all games of one player
     * 2) "NameOfPlayer, NameOfPlayer", For all games of these two players
     * 3) "boolean, NameOfPlayer", For every game that the player won (true) or lost (false)
     */
    private static void viewGames(String searched) {


        try (Socket requestor = new Socket(IP_OF_JS, RESULTPORT);
             BufferedReader bR = new BufferedReader(new InputStreamReader(requestor.getInputStream()));
             PrintWriter pW = new PrintWriter(requestor.getOutputStream(), true)) {
            pW.println("false, " + searched);
            for (int i = 0; i < 150; i++) {
                String s = bR.readLine();
                if (s != null)// For more games increase the upper bound
                    printGame(s);
                else break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void score() {


        try (Socket requestor = new Socket(IP_OF_JS, SCOREPORT);
             BufferedReader bR = new BufferedReader(new InputStreamReader(requestor.getInputStream()));
             PrintWriter pW = new PrintWriter(requestor.getOutputStream(), true)) {

            for (int i = 0; i < 100; i++) {
                String s = bR.readLine();
                if (s != null)// For more games increase the upper bound
                    System.out.println(s);
                else break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void printGame(String gameString) {
        String[] parts = gameString.split(", moves: ");
        System.out.println(parts[0]);
        if (parts.length > 1) {
            String[] coordsStrings = parts[1].split("\\)\\(|\\(|\\)"); // Get every tuple inside the coordinates without ()
            Coordinate[] coords = new Coordinate[coordsStrings.length];
            for (int i = 1; i < coordsStrings.length; i++) { // First entry is empty
                coords[i] = new Coordinate(coordsStrings[i]);
                System.out.println(coords[i]);
            }
        }

        // Print the game in a format you prefer
    }

    private void connect() throws IOException {
        Socket socket = new Socket(IP_OF_JS, GAMEPORT);
        bR = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        pW = new PrintWriter(socket.getOutputStream(), true);
        pW.println(NAME);
        String firstMessage = bR.readLine();
        if ("Welcome".equals(firstMessage)) {
            System.out.println("Connected");
        } else {
            System.out.println("Something went wrong...");
            System.out.println("Message: " + firstMessage);
            throw new IllegalStateException();
        }
    }

    public void run() {
        boolean reconnectedLately = false;

        while (true) {
            String line;


            try {

                line = bR.readLine();
                //System.out.println(line);
                if (line == null) {
                    connect();
                } else if ("Ping".equals(line)) {
                    pW.println("Pong");
                } else {
                    respond(line);
                }
                reconnectedLately = false;
            } catch (Exception e) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e2) {
                    System.out.println("Sleeping failed");
                }
                if (reconnectedLately) {
                    System.out.println("Conection failed twice in a row, shutting down");
                    return;
                } else {
                    System.out.println("Some error occured, trying to reconnect");
                    System.out.println(e);
                    reconnectedLately = true;
                }
                try {
                    connect();
                } catch (IOException e1) {
                    System.out.println("Reconnecting also failed shutting down");
                    return;
                }
            }
        }
    }

    private void respond(String line) {
        if (line == null) {
            System.out.println("null");
            return;

        }
        String[] requests = line.split("/");
        StringBuilder response = new StringBuilder();
        for (String request : requests) {
            String[] parts = request.split(";");
            Player pl;
            if (parts[0].equals("H")) {
                pl = Player.H;
            } else {
                pl = Player.V;
            }

            char[][] board = readBoard(parts[4]);

            Coordinate move = ai.ai(board, pl, parts[3]); // Calling your code

            if (response.toString().equals("")) {
                response.append(parts[1]).append(";").append(parts[2]).append(";").append(move);
            } else {
                response.append("/").append(parts[1]).append(";").append(parts[2]).append(";").append(move);
            }
        }

        pW.println(response);
    }

    /*
     * Returns the board as a two dimensional array. The first index is the
     * horizontal direction, the second is the vertical. The upper left corner is
     * (0, 0)
     */
    private char[][] readBoard(String line) {
        char[][] board = new char[12][12];

        for (int i = 0; i < line.length(); i++) {
            char currentChar = line.charAt(i);
            if ((currentChar - '0') % 3 == 0) {
                board[(i * 2 + 1) % 12][(i * 2 + 1) / 12] = 'E';
            } else if (currentChar % 3 == 1) {
                board[(i * 2 + 1) % 12][(i * 2 + 1) / 12] = 'V';
            } else {
                board[(i * 2 + 1) % 12][(i * 2 + 1) / 12] = 'H';
            }

            if ((currentChar - '0') / 3 == 0) {
                board[(i * 2) % 12][(i * 2) / 12] = 'E';
            } else if (currentChar / 3 == 1) {
                board[(i * 2) % 12][(i * 2) / 12] = 'V';
            } else {
                board[(i * 2) % 12][(i * 2) / 12] = 'H';
            }
        }

        return board;
    }


}