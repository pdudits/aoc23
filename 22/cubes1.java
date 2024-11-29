///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS org.openjfx:javafx-graphics:21.0.5:${os.detected.jfxname}

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import javax.swing.*;

import static java.lang.System.*;

/**
 * The sand has been falling as large compacted bricks of sand, piling up to form an impressive stack here near the edge of Island Island. In order to make use of the sand to filter water, some of the bricks will need to be broken apart - nay, disintegrated - back into freely flowing sand.
 *
 * The stack is tall enough that you'll have to be careful about choosing which bricks to disintegrate; if you disintegrate the wrong brick, large portions of the stack could topple, which sounds pretty dangerous.
 *
 * The Elves responsible for water filtering operations took a snapshot of the bricks while they were still falling (your puzzle input) which should let you work out which bricks are safe to disintegrate. For example:
 *
 * 1,0,1~1,2,1
 * 0,0,2~2,0,2
 * 0,2,3~2,2,3
 * 0,0,4~0,2,4
 * 2,0,5~2,2,5
 * 0,1,6~2,1,6
 * 1,1,8~1,1,9
 *
 * Each line of text in the snapshot represents the position of a single brick at the time the snapshot was taken. The position is given as two x,y,z coordinates - one for each end of the brick - separated by a tilde (~). Each brick is made up of a single straight line of cubes, and the Elves were even careful to choose a time for the snapshot that had all of the free-falling bricks at integer positions above the ground, so the whole snapshot is aligned to a three-dimensional cube grid.
 *
 * A line like 2,2,2~2,2,2 means that both ends of the brick are at the same coordinate - in other words, that the brick is a single cube.
 *
 * Lines like 0,0,10~1,0,10 or 0,0,10~0,1,10 both represent bricks that are two cubes in volume, both oriented horizontally. The first brick extends in the x direction, while the second brick extends in the y direction.
 *
 * A line like 0,0,1~0,0,10 represents a ten-cube brick which is oriented vertically. One end of the brick is the cube located at 0,0,1, while the other end of the brick is located directly above it at 0,0,10.
 *
 * The ground is at z=0 and is perfectly flat; the lowest z value a brick can have is therefore 1. So, 5,5,1~5,6,1 and 0,2,1~0,2,5 are both resting on the ground, but 3,3,2~3,3,3 was above the ground at the time of the snapshot.
 *
 * Because the snapshot was taken while the bricks were still falling, some bricks will still be in the air; you'll need to start by figuring out where they will end up. Bricks are magically stabilized, so they never rotate, even in weird situations like where a long horizontal brick is only supported on one end. Two bricks cannot occupy the same position, so a falling brick will come to rest upon the first other brick it encounters.
 *
 * Here is the same example again, this time with each brick given a letter so it can be marked in diagrams:
 *
 * 1,0,1~1,2,1   <- A
 * 0,0,2~2,0,2   <- B
 * 0,2,3~2,2,3   <- C
 * 0,0,4~0,2,4   <- D
 * 2,0,5~2,2,5   <- E
 * 0,1,6~2,1,6   <- F
 * 1,1,8~1,1,9   <- G
 *
 * At the time of the snapshot, from the side so the x axis goes left to right, these bricks are arranged like this:
 *
 *  x
 * 012
 * .G. 9
 * .G. 8
 * ... 7
 * FFF 6
 * ..E 5 z
 * D.. 4
 * CCC 3
 * BBB 2
 * .A. 1
 * --- 0
 *
 * Rotating the perspective 90 degrees so the y axis now goes left to right, the same bricks are arranged like this:
 *
 *  y
 * 012
 * .G. 9
 * .G. 8
 * ... 7
 * .F. 6
 * EEE 5 z
 * DDD 4
 * ..C 3
 * B.. 2
 * AAA 1
 * --- 0
 *
 * Once all of the bricks fall downward as far as they can go, the stack looks like this, where ? means bricks are hidden behind other bricks at that location:
 *
 *  x
 * 012
 * .G. 6
 * .G. 5
 * FFF 4
 * D.E 3 z
 * ??? 2
 * .A. 1
 * --- 0
 *
 * Again from the side:
 *
 *  y
 * 012
 * .G. 6
 * .G. 5
 * .F. 4
 * ??? 3 z
 * B.C 2
 * AAA 1
 * --- 0
 *
 * Now that all of the bricks have settled, it becomes easier to tell which bricks are supporting which other bricks:
 *
 *     Brick A is the only brick supporting bricks B and C.
 *     Brick B is one of two bricks supporting brick D and brick E.
 *     Brick C is the other brick supporting brick D and brick E.
 *     Brick D supports brick F.
 *     Brick E also supports brick F.
 *     Brick F supports brick G.
 *     Brick G isn't supporting any bricks.
 *
 * Your first task is to figure out which bricks are safe to disintegrate. A brick can be safely disintegrated if, after removing it, no other bricks would fall further directly downward. Don't actually disintegrate any bricks - just determine what would happen if, for each brick, only that brick were disintegrated. Bricks can be disintegrated even if they're completely surrounded by other bricks; you can squeeze between bricks if you need to.
 *
 * In this example, the bricks can be disintegrated as follows:
 *
 *     Brick A cannot be disintegrated safely; if it were disintegrated, bricks B and C would both fall.
 *     Brick B can be disintegrated; the bricks above it (D and E) would still be supported by brick C.
 *     Brick C can be disintegrated; the bricks above it (D and E) would still be supported by brick B.
 *     Brick D can be disintegrated; the brick above it (F) would still be supported by brick E.
 *     Brick E can be disintegrated; the brick above it (F) would still be supported by brick D.
 *     Brick F cannot be disintegrated; the brick above it (G) would fall.
 *     Brick G can be disintegrated; it does not support any other bricks.
 *
 * So, in this example, 5 bricks can be safely disintegrated.
 *
 * Figure how the blocks will settle based on the snapshot. Once they've settled, consider disintegrating a single brick; how many bricks could be safely chosen as the one to get disintegrated?
 */
public class cubes1 {

    public static void main(String... args) throws IOException {
        var cubes = Files.lines(Path.of("input2.txt")).map(Brick::parse).collect(Collectors.toCollection(TreeSet::new));

        var brick1 = new Brick(new Coordinate(0,0,1), new Coordinate(0,0,1));

        var blocks = brick1.blocks().count();
        if (blocks != 1) {
            throw new AssertionError("That's my error right there");
        }

        var viewer = new BrickViewer();
        //viewer.display(cubes);

        out.println(cubes);
        var settled = Scene.settle(cubes);
        var z1 = settled.bricks.stream().filter(b -> b.a().z() == 1).collect(Collectors.toList());
        var nooverlap = z1.stream().allMatch(a -> z1.stream().filter(b -> a != b).filter(a::intersects).peek(b -> out.println(a + " intersects " + b)).count() == 0);
        out.println(nooverlap);
        //viewer.display(settled.bricks);
        //out.println(settled);
        out.println(settled.disintegratable());
    }

    record Support(Set<Brick> supports, Set<Brick> supportedBy) {
        Support() {
            this(new HashSet<>(), new HashSet<>());
        }
    }

    record Scene(TreeSet<Brick> bricks, Map<Coordinate, Brick> zMap, Map<Brick,Support> supportMap) {

        static Scene settle(TreeSet<Brick> bricks) {
            Map<Coordinate, Brick> zMap = new TreeMap<>();
            Map<Brick, Support> supportMap = new HashMap<>();
            var settledBricks = new TreeSet<Brick>();
            for (Brick brick : bricks) {
                var ground = brick.blocks()
                        .map(Coordinate::ground)
                        .map(zMap::get)
                        .filter(Objects::nonNull).mapToInt(b -> b.b().z()).max().orElse(0);
                var settled = brick.fallTo( ground + 1);
                var support = supportMap.computeIfAbsent(settled, b -> new Support());
                settled.blocks().forEach(c -> {
                    var prev = zMap.put(c.ground(), settled);
                    if (prev != null && prev != settled && prev.b().z() == c.z()-1) {
                        support.supportedBy.add(prev);
                        supportMap.get(prev).supports.add(settled);
                    }
                });
                settledBricks.add(settled);
            }
            return new Scene(settledBricks, zMap, supportMap);
        }

        long disintegratable() {
            return bricks.stream().filter(this::disintegratable).count();
        }

        boolean disintegratable(Brick brick) {
            var support = supportMap.get(brick);
            if (support == null) {
                return true;
            }
            return support.supports.isEmpty() || support.supports.stream().map(supportMap::get).allMatch(s -> s.supportedBy.size() > 1);
        }
    }
}
record Coordinate(int x, int y, int z) implements Comparable<Coordinate> {
    static Coordinate parse(String s) {
        var parts = s.split(",");
        return new Coordinate(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }

    static Comparator<Coordinate> COMPARATOR =
            Comparator.comparingInt(Coordinate::z).thenComparingInt(Coordinate::y).thenComparingInt(Coordinate::x);
    @Override
    public int compareTo(Coordinate o) {
        return COMPARATOR.compare(this, o);
    }

    Coordinate translate(int dx, int dy, int dz) {
        return new Coordinate(x + dx, y + dy, z + dz);
    }

    Coordinate min(Coordinate b) {
        return new Coordinate(Math.min(x, b.x), Math.min(y, b.y), Math.min(z, b.z));
    }

    Coordinate max(Coordinate b) {
        return new Coordinate(Math.max(x, b.x), Math.max(y, b.y), Math.max(z, b.z));
    }

    public Coordinate ground() {
        return new Coordinate(x, y, 0);
    }
}

record Brick(Coordinate a, Coordinate b) implements Comparable<Brick> {
    final static Coordinate SUSPECT = new Coordinate(0,0,1);
    public Brick {
        if (a.equals(SUSPECT)) {
            out.println("SUSPECT: " + this);
        }
    }


    static Brick parse(String s) {
        var parts = s.split("~");
        Coordinate a = Coordinate.parse(parts[0]);
        Coordinate b = Coordinate.parse(parts[1]);
        Coordinate min = a.min(b);
        Coordinate max = a.max(b);
        return new Brick(min, max);
    }
    static final Comparator<Brick> COMPARATOR = Comparator.comparing(Brick::a).thenComparing(Brick::b);
    @Override
    public int compareTo(Brick o) {
        return COMPARATOR.compare(this, o);
    }

    Brick translate(int dx, int dy, int dz) {
        return new Brick(a.translate(dx, dy, dz), b.translate(dx, dy, dz));
    }

    Brick fallTo(int z) {
        return translate(0, 0, z - a.z());
    }

    Coordinate vector() {
        return new Coordinate(a.x() == b.x() ? 0 : 1, a.y() == b.y() ? 0 : 1, a.z() == b.z() ? 0 : 1);
    }

    int length() {
        return Math.abs(b.x()-a.x()) + Math.abs(b.y()-a.y()) + Math.abs(b.z()-a.z());
    }

    boolean intersects(Brick other) {
        return overlapOnAxis(a.x(), b.x(), other.a.x(), other.b.x()) &&
                overlapOnAxis(a.y(), b.y(), other.a.y(), other.b.y()) &&
                overlapOnAxis(a.z(), b.z(), other.a.z(), other.b.z());
    }

    private boolean overlapOnAxis(int aMin, int aMax, int bMin, int bMax) {
        // Check if the projections overlap on the given axis
        return Math.max(aMin, bMin) <= Math.min(aMax, bMax);
    }

    Stream<Coordinate> blocks() {
        Coordinate vector = vector();
        var stop = b.translate(vector.x(), vector.y(), vector.z());
        return Stream.iterate(a, c -> c.translate(vector.x(), vector.y(), vector.z())).limit(length()+1);
    }
}

class BrickViewer {

    private static CountDownLatch latch;
    private static double anchorX, anchorY;
    private static double anchorAngleX = 0;
    private static double anchorAngleY = 0;
    private static final Rotate rotateX = new Rotate(-90, Rotate.X_AXIS);
    private static final Rotate rotateY = new Rotate(45, Rotate.Z_AXIS);

    public BrickViewer() {
    }

    public void display(Collection<Brick> bricks) {
        latch = new CountDownLatch(1);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3D Brick Viewer");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setSize(800, 600);

            JFXPanel fxPanel = new JFXPanel();
            frame.add(fxPanel, BorderLayout.CENTER);

            frame.setVisible(true);

            Platform.runLater(() -> initFX(bricks, fxPanel));

            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent windowEvent) {
                    latch.countDown();
                    fxPanel.setScene(null);
                }
            });
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void initFX(Collection<Brick> bricks, JFXPanel fxPanel) {
        Group root = new Group();
        Scene scene = new Scene(root, 800, 600, true);
        scene.setFill(Color.GRAY);

        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.getTransforms().addAll(
                rotateY,
                rotateX,
                new Translate(0, 0, -50)
        );
        scene.setCamera(camera);

        for (Brick brick : bricks) {
            Box box = createBox(brick);
            root.getChildren().add(box);
        }

        // Add z-axis labels
        for (int z = 0; z <= 1000; z += 10) {
            Text label = new Text(0, 0, String.valueOf(z));
            label.setTranslateX(-1); // Adjust position as needed
            label.setTranslateY(-1); // Adjust position as needed
            label.setTranslateZ(z);
            label.setRotationAxis(Rotate.Y_AXIS);
            label.setRotate(90); // Rotate to face the camera
            root.getChildren().add(label);
        }

        initMouseControl(root, scene);

        fxPanel.setScene(scene);
    }

    private static Box createBox(Brick brick) {
        double width = Math.abs(brick.b().x() - brick.a().x()) + 0.8;
        double height = Math.abs(brick.b().y() - brick.a().y()) + 0.8;
        double depth = Math.abs(brick.b().z() - brick.a().z()) + 0.8;

        Box box = new Box(width, height, depth);
        var color = Color.hsb(Math.random()* 360, 0.8, 0.8, 0.5);
        box.setMaterial(new PhongMaterial(color));
        box.setTranslateX(width / 2 + brick.a().x());
        box.setTranslateY(height / 2 + brick.a().y());
        box.setTranslateZ(depth / 2 + brick.a().z());

        return box;
    }

    private static void initMouseControl(Group root, Scene scene) {
        scene.setOnMousePressed(event -> {
            anchorX = event.getSceneX();
            anchorY = event.getSceneY();
            anchorAngleX = rotateX.getAngle();
            anchorAngleY = rotateY.getAngle();
        });

        scene.setOnMouseDragged(event -> {
            rotateX.setAngle(anchorAngleX - (anchorY - event.getSceneY()));
            rotateY.setAngle(anchorAngleY + anchorX - event.getSceneX());
        });
    }
}