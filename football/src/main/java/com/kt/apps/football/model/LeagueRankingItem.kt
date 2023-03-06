package com.kt.apps.football.model

data class LeagueRankingItem(
    val rank: Int,
    val table: String,
    val team: FootballTeam,
    val score: Int,
    val totalPlayed: Int,
    val win: Int,
    val lose: Int,
    val draw: Int,
    val goalDifference: Int,
    val totalGoal: Int,
    val last5Match: Array<String>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LeagueRankingItem

        if (team != other.team) return false
        if (score != other.score) return false
        if (totalPlayed != other.totalPlayed) return false
        if (win != other.win) return false
        if (lose != other.lose) return false
        if (draw != other.draw) return false
        if (goalDifference != other.goalDifference) return false
        if (!last5Match.contentEquals(other.last5Match)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = team.hashCode()
        result = 31 * result + score
        result = 31 * result + totalPlayed
        result = 31 * result + win
        result = 31 * result + lose
        result = 31 * result + draw
        result = 31 * result + goalDifference
        result = 31 * result + last5Match.contentHashCode()
        return result
    }
}