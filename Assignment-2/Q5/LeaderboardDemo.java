/*
 * Programming in Java  -  Assignment 2 (CO2)
 * Q5  -  Derive a Generic Ranking Component  ("Complete the Code")
 *
 * Name            : Vignesh S
 * Register Number : 192325002
 *
 * The given skeleton is unchanged apart from TODO 1 and TODO 2.
 */
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class Team {
    final String name; final int points; final int goalDiff;
    Team(String name, int points, int goalDiff) {
        this.name = name; this.points = points; this.goalDiff = goalDiff;
    }
    @Override public String toString() {
        return name + "(" + points + "," + goalDiff + ")";
    }
}

class Leaderboard<T> {
    private final List<T> items = new ArrayList<>();
    private final Comparator<? super T> order;

    Leaderboard(Comparator<? super T> order) { this.order = order; }

    void add(T item) { items.add(item); }

    List<T> ranking() {
        // TODO 1: Return a new sorted copy of items using order, without modifying
        //   items
        //
        // A new ArrayList is built from items, so the caller's repeated calls to
        // ranking() always see the same insertion order underneath; sorting the
        // field itself would make the component's behaviour depend on how many
        // times it had been read.
        List<T> copy = new ArrayList<>(items);
        copy.sort(order);
        return copy;
    }
}

public class LeaderboardDemo {
    public static void main(String[] args) {
        // TODO 2: Build the comparator: points (high first), then goal difference (high
        //   first), then name (A-Z)
        //
        // comparingInt is used rather than a subtraction lambda because
        // subtraction overflows for large values; reversed() is applied to each
        // numeric key separately so that only those two keys descend and the
        // name still ascends.
        Comparator<Team> byRank =
                Comparator.comparingInt((Team t) -> t.points).reversed()
                          .thenComparing(Comparator.comparingInt((Team t) -> t.goalDiff).reversed())
                          .thenComparing(t -> t.name);

        Leaderboard<Team> board = new Leaderboard<>(byRank);
        board.add(new Team("Panthers", 9, 4));
        board.add(new Team("Eagles", 9, 6));
        board.add(new Team("Cobras", 6, 1));
        board.add(new Team("Bisons", 9, 4));
        System.out.println(board.ranking());
    }
}
