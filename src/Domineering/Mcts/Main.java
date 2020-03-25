package Domineering.Mcts;

import java.io.IOException;

public class Main {
    private static final String NAME = "Ala Monte Carlo JS roll"; // FIXME: Change me!

    public static void main(String[] args) throws IOException {
        // code for the Domineering Competition
        Client c = new Client(new MctAgent(), NAME);
        new Thread(c).start();
    }
}