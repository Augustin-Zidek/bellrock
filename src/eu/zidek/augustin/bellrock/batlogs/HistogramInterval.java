package eu.zidek.augustin.bellrock.batlogs;

/**
 * A class to enable storing interval values in one bin in a histogram. The
 * comparator compares only the beginnings of two intervals -- i.e. the interval
 * that starts before the other interval is considered to be smaller.
 * 
 * @author Augustin Zidek
 * @param <K> The comparable types of the beginning and ending of the histogram
 *            intervals.
 *
 */
public class HistogramInterval<K extends Comparable<? super K>>
        implements Comparable<HistogramInterval<K>> {
    private K begin;
    private K end;

    /**
     * @param begin The beginning of the interval.
     * @param end The ending of the interval.
     */
    public HistogramInterval(final K begin, final K end) {
        this.begin = begin;
        this.end = end;
    }

    /**
     * @return The beginning of the interval.
     */
    public K getBegin() {
        return this.begin;
    }

    /**
     * @return The ending of the interval.
     */
    public K getEnd() {
        return this.end;
    }

    /**
     * @param begin The new value of the beginning of the interval.
     */
    public void setBegin(final K begin) {
        this.begin = begin;
    }

    /**
     * @param end The new value of the ending of the interval.
     */
    public void setEnd(final K end) {
        this.end = end;
    }

    @Override
    public int compareTo(final HistogramInterval<K> o) {
        return this.begin.compareTo(o.getBegin());
    }

    @Override
    public String toString() {
        if (this.begin.equals(this.end)) {
            return this.begin.toString();
        }
        else {
            return this.begin.toString() + "--" + this.end.toString();
        }
    }
}
