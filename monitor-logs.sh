#!/bin/bash
# Monitor LiL Ranker app logs in real-time
# Usage: ./monitor-logs.sh

echo "=== LiL Ranker App Log Monitor ==="
echo "Monitoring for: MatchEntry, Score Calculation, Leaderboard, Penalties"
echo "Press Ctrl+C to stop"
echo ""

adb logcat -c  # Clear existing logs
adb logcat | grep -E "MatchEntry|TournamentRepository|Leaderboard|buildMatchResults|applyPenalties" --color=always
