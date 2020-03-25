package Domineering.Mcts;

import java.util.Scanner;

public class HackerRankMain {


    public static void main(String[] args) throws Exception {
        //code for HackerRank: https://www.hackerrank.com/challenges/domineering
        Scanner s = new Scanner(System.in);
        String player = s.next();
        int turn = player.equals("v") ? 0 : 1;
        final int N = 8;
        RolloutPolicy rp = new RolloutPolicy(N);

        boolean[][] board = new boolean[N][N];

        // the i-index first because that's the convention taken during the original competitions
        for (int j = 0; j < N; ++j) {
            String b = s.next();
            for (int i = 0; i < b.length(); ++i) {
                board[i][j] = b.charAt(i) != '-';
            }
        }
        int[] ans = Mct.answer(board, turn, rp);
        System.out.println(ans[1] + " " + ans[0]);

    }
}
