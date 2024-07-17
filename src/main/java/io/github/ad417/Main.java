package io.github.ad417;

import java.util.LinkedList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        List<BoardState> boards = new LinkedList<>(List.of(new BoardState()));
        while (!boards.isEmpty()) {
            BoardState bs = boards.remove(0);
            if (!bs.isValid().value()) continue;
            if (bs.isComplete()) {
                boards.add(0, bs);
                break;
            }
            boards.addAll(bs.getSuccessors());
        }
        for (BoardState entry : boards) {
            if (!entry.isValid().value()) continue;
            System.out.println(entry);
            System.out.println();
        }
    }
}