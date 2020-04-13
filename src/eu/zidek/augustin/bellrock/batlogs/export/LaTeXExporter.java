package eu.zidek.augustin.bellrock.batlogs.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Class for saving LaTeX files and compiling them. Note that this class is NOT
 * platform independent. MikTeX running on Windows is required.
 * 
 * @author Augustin Zidek
 *
 */
public class LaTeXExporter {

    /**
     * Saves the given string into the given .tex file and produces a .pdf file.
     * 
     * @param path The path of the .tex file.
     * @param contents The contents of the .tex file.
     * @return <code>true</code> if file successfully written and compiled,
     *         <code>false</code> otherwise.
     */
    public boolean saveAndCompile(final String path, final String contents) {
        try (final BufferedWriter out = Files
                .newBufferedWriter(Paths.get(path));) {
            // Write the tex file
            out.write(contents);
            out.close();
            // And compile it, producing pdf of the same name
            return compileWithPdfLaTeX(path);
        }
        catch (final IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean compileWithPdfLaTeX(final String path) throws IOException {
        final String parentDir = Paths.get(path).getParent().toString();
        final String filenameFull = Paths.get(path).getFileName().toString();
        final String filename = filenameFull.substring(0,
                filenameFull.length() - 4);

        // Execute pdflatex
        final String compileCommand = String.format(
                "pdflatex.exe -interaction=nonstopmode -quiet -output-directory %s %s",
                parentDir, path);
        final Runtime r = Runtime.getRuntime();
        final Process p = r.exec(compileCommand);

        // Determine if the compilation went OK
        final int exit;
        try {
            exit = p.waitFor();
        }
        catch (final InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        // Clean up the auxiliary files
        for (final String ext : new String[] { ".aux", ".log" }) {
            Files.delete(Paths.get(parentDir + "\\" + filename + ext));
        }

        return exit == 0;
    }
}
