package Domineering.Mcts;

public class MctAgent extends AI {
    final static int N = 12;
    final RolloutPolicy rp = new RolloutPolicy(N);

    @Override
    public Coordinate ai(char[][] board, Player player, String otherPlayer) {

        Coordinate ans = Mct.ai(board, player, rp);

        System.gc();
        return ans;
    }
}
