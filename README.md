# Monte-Carlo-Tree-search-Domineering

An implementation of Monte Carlo Tree Search for Domineering (12x12) written for a competition organized by Johannes stöhr.

At the time of writing, it ranks first on HackerRank 8x8 board:https://www.hackerrank.com/challenges/domineering/leaderboard

* The default policy is UCT.
* for child selection, we choose the child that maximises (wins + 1)/(games + 2)
* for the rollout Policy, we use an evaluation function due to Johannes stöhr.

