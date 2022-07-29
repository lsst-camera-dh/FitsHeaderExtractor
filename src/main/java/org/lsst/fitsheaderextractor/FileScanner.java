package org.lsst.fitsheaderextractor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

/**
 *
 * @author tonyj
 */
public class FileScanner {
    
    int scanForFiles(Path root, String filePattern, FileVisitor visitor) throws IOException {
        PathMatcher matcher = root.getFileSystem().getPathMatcher("glob:" + filePattern);
        return FileScanner.this.scanForFiles(root, matcher, visitor);
    }

    private int scanForFiles(Path root, PathMatcher matcher, FileVisitor visitor) throws IOException {
        return scanForFiles(root, (path, fileAttributes) -> matcher.matches(root.relativize(path)), visitor);
    }
    
    int scanForFiles(Path root,  BiPredicate<Path,BasicFileAttributes> predicate, FileVisitor visitor) throws IOException {
        AtomicInteger n = new AtomicInteger(0);
        try ( Stream<Path> stream = Files.find(root, Integer.MAX_VALUE, predicate)) {
            stream.forEach((p) -> {
                try {
                    visitor.visit(p);
                    n.incrementAndGet();
                } catch (IOException x) {
                    throw new UncheckedIOException(x);
                }
            });
        } catch (UncheckedIOException x) {
            throw x.getCause();
        }
        return n.get();
    }


    
    interface FileVisitor {
        
        public void visit(Path file) throws IOException;
    }
    
    public static void main(String[] args) throws IOException {
        Path path = Path.of("/home/tonyj/Data/");
        FileScanner scanner = new FileScanner();
        int nFilesFound = scanner.scanForFiles(path, "*.fits", (p) -> System.out.println(p));
        System.out.println(nFilesFound);
    }
}
