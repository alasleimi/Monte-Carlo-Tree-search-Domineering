package Domineering.Mcts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

public class Mct {
    static final Random rand = new Random();
    final Mct parent;
    // 0: Vertical
    // 1: Horizental
    final int nextToMove;

    // (x,y) convention
    final int[] action;
    final ArrayList<int[]> choices;
    final ArrayList<Mct> children;
    int expandedMoves = 0;
    int games = 0;
    int wins = 0;

    Mct(boolean[][] board, int player) {
        parent = null;
        action = null;
        nextToMove = player;
        choices = getMoves(board, player);
        children = new ArrayList<>();
    }

    Mct(Mct parent, int[] action, boolean[][] board) {

        nextToMove = 1 - parent.nextToMove;
        this.parent = parent;
        this.action = action;
        apply(action, parent.nextToMove, board);
        choices = parent.parent != null ?
                getMoves(parent.parent.choices, board, nextToMove)
                : getMoves(board, nextToMove);
        children = new ArrayList<>();

    }

    static void copy(boolean[][] src, boolean[][] dst) {
        for (int i = 0; i < src.length; i++) {
            System.arraycopy(src[i], 0, dst[i], 0, src[i].length);
        }
    }

    static int[] answer(boolean[][] b, int player, RolloutPolicy rPolicy) {
        long end = System.currentTimeMillis() + 270;

        Mct mct = new Mct(b, player);
        if (mct.choices.isEmpty())
            return null;
        boolean[][] cb = new boolean[b.length][b[0].length];
        for (; System.currentTimeMillis() < end; ) {

            copy(b, cb);
            Mct leaf = mct.select(cb);
            int winner = leaf.rollout(cb, rPolicy);
            leaf.backPropagate(winner);

        }
        //System.out.println(mct.games);
        return mct.bestChild();
    }

    static void apply(int[] move, int player, boolean[][] board) {

        board[move[0]][move[1]] = true;
        board[move[0] + player][move[1] + 1 - player] = true;
    }

    static public void printBoard(boolean[][] board) {
        int size = board.length;
        for (int i = 0; i < size; i++) {
            for (boolean[] booleans : board) {
                System.out.print(booleans[i] ? 'E' : '.');
            }
            System.out.println();
        }
    }

    static public void printBoard(char[][] board) {
        int size = board.length;
        for (int i = 0; i < size; i++) {
            for (char[] chars : board) {
                System.out.print(chars[i]);
            }
            System.out.println();
        }
    }

    static boolean[][] b(char[][] br) {
        boolean[][] ans = new boolean[br.length][br[0].length];
        for (int i = 0; i < br.length; i++) {
            for (int j = 0; j < br[i].length; j++) {
                ans[i][j] = br[i][j] != 'E';
            }
        }
        return ans;
    }

    static public Coordinate ai(char[][] board, Player player, RolloutPolicy rp) {
        int turn = Player.V == player ? 0 : 1;
        boolean[][] x = b(board);

        int[] u = answer(x, turn, rp);
        return new Coordinate(u[0], u[1]);
    }

    private ArrayList<int[]> getMoves(ArrayList<int[]> grandfather, boolean[][] board, int player) {
        ArrayList<int[]> ans = new ArrayList<>(grandfather.size());
        int ni = player;
        int njs = 1 - player;

        for (int[] x : grandfather) {

            if (!board[x[0]][x[1]] && !board[x[0] + ni][x[1] + njs]) {
                ans.add(x);
            }
        }
        return ans;

    }

    boolean fullyExpanded() {
        return choices.size() == children.size();
    }

    private ArrayList<int[]> getMoves(boolean[][] board, int player) {
        ArrayList<int[]> ans = new ArrayList<>();
        int ni = player;
        int njs = 1 - player;
        for (int i = 0; ni < board.length; ++ni, i++) {

            for (int nj = njs, j = 0; nj < board.length; ++nj, ++j) {

                if (!board[i][j] && !board[ni][nj]) {
                    ans.add(new int[]{i, j});
                }

            }

        }
        return ans;

    }

    private int[] bestChild() {
        return children.stream().max(Comparator.comparing(p -> (p.wins + 1) / (double) (p.games) + 2)).get().action;
    }

    void backPropagate(int winner) {

        if (winner != nextToMove) {
            ++wins;
        }

        ++games;
        if (parent != null)
            parent.backPropagate(winner);
    }

    Mct select(boolean[][] board) {
        Mct curr = this;
        for (; !curr.choices.isEmpty(); ) {
            if (!curr.fullyExpanded())
                return curr.expand(board);
            curr = curr.best();
            apply(curr.action, 1 - curr.nextToMove, board);
        }
        return curr;
    }

    private Mct expand(boolean[][] board) {
        int i = rand.nextInt(choices.size() - expandedMoves) + expandedMoves;

        //swap the choice to the front
        int[] tmp = choices.get(i);
        choices.set(i, choices.get(expandedMoves));
        choices.set(expandedMoves, tmp);
        ++expandedMoves;

        Mct ans = new Mct(this, tmp, board);
        children.add(ans);
        return ans;
    }

    int rollout(boolean[][] board, RolloutPolicy rPolicy) {

        return rPolicy.rollout(RolloutPolicy.transform(board), nextToMove);

    }

    @Override
    public String toString() {
        return "Mct{" +

                ", nextToMove=" + nextToMove +

                ", action=" + Arrays.toString(action) +
                ", games=" + games +
                ", wins=" + wins +
                '}';
    }

    Mct best() {

        double lN = Math.log(games);
        return children.stream()
                .max(Comparator
                        .comparing(p -> p.wins / (double) p.games + 1.41 * Math.sqrt(lN / p.games))).get();


    }
}
