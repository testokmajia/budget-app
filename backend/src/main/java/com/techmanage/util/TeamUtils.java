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
        for (Team t : allTeams) {
            if (t.getMembers() != null && !t.getMembers().isBlank()) {
                for (String name : t.getMembers().split(",")) {
                    if (name.trim().equals(userName)) {
                        return t.getName();
                    }
                }
            }
        }
        return null;
    }
}
