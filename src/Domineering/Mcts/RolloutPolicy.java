package Domineering.Mcts;

import java.util.ArrayList;
import java.util.Random;

public class RolloutPolicy {
    static final Random rand = new Random();
    public final short[] count;
    final ArrayList<Coordinate> goodMoves = new ArrayList<>();
    final int N;

    RolloutPolicy(int N) {

        this.N = N;
        count = new short[1 << N];
        for (int i = 0; i < count.length; i++) {
            count[i] = (short) count(i);
        }

    }


    // counts the number of non-overlapping moves in a line
    static int count(int line) {

        int count = 0;

        while (line != 0) {
            int lsb = line & -line;
            line &= ~(lsb | (lsb << 1));
            ++count;

        }

        return count;
    }

    static int countSafeH(char[] board) {

        int k = 0;
        int old = 0;


        for (int i = 0; i < board.length - 1; ++i) {
            int x = ~board[i] & ~board[i + 1];
            int v = (board[i] & board[i + 1]) & (x >>> 1) & ((x << 1) + 1);
            old = v & ~old;
            k += Integer.bitCount(old);
        }

        return k;

    }

    public static char[] transform(boolean[][] board) {
        char[] ans = new char[board.length];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (!board[i][j]) ans[i] |= 1 << j;
            }
        }
        return ans;
    }

    static void set(char[] ans, int i, int j) {
        ans[i] |= (1 << j);
    }

    static void unset(char[] ans, int i, int j) {
        ans[i] &= ~(1 << j);
    }

    static boolean check(char[] ans, int i, int j) {
        return (ans[i] & (1 << j)) != 0;
    }

    public static int countHoles(char[] board) {
        int N = board.length;
        int k = Integer.bitCount(board[0] & ~(board[0] >>> 1) & ~(board[0] << 1) & ~board[1]);
        k += Integer.bitCount(board[N - 1] & ~(board[N - 1] >>> 1) & ~(board[N - 1] << 1) & ~board[N - 2]);
        for (int i = 1; i < N - 1; ++i) {

            int line = board[i] & ~(board[i] >>> 1) & ~(board[i] << 1);
            k += Integer.bitCount(~board[i - 1] & line & ~board[i + 1]);

        }
        return k;
    }

    static int countMovesH(char[] ans) {
        int j = 0;
        int old = 0;
        for (int i = 0; i < ans.length - 1; i++) {
            j += Integer.bitCount(old = ans[i] & ans[i + 1] & ~old);
        }
        return j;
    }

    int countSafeV(char[] board) {

        int v = board[0] & (board[0] >>> 1) & (~board[1]) & ((~board[1]) >>> 1);

        int sum = count[v];
        v = board[N - 1] & (board[N - 1] >>> 1) & (~board[N - 2]) & ((~board[N - 2]) >>> 1);
        sum += count[v];
        for (int i = 1; i < N - 1; ++i) {
            v = board[i] & (board[i] >>> 1) & (~board[i + 1]) & ((~board[i + 1]) >>> 1)
                    & (~board[i - 1]) & ((~board[i - 1]) >>> 1);

            sum += count[v];


        }
        return sum;


    }

    private Coordinate horizentalPlayer(char[] board, ArrayList<Coordinate> goodMoves) {
        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < N - 1; ++i) {
            int play = board[i] & board[i + 1];
            for (int j = 0; j < N; ++j) {
                if ((play & (1 << j)) != 0) {
                    unset(board, i, j);
                    unset(board, i + 1, j);
                    int curr = evaluateMove(board, Player.H);
                    if (curr >= bestScore) {
                        if (curr > bestScore) {
                            bestScore = curr;
                            goodMoves.clear();
                        }
                        goodMoves.add(new Coordinate(i, j));
                    }
                    set(board, i, j);
                    set(board, i + 1, j);

                }

            }


        }
        if (goodMoves.isEmpty())
            return new Coordinate(-1, -1);


        return goodMoves.get(rand.nextInt(goodMoves.size()));
    }

    public Coordinate verticalPlayer(char[] board, ArrayList<Coordinate> goodMoves) {

        int bestScore = Integer.MIN_VALUE;
        for (int i = 0; i < N; ++i) {
            int play = (board[i] & (board[i] >>> 1));
            for (int j = 0; j < N - 1; ++j) {
                if ((play & (1 << j)) != 0) {
                    unset(board, i, j);
                    unset(board, i, j + 1);
                    int curr = evaluateMove(board, Player.V);
                    if (curr >= bestScore) {
                        if (curr > bestScore) {
                            bestScore = curr;
                            goodMoves.clear();
                        }
                        goodMoves.add(new Coordinate(i, j));
                    }
                    set(board, i, j);
                    set(board, i, j + 1);

                }

            }

        }
        if (goodMoves.isEmpty())
            return new Coordinate(-1, -1);


        return goodMoves.get(rand.nextInt(goodMoves.size()));

    }

    public int evaluateMove(char[] board, Player player) {
        int turn = player == Player.H ? 1 : -1;
        int ans = (countMovesH(board) - countMovesV(board))
                + (countSafeH(board) - countSafeV(board));

        int holes = countHoles(board);

        int parity = ((holes + 1) / 2) % 2;
        int holeScore = 0;

        if (turn == 1 && parity == 0)
            holeScore = 10;
        else if (turn == -1 && parity == 1) {
            holeScore = 9;
        }


        return turn * 11 * ans + holeScore;
    }

    int countMovesV(char[] ans) {
        int j = 0;
        for (int i = 0; i < ans.length; i++) {
            j += count[ans[i] & (ans[i] << 1)];
        }
        return j;

    }

    void print(char[] c) {
        boolean[][] n = new boolean[N][N];
        for (int i = 0; i < N; ++i)
            for (int j = 0; j < N; ++j) {
                n[i][j] = (c[i] & (1 << j)) != 0;
            }
        Mct.printBoard(n);
    }

    int rollout(char[] board, int nextToMove) {


        for (; ; ) {

            Coordinate u = nextToMove == 0 ?
                    verticalPlayer(board, goodMoves)
                    : horizentalPlayer(board, goodMoves);

            if (u.getY() == -1)
                break;

            goodMoves.clear();
            apply(board, u, nextToMove);


            nextToMove = 1 - nextToMove;


        }

        return 1 - nextToMove;

    }

    void apply(char[] board, Coordinate u, int nextToMove) {
        int i = u.getX();
        int j = u.getY();

        unset(board, i, j);
        unset(board, i + nextToMove, j + 1 - nextToMove);

    }
}
