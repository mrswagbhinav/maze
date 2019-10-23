import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;
import java.io.*;
import javax.sound.sampled.*;

public class MazeProgram extends JPanel implements KeyListener, MouseListener {
  private static final long serialVersionUID = 1L;
  JFrame frame;
  Maze maze;

  int scale = 15; // scale of 2d maze
  int row = 10; // size of the maze
  int col = 10; // size of the maze

  int mazes = 1;
  boolean gameOver = false;

  Wall[][] walls = new Wall[(row * 2) + 1][(row * 2) + 1];
  ArrayList<Wall> wallList = new ArrayList<Wall>();
  Explorer explorer = new Explorer(new Location(1, 1), 0, 3, 100, false);
  Location fLocation = new Location((row * 2) - 1, (row * 2) - 1);

  int mapSize = explorer.getVisibleDist(); // area shown on the 2d minimap

  int xMax = 800; // horizontal window res - 600
  int yMax = 800; // vertical window res
  int iterX = xMax / 11; // size of iterations of each level for X
  int iterY = yMax / 11; // size of iterations of each level for Y

  public MazeProgram() {
    setBoard();
    frame = new JFrame();
    frame.add(this);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(xMax + 600, yMax);
    frame.setTitle("Abhinav's Maze");
    frame.setVisible(true);
    frame.addKeyListener(this);

    try {
      File yourFile = new File("bg_music.wav");
      AudioInputStream stream;
      AudioFormat format;
      DataLine.Info info;
      Clip clip;

      stream = AudioSystem.getAudioInputStream(yourFile);
      format = stream.getFormat();
      info = new DataLine.Info(Clip.class, format);
      clip = (Clip) AudioSystem.getLine(info);
      clip.open(stream);
      clip.start();
      clip.loop(Clip.LOOP_CONTINUOUSLY);
      Thread.sleep(10000);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void paintComponent(Graphics g) {
    if (!explorer.getLocation().equals(fLocation) && !gameOver) { // 3d stuff
      super.paintComponent(g);
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, xMax, yMax);
      for (int i = 0; i < wallList.size(); i++) {
        Polygon p = new Polygon();
        Wall w = wallList.get(i);

        for (int j = 0; j < w.getX().length; j++)
          p = new Polygon(w.getX(), w.getY(), w.getX().length);

        // draws borders of the trapezoids
        g.setColor(Color.CYAN);
        g.drawPolygon(p);

        // makes each level a darker gray and colors the finish green
        if (explorer.seeFinish(maze) && i == wallList.size() - 1)
          g.setColor(Color.GREEN);
        else
          g.setColor(new Color(220 - (i * 10), 220 - (i * 10), 220 - (i * 10)));
        g.fillPolygon(p);
      }

      // 2d
      g.setColor(Color.DARK_GRAY);
      g.fillRect(xMax, 0, xMax + 600, yMax);
      g.setColor(Color.WHITE);

      // try {
      // final BufferedImage image = ImageIO.read(new File("img.png"));
      // g.drawImage(image, 0, 0, null);
      // } catch (IOException e) {
      // e.printStackTrace();
      // }

      // loops through the 2d array and draws each wall
      for (int i = 0; i < walls.length; i++) {
        for (int j = 0; j < walls[i].length; j++) {
          Wall w = walls[i][j];
          if (w != null && explorer.getLocation().getDistance(w.getLocation()) < mapSize)
            g.fillRect(w.getCol() * w.getWidth() + 950, w.getRow() * w.getHeight() + 200, w.getWidth(), w.getHeight());
        }
      }

      // draws explorer
      g.setColor(Color.RED);
      g.fillOval(explorer.getCol() * scale + 950, explorer.getRow() * scale + 200, scale, scale);

      // draw finish
      if (explorer.getLocation().getDistance(fLocation) < mapSize) {
        g.setColor(Color.GREEN);
        g.fillRect(fLocation.getCol() * scale + 950, fLocation.getRow() * scale + 200, scale, scale);
      }

      // draw stats
      g.setColor(Color.WHITE);
      g.setFont(new Font("Arial", Font.BOLD, 12));

      Button exitButton = new Button("Exit");
      exitButton.addActionListener(new Exit());

      g.drawString("* Press Spacebar for Flashlight", 1200, 35);
      g.drawString("* Press Delete to Quit", 1200, 50);

      g.drawString("Maze Number: " + mazes, 1200, 610);

      g.drawString("Moves: " + maze.getMoves(), 1200, 640);

      if (explorer.getFlash())
        g.drawString("Flashlight: On", 1200, 670);
      else
        g.drawString("Flashlight: Off", 1200, 670);

      g.drawString("Battery: " + explorer.getBattery() + "%", 1200, 700);

      g.drawString("Visiblity: " + explorer.getVisibleDist() + " Spaces", 1200, 730);

      if ((explorer.getRotation() % 360) == 0)
        g.drawString("Direction: North", 1200, 760);
      else if ((explorer.getRotation() % 360) == 90 || (explorer.getRotation() % 360) == -270)
        g.drawString("Direction: East", 1200, 760);
      else if ((explorer.getRotation() % 360) == 180 || (explorer.getRotation() % 360) == -180)
        g.drawString("Direction: South", 1200, 760);
      else if ((explorer.getRotation() % 360) == 270 || (explorer.getRotation() % 360) == -90)
        g.drawString("Direction: West", 1200, 760);

    } else if (!gameOver) { // game over screen
      explorer.setLocation(new Location(1, 1));
      explorer.setBattery(100);
      walls = new Wall[(row * 2) + 1][(row * 2) + 1];
      mazes++;
      setBoard();
      repaint();
    } else if (gameOver) {
      super.paintComponent(g);
      g.setColor(Color.LIGHT_GRAY);
      g.fillRect(0, 0, xMax + 600, yMax);
      g.setColor(Color.BLACK);
      g.setFont(new Font("Arial", Font.BOLD, 36));
      g.drawString("You escaped from " + (mazes - 1) + " Mazes", 450, 400);
    }
  }

  public void setBoard() {
    RecursiveDivision recDiv = new RecursiveDivision(row, col);
    recDiv.makeMaze();
    // recDiv.printMaze();
    char[][] board = recDiv.getMaze();
    for (int r = 0; r < board.length; r++) {
      for (int c = 0; c < board[r].length; c++) {
        if (board[r][c] == 35) {
          walls[r][c] = new Wall(new Location(r, c), scale, scale);
        } else if (board[r][c] == 83) {
          explorer.setLocation(new Location(r, c));
        } else if (board[r][c] == 69) {
          fLocation = new Location(r, c);
        }
      }
    }

    // creates maze object to store the walls in
    ArrayList<Location> locations = new ArrayList<Location>();
    for (int i = 0; i < walls.length; i++) {
      for (int j = 0; j < walls[i].length; j++) {
        if (walls[i][j] != null)
          locations.add(walls[i][j].getLocation());
      }
    }
    maze = new Maze(locations, fLocation, 0);

    setWalls();
  }

  public void setWalls() {
    ArrayList<int[]> xList = new ArrayList<>();
    ArrayList<int[]> yList = new ArrayList<>();

    wallList.clear();

    for (int i = 0; i < explorer.getSpace(maze); i++) {
      // ceiling
      xList.add(new int[] { i * iterX, xMax - i * iterX, xMax - iterX - i * iterX, iterX + i * iterX });
      yList.add(new int[] { i * iterY, i * iterY, iterY + i * iterY, iterY + i * iterY });
      // floor
      xList.add(new int[] { i * iterX, xMax - i * iterX, xMax - iterX - i * iterX, iterX + i * iterX });
      yList.add(new int[] { yMax - i * iterY, yMax - i * iterY, yMax - iterY - i * iterY, yMax - iterY - i * iterY });
      // right
      if (explorer.isFree(maze, i, 90)) {
        xList.add(new int[] { xMax - i * iterX, xMax - iterX - i * iterX, xMax - iterX - i * iterX, xMax - i * iterX });
        yList.add(new int[] { i * iterY, i * iterY + iterY, yMax - iterY - i * iterY, yMax - i * iterY });
      }
      // left
      if (explorer.isFree(maze, i, -90)) {
        xList.add(new int[] { i * iterX, iterX + i * iterX, iterX + i * iterX, i * iterX });
        yList.add(new int[] { i * iterY, i * iterY + iterY, yMax - iterY - i * iterY, yMax - i * iterY });
      }
      // back wall
      if (i == explorer.getSpace(maze) - 1) {
        xList.add(
            new int[] { xMax - iterX - i * iterX, iterX + i * iterX, iterX + i * iterX, xMax - iterX - i * iterX });
        yList.add(
            new int[] { yMax - iterY - i * iterY, yMax - iterY - i * iterY, iterY + i * iterY, iterY + i * iterY });
      }
    }

    for (int i = 0; i < xList.size(); i++)
      wallList.add(new Wall(xList.get(i), yList.get(i)));
  }

  public void keyReleased(KeyEvent e) {
  }

  public void keyPressed(KeyEvent e) {
    if (!explorer.getLocation().equals(fLocation)) {
      if (e.getKeyCode() == 87) { // moves explorer in direction its facing
        explorer.move(maze);
        if (explorer.getBattery() < 1) {
          explorer.setVisibleDist(3);
          explorer.toggleFlash();
          mapSize = explorer.getVisibleDist();
        } else if (explorer.getBattery() < 50 && explorer.getFlash())
          explorer.setVisibleDist(4);
      }
      if (e.getKeyCode() == 65) // turns 90 degrees counterclockwise
        explorer.turn(-90);
      if (e.getKeyCode() == 68) // turns 90 degrees clockwise
        explorer.turn(90);
      if (e.getKeyCode() == 32 && explorer.getBattery() > 0) {
        explorer.toggleFlash();
        mapSize = explorer.getVisibleDist();
      }
      setWalls();
      repaint();
    }
    if (e.getKeyCode() == 8) {
      gameOver = true;
    }
  }

  public class Exit implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
      gameOver = false;
    }
  }

  public void keyTyped(KeyEvent e) {
  }

  public void mouseClicked(MouseEvent e) {
  }

  public void mousePressed(MouseEvent e) {
  }

  public void mouseReleased(MouseEvent e) {
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  public static void main(String args[]) {
    new MazeProgram();
  }
}
