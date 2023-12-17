///usr/bin/env jbang "$0" "$@" ; exit $?


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.System.*;

public class galaxy1 {

    public static void main(String... args) throws IOException {
        var m = new galaxy1(Files.readAllLines(Path.of("input2.txt")));
        out.println(m.allGalaxyDistances());
    }

    final SpaceMap spaceMap;
    final Space space;

    galaxy1(List<String> input) {
        spaceMap = new SpaceMap(input);
        space = Space.forMap(spaceMap);
    }

    long allGalaxyDistances() {
        var galaxies = spaceMap.galaxies();
        var totalDistance = 0L;
        // sum up distance between all pairs of galaxies;
        for (int i = 0; i < galaxies.size() - 1; i++) {
            for (int j = i + 1; j < galaxies.size(); j++) {
                totalDistance += distanceBetween(galaxies.get(i), galaxies.get(j));
            }
        }
        return totalDistance;
    }

    long distanceBetween(Coord a, Coord b) {
        return space.transform(a).distanceTo(space.transform(b));
    }


    record Coord(long x, long y) {
        long distanceTo(Coord b) {
            return Math.abs(b.x-x) + Math.abs(b.y - y);
        }
    }
    record SpaceMap(List<String> map) {
       int width() {
           return map.get(0).length();
       }
       int height() {
           return map.size();
       }

       boolean isGalaxy(int x, int y) {
           return x >= 0 && x < width() && y >= 0 && y < height()
                   && map.get(y).charAt(x) == '#';
       }

       boolean anyGalaxyInRow(int y) {
           return map.get(y).contains("#");
       }

       boolean anyGalaxyInColumn(int x) {
           return map.stream().anyMatch(r -> r.charAt(x) == '#');
       }

       List<Coord> galaxies() {
           var result = new ArrayList<Coord>();
           for (int y = 0; y < height(); y++) {
               // any position of # makes up the x coordinate:
               var line = map.get(y);
               for(int x = 0; x < line.length(); x++) {
                   if (line.charAt(x) == '#') {
                       result.add(new Coord(x, y));
                   }
               }
           }
           return result;
       }
    }
    record Space(long[][] distances) {
        static Space forMap(SpaceMap map) {
            var d = new long[map.height()][map.width()];
            // initialize map with ones
            for (int i = 0; i < map.height(); i++) {
                Arrays.fill(d[i], 1);
            }
            // multiply any row an any column that has no galaxy by 2
            // first the rows:
            for(int y = 0; y < map.height(); y++) {
                if (!map.anyGalaxyInRow(y)) {
                    for (int x = 0; x < map.width(); x++) {
                        d[y][x] *= 1000000;
                    }
                }
            }
            // the columns:
            for(int x = 0; x < map.width(); x++) {
                if (!map.anyGalaxyInColumn(x)) {
                    for (int y = 0; y < map.height(); y++) {
                        d[y][x] *= 1000000;
                    }
                }
            }
            return new Space(d);
        }

        Coord transform(Coord a) {
            // we're just simply sum the path from 0 to the coordinates
            var newX = 0;
            for(int x = 0; x < (int)a.x; x++) {
                newX += distances[0][x];
            }
            var newY = 0;
            for(int y=0; y < (int)a.y; y++) {
                newY += distances[y][(int)a.x];
            }
            return new Coord(newX, newY);
        }
    }

}
