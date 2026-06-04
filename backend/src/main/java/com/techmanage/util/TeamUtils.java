package com.techmanage.util;

import com.techmanage.entity.Team;

import java.util.List;

/**
 * Shared team-related utility methods to avoid duplication across services.
 */
public final class TeamUtils {

    private TeamUtils() {
        // utility class
    }

    /**
     * Find the team name that a user belongs to by scanning team member lists.
     *
     * @param allTeams pre-loaded list of all teams (caller should load once before loops)
     * @param userName the user's display name
     * @return team name, or null if not found
     */
    public static String findTeamNameForUser(List<Team> allTeams, String userName) {
        if (userName == null || allTeams == null) return null;
        String trimmedName = userName.trim();
        for (Team t : allTeams) {
            // 检查组长
            if (t.getLeader() != null && trimmedName.equals(t.getLeader().trim())) {
                return t.getName();
            }
            // 检查成员列表
            if (t.getMembers() != null && !t.getMembers().isBlank()) {
                for (String name : t.getMembers().split(",")) {
                    if (trimmedName.equals(name.trim())) {
                        return t.getName();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Find ALL team names that a user belongs to (as leader or member).
     * A user can be the leader/member of multiple teams.
     *
     * @param allTeams pre-loaded list of all teams
     * @param userName the user's display name
     * @return list of team names (never null, may be empty)
     */
    public static java.util.List<String> findTeamNamesForUser(java.util.List<Team> allTeams, String userName) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (userName == null || allTeams == null) return result;
        String trimmedName = userName.trim();
        for (Team t : allTeams) {
            boolean found = false;
            if (t.getLeader() != null && trimmedName.equals(t.getLeader().trim())) {
                found = true;
            }
            if (!found && t.getMembers() != null && !t.getMembers().isBlank()) {
                for (String name : t.getMembers().split(",")) {
                    if (trimmedName.equals(name.trim())) {
                        found = true;
                        break;
                    }
                }
            }
            if (found) {
                result.add(t.getName());
            }
        }
        return result;
    }
}
