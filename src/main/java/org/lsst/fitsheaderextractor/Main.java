package org.lsst.fitsheaderextractor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import org.lsst.fitsheaderextractor.FitsHeaderReader.Mode;
import picocli.CommandLine;

/**
 *
 * @author tonyj
 */
@SuppressWarnings("FieldMayBeFinal")
@CommandLine.Command(name = "fhe", usageHelpAutoWidth = true)
public class Main implements Callable<Integer> {

    private final FileScanner scanner;

    @CommandLine.Option(names = {"--help", "-h"}, usageHelp = true, description = "display this help and exit")
    private boolean help;

    @CommandLine.Option(names = {"--dir", "-d"}, description = "Directory to scan for FITS files (default: ${DEFAULT-VALUE})")
    private Path root = Path.of(".");

    @CommandLine.Option(names = {"--mode", "-m"}, description = "Scan mode (default: ${DEFAULT-VALUE}). \nPRIMARY=use only primary headers\nIMAGEEXT=Use all image extensions\nALL=Use all HDU headers\nCRAZY=Use and combine primary and first image extension (emulate astrometadata)")
    private Mode mode = Mode.PRIMARY;

    @CommandLine.Option(names = {"--output", "-o"}, description = "Directory to write .json files. If not specified writes in the directory where FITS files were found")
    private Path outputDir = null;

    @CommandLine.Option(names = "-v", description = "Specify multiple -v options to increase verbosity. For example, `-v -v -v` or `-vvv`")
    private boolean[] verbosity = {};

    @CommandLine.Option(names = {"-u", "--update"}, description = "Update mode, skip .json files which already exist (default: ${DEFAULT-VALUE})")
    private boolean update = false;

    @CommandLine.Option(names = "--imageDirectoryPattern", description = "Regexp for image directories (default: ${DEFAULT-VALUE})")
    private Pattern imageDirectoryPattern = Pattern.compile("\\w\\w_\\w_\\d\\d\\d\\d\\d\\d\\d\\d_\\d\\d\\d\\d\\d\\d");

    @CommandLine.Option(names = "--imageFilePattern", description = "Regexp for image file (default: ${DEFAULT-VALUE})")
    private Pattern imagePattern = Pattern.compile(".+\\.fits");

    public Main() {
        scanner = new FileScanner();
    }

    @Override
    public Integer call() throws IOException {
        if (outputDir != null && !Files.isDirectory(outputDir)) {
            throw new RuntimeException("Invalid output directory: " + outputDir);
        }

        scanner.scanForFiles(root, (path, fileAttributes) -> fileAttributes.isDirectory() && imageDirectoryPattern.matcher(path.getFileName().toString()).matches(), this::visitDirectory);
        //ForkJoinPool.commonPool().awaitQuiescence(365, TimeUnit.DAYS);
        return 0;
    }

    private void visitDirectory(Path directory) throws IOException {
        try ( IndexWriter indexWriter = new IndexWriter(directory, mode, outputDir, verbosity.length)) {
            if (!update || !indexWriter.outputFileExists()) {
                scanner.scanForFiles(directory, (path, fileAttributes) -> (fileAttributes.isRegularFile() || fileAttributes.isSymbolicLink()) && imagePattern.matcher(path.getFileName().toString()).matches(), (file) -> visitFile(indexWriter, file));
            }
        }
    }

    private void visitFile(IndexWriter indexWriter, Path file) throws IOException {
        indexWriter.addFile(file);
    }

    public static void main(String[] args) throws IOException {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

}
