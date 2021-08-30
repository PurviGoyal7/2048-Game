package com.purvi.game;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Random;

public class GameBoard
{
    public static final int ROWS = 4;
    public static final int COL = 4;

    private final int startingTiles = 2;
    private Tile[][] board;
    private boolean dead;
    private boolean won;
    private BufferedImage gameBoard;      //background of board when tiles are not there
    private BufferedImage finalBoard;     //final game board having all tiles on board

    private int x;
    private int y;

    private int score = 0;
    private Font scoreFont;

    private static int SPACING = 10;
    public static int BOARD_WIDTH = (COL + 1) * SPACING + COL * Tile.WIDTH;
    public static int BOARD_HEIGHT = (ROWS + 1) * SPACING + ROWS * Tile.HEIGHT;

    private boolean hasStarted;

    private String saveDataPath;
    private String fileName = "SaveData";

    public GameBoard(int x, int y)
    {
        try{
            saveDataPath = GameBoard.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        scoreFont = Game.main.deriveFont(24f);
        this.x = x;
        this.y = y;
        board = new Tile[ROWS][COL];
        gameBoard = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        finalBoard = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_RGB);

        createBoardImage();
        start();
    }

    private void createSaveData() {
        try{
            File file = new File(saveDataPath, fileName);
            FileWriter output = new FileWriter(file);
            BufferedWriter writer = new BufferedWriter(output);
            writer.write(""+0);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    private void createBoardImage() {
        Graphics2D g = (Graphics2D) gameBoard.getGraphics();
        g.setColor(Color.darkGray);
        g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
        g.setColor(Color.lightGray);

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COL; c++) {
                int x = SPACING + SPACING * c + Tile.WIDTH * c;
                int y = SPACING + SPACING * r + Tile.WIDTH * r;
                g.fillRoundRect(x, y, Tile.WIDTH, Tile.HEIGHT, Tile.ARC_WIDTH, Tile.ARC_HEIGHT);


            }
        }
    }
    public void start()
    {
        for(int i = 0;i<startingTiles;i++ )
        {
            spawnRandom();
        }
    }
    private void spawnRandom()
    {
        Random random = new Random();
        boolean notValid = true;

        while(notValid)
        {
            int location = random.nextInt(ROWS*COL);
            int row = location/ROWS;
            int col = location % COL;
            Tile current = board[row][col];
            if(current == null)
            {
                int value = random.nextInt(10) < 9 ? 2:4;
                Tile tile = new Tile(value, getTileX(col), getTileY(row));
                board[row][col] = tile;
                notValid = false;
            }
        }
    }

    public int getTileX(int col)
    {
        return SPACING + col*Tile.WIDTH + col*SPACING;
    }
    public int getTileY(int row)
    {
        return SPACING + row*Tile.HEIGHT + row*SPACING;
    }
    public void render (Graphics2D g)
    {
        Graphics2D g2d = (Graphics2D) finalBoard.getGraphics();
        g2d.drawImage(gameBoard, 0, 0, null);

        for(int r = 0;r<ROWS;r++)
        {
            for(int c = 0;c<COL;c++)
            {
                Tile current = board[r][c];
                if(current == null)
                    continue;
                current.render(g2d);
            }
        }
        //draw tiles
        g.drawImage(finalBoard, x, y, null);
        g2d.dispose();

        g.setColor(Color.lightGray);
        g.setFont(scoreFont);
        g.drawString(""+score,30, 40);
        g.setColor(Color.red);
    }

    public void update ()
    {
        checkKeys();
        for(int r = 0;r<ROWS;r++)
        {
            for (int c = 0; c < COL; c++)
            {
                Tile current = board[r][c];
                if(current == null)
                    continue;
                current.update();
                resetPosition(current, r, c);
                if(current.getValue() == 2048)
                {
                    won = true;
                }
            }
        }
    }

    private void resetPosition(Tile current, int r, int c) {
        if(current == null) return;

        int x = getTileX(c);
        int y = getTileY(r);

        int distX = current.getX() - x;
        int distY = current.getY() - y;

        if(Math.abs(distX) < Tile.SLIDE_SPEED)
        {
            current.setX(current.getX() - distX);
        }
        if(Math.abs(distY) < Tile.SLIDE_SPEED)
        {
            current.setY(current.getY() - distY);
        }
        if(distX < 0)
        {
            current.setX(current.getX() + Tile.SLIDE_SPEED);
        }
        if(distY < 0)
        {
            current.setY(current.getY() + Tile.SLIDE_SPEED);
        }
        if(distX > 0)
        {
            current.setX(current.getX() - Tile.SLIDE_SPEED);
        }
        if(distY > 0)
        {
            current.setY(current.getY() - Tile.SLIDE_SPEED);
        }

    }

    private boolean move(int row, int col, int horizontalDirection, int verticalDirection, Direction dir)
    {
        boolean canMove = false;
        Tile current = board[row][col];
        if(current == null) return false;
        boolean move = true;
        int newCol = col;
        int newRow = row;
        while(move)
        {
            newCol += horizontalDirection;
            newRow += verticalDirection;
            if(checkOutOfBounds(dir, newRow, newCol)) break;
            if(board[newRow][newCol] == null)
            {
                board[newRow][newCol] = current;
                board[newRow - verticalDirection][newCol - horizontalDirection] = null;
                board[newRow][newCol].setSlideTo(new Point(newRow, newCol));
                canMove = true;
            }
            else if(board[newRow][newCol].getValue() == current.getValue() && board[newRow][newCol].canCombine())
            {
                board[newRow][newCol].setCanCombine(false);
                board[newRow][newCol].setValue(board[newRow][newCol].getValue()*2);
                canMove =true;
                board[newRow - verticalDirection][newCol-horizontalDirection]=null;
                board[newRow][newCol].setSlideTo(new Point(newRow, newCol));
                //board[newRow][newCol].setCombineAnimations(true);
                score += board[newRow][newCol].getValue();
            }
            else
            {
                move = false;
            }
        }
        return canMove;
    }

    private boolean checkOutOfBounds(Direction dir, int row, int col)
    {
        if(dir == Direction.LEFT)
        {
            return col<0;
        }
        else if(dir == Direction.RIGHT)
        {
            return col>COL-1;
        }
        else if(dir == Direction.UP)
        {
            return row<0;
        }
        else if(dir == Direction.DOWN)
        {
            return row>ROWS-1;
        }
        return false;
    }

    private void moveTiles(Direction dir)
    {
        boolean canMove = false;
        int horizontalDirection = 0;
        int verticalDirection = 0;

        if(dir == Direction.LEFT)
        {
            horizontalDirection =-1;
            for(int r = 0; r<ROWS;r++)
            {
                for(int c = 0;c<COL;c++)
                {
                    if(!canMove)
                    {
                        canMove = move(r, c, horizontalDirection, verticalDirection, dir);
                    }
                    else
                    {
                        move(r, c, horizontalDirection, verticalDirection, dir);
                    }
                }
            }
        }
        else if(dir == Direction.RIGHT)
        {
            horizontalDirection =1;
            for(int r = 0; r<ROWS;r++)
            {
                for(int c = COL-1;c>=0;c--)
                {
                    if(!canMove)
                    {
                        canMove = move(r, c, horizontalDirection, verticalDirection, dir);
                    }
                    else
                    {
                        move(r, c, horizontalDirection, verticalDirection, dir);
                    }
                }
            }
        }
        else if(dir == Direction.UP)
        {
            verticalDirection = -1;
            for(int r = 0; r<ROWS;r++)
            {
                for(int c = 0;c<COL;c++)
                {
                    if(!canMove)
                    {
                        canMove = move(r, c, horizontalDirection, verticalDirection, dir);
                    }
                    else
                    {
                        move(r, c, horizontalDirection, verticalDirection, dir);
                    }
                }
            }
        }
        else if(dir == Direction.DOWN)
        {
            verticalDirection =1;
            for(int r = ROWS-1; r>=0;r--)
            {
                for(int c = 0;c<COL;c++)
                {
                    if(!canMove)
                    {
                        canMove = move(r, c, horizontalDirection, verticalDirection, dir);
                    }
                    else
                    {
                        move(r, c, horizontalDirection, verticalDirection, dir);
                    }
                }
            }
        }
        else
        {
            System.out.println(dir + "is not a valid direction");
        }
        for(int r = 0;r<ROWS;r++)
        {
            for(int c = 0; c<COL; c++)
            {
                Tile current = board[r][c];
                if(current == null) continue;
                current.setCanCombine(true);
            }
        }
        if(canMove)
        {
            spawnRandom();
            //check for dead
            checkDead();
        }
    }

    private void checkDead() {
        for(int r = 0; r<ROWS; r++)
        {
            for(int c = 0;c<COL;c++)
            {
                if(board[r][c] == null) return;
                if(checkSurroundingTiles(r, c, board[r][c]))
                {
                    return;
                }
            }
        }
        dead = true;
    }

    private boolean checkSurroundingTiles(int r, int c, Tile current) {
        if(r>0)
        {
            Tile check = board[r-1][c];
            if(check == null) return true;
            if(current.getValue() == check.getValue()) return true;
        }
        if(r < ROWS-1)
        {
            Tile check = board[r+1][c];
            if(check == null) return true;
            if(current.getValue() == check.getValue()) return true;
        }
        if(c>0)
        {
            Tile check = board[r][c-1];
            if(check == null) return true;
            if(current.getValue() == check.getValue()) return true;
        }
        if(c < COL-1)
        {
            Tile check = board[r][c+1];
            if(check == null) return true;
            if(current.getValue() == check.getValue()) return true;
        }
        return false;
    }

    private void checkKeys() {
        if(Keyboard.typed(KeyEvent.VK_LEFT))
        {
            moveTiles(Direction.LEFT);
            if(!hasStarted)
                hasStarted = true;
        }
        if(Keyboard.typed(KeyEvent.VK_RIGHT))
        {
            moveTiles(Direction.RIGHT);
            if(!hasStarted)
                hasStarted = true;
        }
        if(Keyboard.typed(KeyEvent.VK_UP))
        {
            moveTiles(Direction.UP);
            if(!hasStarted)
                hasStarted = true;
        }
        if(Keyboard.typed(KeyEvent.VK_DOWN))
        {
            moveTiles(Direction.DOWN);
            if(!hasStarted)
                hasStarted = true;
        }
    }
}

