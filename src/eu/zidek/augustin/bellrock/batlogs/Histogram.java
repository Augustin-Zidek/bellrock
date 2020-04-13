package eu.zidek.augustin.bellrock.batlogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.zidek.augustin.bellrock.batlogs.export.Histogram2TikZConvertor;
import eu.zidek.augustin.bellrock.batlogs.export.LaTeXExporter;

/**
 * Class for storing histograms -- a map of objects to their counts.
 * 
 * @author Augustin Zidek
 *
 * @param <K>
 */
public class Histogram<K extends Comparable<? super K>> {
    private final Map<K, Double> map;
    private double normalizationConst = 1;

    /**
     * Initializes a new empty histogram.
     */
    public Histogram() {
        this.map = new HashMap<>();
    }

    /**
     * Initializes a new histogram backed by the given map.
     * 
     * @param map The map backing the histogram.
     */
    public Histogram(final Map<K, Double> map) {
        this.map = map;
    }

    /**
     * Adds the given key into the histogram. That means, if the key is already
     * present in the histogram, its count is increased. If it is not, then it
     * is added into the histogram with count 1.
     * 
     * @param key The key.
     */
    public void add(final K key) {
        this.map.merge(key, 1D, (oldValue, value) -> oldValue + 1);
    }

    /**
     * Adds the given key multiple times into the histogram.
     * 
     * @param key The key.
     * @param value The value that should be added to the given key.
     */
    public void put(final K key, final double value) {
        this.map.merge(key, (double) value,
                (oldValue, newValue) -> oldValue + value);
    }

    /**
     * @param key The key.
     * @return The value corresponding to the given key.
     */
    public double get(final K key) {
        return this.map.get(key);
    }

    /**
     * @param key The key to be deleted from the histogram.
     */
    public void delete(final K key) {
        this.map.remove(key);
    }

    /**
     * Normalizes the histogram. After normalization, the sum of all values is
     * 1, hence the individual numbers represent frequencies. The normalization
     * can be reversed by denormalizing.
     */
    public void normalize() {
        double sum = 0;
        for (final Map.Entry<K, Double> entry : this.map.entrySet()) {
            sum += entry.getValue();
        }

        for (final Map.Entry<K, Double> entry : this.map.entrySet()) {
            entry.setValue(entry.getValue() / sum);
        }
        this.normalizationConst = sum;
    }

    /**
     * @param c The normalization constant by which all values in the histogram
     *            should be divided.
     */
    public void normalize(final double c) {
        for (final Map.Entry<K, Double> entry : this.map.entrySet()) {
            entry.setValue(entry.getValue() / c);
        }
        this.normalizationConst = c;
    }

    /**
     * Reverts the last normalization. This operation is valid only if called
     * directly after normalization.
     */
    public void denormalize() {
        for (final Map.Entry<K, Double> entry : this.map.entrySet()) {
            entry.setValue(entry.getValue() * this.normalizationConst);
        }
        this.normalizationConst = 1;
    }

    /**
     * @return A sorted list of the keys.
     */
    public List<K> getSortedKeys() {
        final List<K> keys = new ArrayList<>(this.map.size());

        for (final K key : this.map.keySet()) {
            keys.add(key);
        }
        Collections.sort(keys);

        return keys;
    }

    /**
     * @return The map underlying this histogram.
     */
    public Map<K, Double> getUnderlyingMap() {
        return this.map;
    }

    /**
     * @return The average of the histogram values. Watch out -- if normalized,
     *         the average of the normalized values is returned.
     */
    public double getAverage() {
        double sum = 0;
        for (final double binValue : this.map.values()) {
            sum += binValue;
        }
        return sum / this.map.size();
    }

    /**
     * @return The standard deviation (i.e.\ square root of the variance) of the
     *         histogram values. The unbiased variance is used:
     *         <code>s = sqrt(1/(n - 1) * sum[i = 1 to n]((xi - xavg)^2))</code>
     *         .
     */
    public double getStDev() {
        double sum = 0;
        final double average = this.getAverage();
        for (final double binValue : this.map.values()) {
            sum += (binValue - average) * (binValue - average);
        }
        return Math.sqrt(sum / (this.map.size() - 1));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        final List<K> keysSorted = this.getSortedKeys();
        for (final K key : keysSorted) {
            sb.append(String.format("%s: %s%n", key.toString(),
                    this.map.get(key).toString()));
        }
        return sb.toString();
    }

    /**
     * Like toString() but the format of each key-value pair can be specified.
     * The default format is <code>%s: %s%n</code>. However, in this case a
     * string-double pair is given, so the format has to be for instance
     * <code>%-6s: %f%n</code>.
     * 
     * @param format The format specifying the format of the string and of the
     *            double.
     * @return String representation of the histogram with key-value pairs
     *         formatted according to the given format string.
     */
    public String toString(final String format) {
        final StringBuilder sb = new StringBuilder();
        final List<K> keysSorted = this.getSortedKeys();
        for (final K key : keysSorted) {
            sb.append(String.format(format, key.toString(), this.map.get(key)));
        }
        return sb.toString();
    }

    /**
     * @param format The format specifying the format of the string and of the
     *            double.
     * @return A list of key-value pairs in this histogram, one per item in the
     *         list.
     */
    public List<String> toLinesString(final String format) {
        final List<String> lines = new ArrayList<>();
        final List<K> keysSorted = this.getSortedKeys();
        for (final K key : keysSorted) {
            lines.add(String.format(format, key.toString(), this.map.get(key)));
        }
        return lines;
    }

    /**
     * Exports the histogram in TikZ format that can be directly compiled into a
     * pdf with a bar graph.
     * 
     * @param xLabel The label of the x-axis.
     * @param yLabel The label of the y-axis.
     * @param normalized <code>true</code> if the histogram should be normalized
     *            before the export. The histogram then becomes normalized,
     *            which could be reverted by denormalizing! <code>false</code>
     *            otherwise.
     * @param displayRawNumbers <code>true</code> if the numbers should be left
     *            as they are, displayed as they are. <code>false</code> if the
     *            values in the histogram should be treated as frequencies that
     *            sum to one, then multiplying by 100 and displayed with a
     *            percent symbol.
     * @return A string containing the LaTeX source code to generate the
     *         histogram as a bar plot.
     */
    public String exportToTikZ(final String xLabel, final String yLabel,
            final boolean normalized, final boolean displayRawNumbers) {
        final Histogram2TikZConvertor convertor = new Histogram2TikZConvertor();
        if (normalized) {
            this.normalize();
        }

        final String tikzOutput = convertor.convert(this.map, xLabel, yLabel,
                displayRawNumbers);
        return tikzOutput;
    }

    /**
     * Exports the histogram in TikZ format, saves the LaTeX source code in a
     * file and compiles the file using pdflatex.
     * 
     * @param xLabel The label of the x-axis.
     * @param yLabel The label of the y-axis.
     * @param normalized <code>true</code> if the histogram should be normalized
     *            before the export. The histogram is then automatically
     *            denormalized. <code>false</code> otherwise.
     * @param displayRawNumbers <code>true</code> if the numbers should be left
     *            as they are, displayed as they are. <code>false</code> if the
     *            values in the histogram should be treated as frequencies that
     *            sum to one, then multiplying by 100 and displayed with a
     *            percent symbol.
     * @param path The path of the LaTeX file with the graph.
     * @return <code>true</code> if all operations were completed successfully,
     *         <code>false</code> otherwise.
     */
    public boolean saveAsTeXandPDF(final String xLabel, final String yLabel,
            final boolean normalized, final boolean displayRawNumbers,
            final String path) {
        final LaTeXExporter exporter = new LaTeXExporter();
        return exporter.saveAndCompile(path, this.exportToTikZ(xLabel, yLabel,
                normalized, displayRawNumbers));
    }

    /**
     * Cleans the histogram by grouping together bins with low frequencies. A
     * frequency is considered low when it is less than the given constant. The
     * constant is raw frequency, hence 0.01 ~ 1%.
     * 
     * @param minFrequency The minimum frequency of a bin in order not to group
     *            it.
     * @return The cleaned version of the histogram that has bin labeled by
     *         intervals.
     */
    public Histogram<HistogramInterval<K>> clean(final double minFrequency) {
        this.normalize();

        final List<K> keysSorted = this.getSortedKeys();
        final Histogram<HistogramInterval<K>> cleaned = new Histogram<>();

        double groupFreq = 0;
        K groupBegin = null;
        int cnt = 0;
        for (final K key : keysSorted) {
            cnt++;
            final double value = this.map.get(key);
            // Key has minimal frequency and there is no open interval
            if (groupFreq == 0 && value > minFrequency) {
                cleaned.put(new HistogramInterval<K>(key, key), value);
            }
            // Key doesn't have minimal frequency, grouping mode
            else {
                // First element of a group
                if (groupBegin == null) {
                    groupBegin = key;
                    groupFreq += value;
                }
                // Middle element of a group
                else if (groupFreq < minFrequency) {
                    groupFreq += value;
                }
                // The last element of a group
                if (groupFreq >= minFrequency || cnt == keysSorted.size()) {
                    final HistogramInterval<K> groupKey = new HistogramInterval<K>(
                            groupBegin, key);
                    cleaned.put(groupKey, groupFreq);
                    groupBegin = null;
                    groupFreq = 0;
                }
            }
        }
        this.denormalize();
        return cleaned;
    }

}
