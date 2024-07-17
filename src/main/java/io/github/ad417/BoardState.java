package io.github.ad417;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BoardState {
    /** The number of rows in the board. */
    private static final int ROWS = 5;
    /** The number of columns in the board. */
    private static final int COLS = 5;
    private static final char MARKED = 'X';
    private static final char UNMARKED = ' ';
    private static final char UNKNOWN = '#';

    /**
     * A 2D representation of the board state.
     * Nominally, this would be a boolean matrix, but I wouldn't be able to
     * represent "unfilled" otherwise. Characters make printing easier. <br>
     * Access via <c>grid[col][row]</c>.
     */
    private char[][] grid;
    /**
     * The current row position of the "cursor". The cursor fills in tiles
     * sequentially as part of the DFS/Backtracking algorithm.
     */
    private final int cursorRow;
    /**
     * The current column position of the "cursor".
     */
    private final int cursorCol;

    public BoardState() {
        grid = new char[COLS][ROWS];
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                grid[row][col] = UNKNOWN;
            }
        }
        cursorCol = 0;
        cursorRow = 0;
    }

    private BoardState(BoardState prev, boolean fillNext) {
        this.grid = Arrays.stream(prev.grid)
                .map(char[]::clone)
                .toArray(char[][]::new);

        this.grid[prev.cursorCol][prev.cursorRow] = fillNext ? 'X' : ' ';
        if (prev.cursorCol == COLS - 1) {
            cursorCol = 0;
            cursorRow = prev.cursorRow + 1;
        } else {
            cursorCol = prev.cursorCol + 1;
            cursorRow = prev.cursorRow;
        }
    }

    /**
     * Determine if this (incomplete?) configuration is valid according to all
     * of its tiles' rules.
     * Specifically, a board is valid if all marked tiles are TRUE, and all
     * unmarked tiles are FALSE. Some tiles can't be evaluated immediately, but
     * as long as nothing is violated, then the board is valid.
     * @return True if a board is valid (or could be valid if incomplete);
     * False if a board cannot be valid, and Unknown if the board's validity
     * cannot be conclusively determined.
     */
    public Validator isValid() {
        Validator result = Validator.TRUE;
        Validator tileEntry;
        // Oh boy
        // A1
        tileEntry = diagonalBingo(true).invert().matches(tileValue('A', 1));
        result = result.and(tileEntry);
        // B1
        tileEntry = isInBingo('B', 1).invert().matches(tileValue('B', 1));
        result = result.and(tileEntry);
        // C1
        tileEntry = diagonalBingo(false).matches(tileValue('C', 1));
        result = result.and(tileEntry);
        // D1
        tileEntry = tileValue('D', 4).matches(tileValue('D', 1));
        result = result.and(tileEntry);
        // E1
        tileEntry = isInBingo('E', 1).matches(tileValue('E', 1));
        result = result.and(tileEntry);

        // A2
        tileEntry = tileValue('A', 4).invert().matches(tileValue('A', 2));
        result = result.and(tileEntry);
        // B2
        tileEntry = rowBingoCountMatches(x -> x > 0)
                .and(colBingoCountMatches(x -> x > 0))
                .and(diagonalBingo(false).or(diagonalBingo(true)))
                .matches(tileValue('B', 2));
        result = result.and(tileEntry);
        // C2
        tileEntry = Validator.TRUE.matches(tileValue('C', 2));
        result = result.and(tileEntry);

        // D2
        tileEntry = filledSpaceCountMatches(x -> x < 17);
        result = result.and(tileEntry);
        // E2
        tileEntry = spacesInBingoCountMatches(x -> x % 2 == 0);
        result = result.and(tileEntry);


        // A3
        tileEntry = isInBingo('A', 3).matches(tileValue('A', 3));
        result = result.and(tileEntry);
        // B3
        tileEntry = filledNotInBingoCountMatches(x -> x > 5).matches(tileValue('B', 3));
        result = result.and(tileEntry);
        // C3
        tileEntry = tileValue('C', 3).invert()
                .or(isInBingo('C', 3))
                .matches(tileValue('C', 3));
        result = result.and(tileEntry);
        // D3
        tileEntry = colBingoCountMatches(x -> x > 2).matches(tileValue('D', 3));
        result = result.and(tileEntry);
        // E3
        tileEntry = spacesInBingoCountMatches(x -> (25 - x) > 10);
        result = result.and(tileEntry);

        // A4
        tileEntry = tileValue('A', 2).invert().matches(tileValue('A', 4));
        result = result.and(tileEntry);
        // B4
        tileEntry = rowBingo(1).or(colBingo(3)).matches(tileValue('B', 4));
        result = result.and(tileEntry);
        // C4
        tileEntry = colCountMatches(2, x -> x < 3).matches(tileValue('C', 4));
        result = result.and(tileEntry);
        // D4
        tileEntry = tileValue('D', 1).matches(tileValue('D', 4));
        result = result.and(tileEntry);
        // E4
        tileEntry = diagonalBingo(true).or(diagonalBingo(false))
                .matches(tileValue('E', 4));
        result = result.and(tileEntry);

        // A5
        tileEntry = tileValue('E', 5).matches(tileValue('A', 5));
        result = result.and(tileEntry);
        // B5
        // Unsure how to deal with this situation. So, I'm gonna ignore it.
        tileEntry = Validator.FALSE.matches(tileValue('B', 5));
        result = result.and(tileEntry);
        // C5
        // Arbitrary.
        // D5
        tileEntry = bingoCountMatches(x -> x > 3).matches(tileValue('D', 5));
        result = result.and(tileEntry);
        // E5
        tileEntry = tileValue('A', 5).matches(tileValue('E', 5));
        result = result.and(tileEntry);




        return result;
    }

    public boolean isComplete() {
        return cursorRow == ROWS;
    }

    /**
     * Get the board states that come from filling in the next tile.
     * This does not consider the validity of any such boards.
     * @return the boards obtained by either marking or not marking the next
     * tile.
     */
    public List<BoardState> getSuccessors() {
        if (isComplete()) return List.of(this);
        List<BoardState> states = new LinkedList<>();
        states.add(new BoardState(this, true));
        states.add(new BoardState(this, false));
        return states;
    }

    // AND DOTH BEGINS THE LOGIC!
    public Validator tileValue(char col, int row) {
        if (col >= 'a') col -= 'a';
        if (col >= 'A') col -= 'A';
        return tileValue((int)col, row-1);
    }

    public Validator tileValue(int col, int row) {
        return switch (grid[col][row]) {
            case MARKED -> Validator.TRUE;
            case UNMARKED -> Validator.FALSE;
            default -> Validator.UNKNOWN;
        };
    }

    public Validator isInBingo(char col, int row) {
        if (col >= 'a') col -= 'a';
        if (col >= 'A') col -= 'A';
        return isInBingo((int)col, row-1);
    }

    public Validator isInBingo(int col, int row) {
        Validator result = Validator.FALSE;
        result = result.or(rowBingo(row));
        result = result.or(colBingo(col));
        // Check diagonals.
        if (row == col) result = result.or(diagonalBingo(false));
        if (row == ROWS - 1 - col) result = result.or(diagonalBingo(true));

        return result;
    }

    public Validator rowBingo(int row) {
        Validator result = Validator.TRUE;
        for (int col = 0; col < COLS; col++) {
            result = result.and(tileValue(col, row));
        }
        return result;
    }
    public Validator colBingo(int col) {
        Validator result = Validator.TRUE;
        for (int row = 0; row < ROWS; row++) {
            result = result.and(tileValue(col, row));
        }
        return result;
    }

    public Validator diagonalBingo(boolean upRight) {
        Function<Integer, Integer> diagMapper = (row) -> row;
        if (upRight) diagMapper = (row) -> ROWS - 1 - row;

        Validator result = Validator.TRUE;
        for (int row = 0; row < ROWS; row++) {
            int col = diagMapper.apply(row);
            result = result.and(tileValue(col, row));
        }
        return result;
    }

    private Set<Integer> possibleSpacesFilled() {
        int low = 0;
        int high = 1;
        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < ROWS; row++) {
                Validator filled = tileValue(col, row);
                if (filled.value()) high++;
                if (filled.equals(Validator.TRUE)) low++;
            }
        }
        return IntStream.range(low, high).boxed().collect(Collectors.toSet());
    }

    private Set<Integer> possibleSpacesInBingo() {
        int low = 0;
        int high = 1;
        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < ROWS; row++) {
                Validator filled = isInBingo(col, row);
                if (filled.value()) high++;
                if (filled.equals(Validator.TRUE)) low++;
            }
        }
        return IntStream.range(low, high).boxed().collect(Collectors.toSet());
    }

    private Set<Integer> possibleFilledNotInBingo() {
        int low = 0;
        int high = 1;
        for (int col = 0; col < COLS; col++) {
            for (int row = 0; row < ROWS; row++) {
                Validator filled = isInBingo(col, row).invert().and(tileValue(col, row));
                if (filled.value()) high++;
                if (filled.equals(Validator.TRUE)) low++;
            }
        }
        return IntStream.range(low, high).boxed().collect(Collectors.toSet());
    }

    private Set<Integer> possibleFilledInRow(int row) {
        int low = 0;
        int high = 1;
        for (int col = 0; col < COLS; col++) {
            Validator filled = tileValue(col, row);
            if (filled.value()) high++;
            if (filled.equals(Validator.TRUE)) low++;
        }
        return IntStream.range(low, high).boxed().collect(Collectors.toSet());
    }

    private Set<Integer> possibleFilledInCol(int col) {
        int low = 0;
        int high = 1;
        for (int row = 0; row < COLS; row++) {
            Validator filled = tileValue(col, row);
            if (filled.value()) high++;
            if (filled.equals(Validator.TRUE)) low++;
        }
        return IntStream.range(low, high).boxed().collect(Collectors.toSet());
    }

    private Set<Integer> possibleRowBingoes() {
        int low = 0;
        int high = 1;
        for (int row = 0; row < ROWS; row++) {
            Validator bingo = rowBingo(row);
            if (bingo.value()) high++;
            if (bingo.equals(Validator.TRUE)) low++;
        }
        return IntStream.range(low, high).boxed().collect(Collectors.toSet());
    }

    private Set<Integer> possibleColBingoes() {
        int low = 0;
        int high = 1;
        for (int col = 0; col < COLS; col++) {
            Validator bingo = colBingo(col);
            if (bingo.value()) high++;
            if (bingo.equals(Validator.TRUE)) low++;
        }
        return IntStream.range(low, high).boxed().collect(Collectors.toSet());
    }

    public Set<Integer> possibleBingoes() {
        int low = 0;
        int high = 1;
        for (int col = 0; col < COLS; col++) {
            Validator bingo = colBingo(col);
            if (bingo.value()) high++;
            if (bingo.equals(Validator.TRUE)) low++;
        }
        for (int row = 0; row < ROWS; row++) {
            Validator bingo = rowBingo(row);
            if (bingo.value()) high++;
            if (bingo.equals(Validator.TRUE)) low++;
        }
        Validator bingo = diagonalBingo(false);
        if (bingo.value()) high++;
        if (bingo.equals(Validator.TRUE)) low++;
        bingo = diagonalBingo(true);
        if (bingo.value()) high++;
        if (bingo.equals(Validator.TRUE)) low++;
        return IntStream.range(low, high).boxed().collect(Collectors.toSet());
    }

    public Validator colCountMatches(int col, Predicate<Integer> matcher) {
        Set<Integer> possibleInCol = possibleFilledInCol(col);
        if (possibleInCol.stream().allMatch(matcher)) return Validator.TRUE;
        if (possibleInCol.stream().noneMatch(matcher)) return Validator.FALSE;
        return Validator.UNKNOWN;
    }

    public Validator bingoCountMatches(Predicate<Integer> matcher) {
        Set<Integer> possibleBingoes = possibleBingoes();
        if (possibleBingoes.stream().allMatch(matcher)) return Validator.TRUE;
        if (possibleBingoes.stream().noneMatch(matcher)) return Validator.FALSE;
        return Validator.UNKNOWN;
    }

    public Validator rowBingoCountMatches(Predicate<Integer> matcher) {
        Set<Integer> possibleRows = possibleRowBingoes();
        if (possibleRows.stream().allMatch(matcher)) return Validator.TRUE;
        if (possibleRows.stream().noneMatch(matcher)) return Validator.FALSE;
        return Validator.UNKNOWN;
    }

    public Validator colBingoCountMatches(Predicate<Integer> matcher) {
        Set<Integer> possibleCols = possibleColBingoes();
        if (possibleCols.stream().allMatch(matcher)) return Validator.TRUE;
        if (possibleCols.stream().noneMatch(matcher)) return Validator.FALSE;
        return Validator.UNKNOWN;
    }

    public Validator filledSpaceCountMatches(Predicate<Integer> matcher) {
        Set<Integer> possibleFilled = possibleSpacesFilled();
        if (possibleFilled.stream().allMatch(matcher)) return Validator.TRUE;
        if (possibleFilled.stream().noneMatch(matcher)) return Validator.FALSE;
        return Validator.UNKNOWN;
    }

    public Validator spacesInBingoCountMatches(Predicate<Integer> matcher) {
        Set<Integer> possibleInBingo = possibleSpacesInBingo();
        if (possibleInBingo.stream().allMatch(matcher)) return Validator.TRUE;
        if (possibleInBingo.stream().noneMatch(matcher)) return Validator.FALSE;
        return Validator.UNKNOWN;

    }

    public Validator filledNotInBingoCountMatches(Predicate<Integer> matcher) {
        Set<Integer> possibleNotInBingo = possibleFilledNotInBingo();
        if (possibleNotInBingo.stream().allMatch(matcher)) return Validator.TRUE;
        if (possibleNotInBingo.stream().noneMatch(matcher)) return Validator.FALSE;
        return Validator.UNKNOWN;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                sb.append(grid[col][row]);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        BoardState board = new BoardState();
        board.grid = new char[][]{
                " X  X".toCharArray(),
                "XX XX".toCharArray(),
                " XX X".toCharArray(),
                " X  X".toCharArray(),
                "XXXXX".toCharArray(),
        };
        System.out.println(board.tileValue('B', 4));
        System.out.println(board.rowBingo(1).or(board.colBingo(3)).matches(board.tileValue('B', 4)));

        System.out.println(board.isValid());


    }
}
