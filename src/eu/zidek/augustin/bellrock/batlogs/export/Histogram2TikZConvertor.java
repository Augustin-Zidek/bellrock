package eu.zidek.augustin.bellrock.batlogs.export;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Converts a histogram (a map of objects to numbers) into a TikZ format that
 * can be compiled into a nice graphical histogram using LaTeX. The order of the
 * keys is determined by the ordering given by their comparator.
 * 
 * @author Augustin Zidek
 *
 */
public class Histogram2TikZConvertor {
    private StringBuilder sb;

    private void append(final String s) {
        this.sb.append(String.format("%s%n", s));
    }

    private void buildHeader() {
        append("\\documentclass[11pt]{standalone}");
        append("\\usepackage[T1]{fontenc}");
        append("\\usepackage{lmodern}");
        append("\\usepackage{pgfplots}");
        append("");
        append("\\pgfplotsset{compat=newest, xlabel near ticks, ylabel near ticks, grid style={white!90!black}}");
        append("");
        append("\\begin{document}");
        append("\\begin{tikzpicture}[font=\\small]");

        append("\\definecolor{c1}{HTML}{331700}");
        append("\\definecolor{c2}{HTML}{295F99}");
        append("\\definecolor{c3}{HTML}{FF7F00}");
        append("\\definecolor{c4}{HTML}{CFCF00}");
        append("\\definecolor{c5}{HTML}{7F007F}");
        append("\\definecolor{c6}{HTML}{00A833}");
        append("\\definecolor{c7}{HTML}{FF0000}");
        append("\\definecolor{c8}{HTML}{94AFCC}");
        append("\\definecolor{c9}{HTML}{994B00}");
        append("\\definecolor{c10}{HTML}{196019}");
        append("\\definecolor{c11}{HTML}{590B3F}");
        append("\\definecolor{c12}{HTML}{148366}");
        append("\\definecolor{c13}{HTML}{FF3F00}");
        append("\\definecolor{c14}{HTML}{7FD319}");
        append("\\definecolor{c15}{HTML}{BF003F}");
        append("\\definecolor{c16}{HTML}{374752}");
        append("");
        append("\\pgfplotscreateplotcyclelist{nicecol}");
        append("{{c1,fill=c1!70!white,draw=black},");
        append("{c2,fill=c2!70!white,draw=black},");
        append("{c3,fill=c3!70!white,draw=black},");
        append("{c4,fill=c4!70!white,draw=black},");
        append("{c5,fill=c5!70!white,draw=black},");
        append("{c6,fill=c6!70!white,draw=black},");
        append("{c7,fill=c7!70!white,draw=black},");
        append("{c8,fill=c8!70!white,draw=black},");
        append("{c9,fill=c9!70!white,draw=black},");
        append("{c10,fill=c10!70!white,draw=black},");
        append("{c11,fill=c11!70!white,draw=black},");
        append("{c12,fill=c12!70!white,draw=black},");
        append("{c13,fill=c13!70!white,draw=black},");
        append("{c14,fill=c14!70!white,draw=black},");
        append("{c15,fill=c15!70!white,draw=black},");
        append("{c16,fill=c16!70!white,draw=black}}");
    }

    private void buildPlotConfig(final String xLabel, final String yLabel,
            final List<String> xCoords, final boolean displayRawNumbers) {
        // This is the list of labels to be used
        final StringBuilder xCoordsStr = new StringBuilder();
        xCoords.forEach(e -> {
            xCoordsStr.append(e);
            xCoordsStr.append(",");
        });
        // Delete the last comma
        xCoordsStr.deleteCharAt(xCoordsStr.length() - 1);

        append("\\begin{axis}[");
        append("  ybar,");
        append("  ymajorgrids,");
        append("  bar width=20pt,");
        append("  x=22pt,");
        append("  xlabel={" + xLabel + "},");
        append("  ylabel={" + yLabel + "},");
        append("  ymin=0,");
        append("  xtick={" + xCoordsStr.toString() + "},");
        append("  axis x line*=bottom,");
        append("  axis y line*=left,");
        append("  enlarge x limits=" + 1.0 / xCoords.size() + ",");
        append("  symbolic x coords={" + xCoordsStr.toString() + "},");
        append("  xticklabel style={anchor=east,rotate=90},");
        append("  scaled y ticks = false,");
        if (displayRawNumbers) {
            append("  nodes near coords={\\pgfmathprintnumber\\pgfplotspointmeta},");
        }
        else {
            append("  point meta={y*100},");
            append("  yticklabel={\\pgfmathparse{\\tick*100}\\pgfmathprintnumber{\\pgfmathresult}\\%},");
            append("  nodes near coords={\\pgfmathprintnumber\\pgfplotspointmeta\\%},");
        }
        append("  every node near coord/.append style={rotate=90, anchor=west},");
        append("  bar shift=0pt,");
        append("  cycle list name=nicecol");

        append("]");
    }

    private <K> void buildHistogram(final Map<K, ? extends Number> map) {
        // Get the list of histogram (key, value) pairs. The order is not
        // relevant, as key coordinate system is used in TikZ
        for (final Map.Entry<K, ? extends Number> entry : map.entrySet()) {
            append(String.format("  \\addplot+ coordinates { (%s,%s) };",
                    entry.getKey().toString(), entry.getValue().toString()));
        }
    }

    private void buildFooter() {
        append("\\end{axis}");
        append("\\end{tikzpicture}");
        append("\\end{document}");
    }

    /**
     * Converts the given histogram to TikZ bar plot.
     * 
     * @param histogram The histogram.
     * @param xLabel The label for the x-axis.
     * @param yLabel The label for the y-axis.
     * @param displayRawNumbers <code>true</code> if the numbers should be left
     *            as they are, displayed as they are. <code>false</code> if the
     *            values in the histogram should be treated as frequencies that
     *            sum to one, then multiplying by 100 and displayed with a
     *            percent symbol.
     * @return A string containing the histogram. The string should be saved
     *         into a file that can be directly compiled using pdfLaTeX to
     *         produce a pdf with the histogram.
     */
    public <K extends Comparable<? super K>> String convert(
            final Map<K, ? extends Number> histogram, final String xLabel,
            final String yLabel, final boolean displayRawNumbers) {
        this.sb = new StringBuilder();
        // Get the list of histogram keys and sort them using their comparator
        final List<K> xCoords = new ArrayList<>();
        xCoords.addAll(histogram.keySet());
        Collections.sort(xCoords);
        // Get the labels for the histogram keys
        final List<String> xCoordLabels = new ArrayList<>();
        xCoords.forEach(e -> xCoordLabels.add(e.toString()));

        buildHeader();
        buildPlotConfig(xLabel, yLabel, xCoordLabels, displayRawNumbers);
        buildHistogram(histogram);
        buildFooter();

        return this.sb.toString();
    }
}
