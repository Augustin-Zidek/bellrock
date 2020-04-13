package eu.zidek.augustin.bellrock.celltowerapi;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Processes the Cell Tower database and converts it into a hash map, so that it
 * can be very efficiently accessed during run time. This class is very memory
 * hungry, use with -Xmx1300m.
 * 
 * @author Augustin Zidek
 *
 */
class OpenCellIDDatabaseProcessor {

	public static void main(String[] args) throws IOException {
		final String input = "c:\\Users\\augustin\\Repositories\\Bellrock-server\\cell_towers_db\\cell_towers_filtered.csv";
		final String output = "c:\\Users\\augustin\\Repositories\\Bellrock-server\\cell_towers_db\\cell_towers_hashmap.dat";
		final Map<Long, CoarseLocation> cellTowerLocations = new HashMap<>(
				8_660_346);

		System.out.println("Started");
		try (final BufferedReader br = Files
				.newBufferedReader(Paths.get(input));) {

			int count = 0;
			String line = "";
			while ((line = br.readLine()) != null) {
				// Show progress bar
				count++;
				if (count % 866_034 == 0) {
					System.out.print("-");
				}

				final String[] fields = line.split(" ");

				// Parse the cell tower information
				final short mcc = Short.parseShort(fields[0]);
				final short mnc = Short.parseShort(fields[1]);
				final int lac = Integer.parseInt(fields[2]);
				final int cid = Integer.parseInt(fields[3]);
				final CellTower ctInfo = new CellTower(mcc, mnc, lac, cid);

				// Parse the cell tower location and round to float precision
				// Watch out: The XML stores (lon, lat), Bellrock uses (lat, lon)
				final float lat = (float) Double.parseDouble(fields[5]);
				final float lon = (float) Double.parseDouble(fields[4]);
				final CoarseLocation ctLoc = new CoarseLocation(lat, lon);

				cellTowerLocations.put(ctInfo.pack(), ctLoc);
			}
		}
		System.out.println();
		System.out.println("File read and parsed.");

		// Save the hash map
		try (final ObjectOutputStream objOutStream = new ObjectOutputStream(
				new BufferedOutputStream(new FileOutputStream(output)));) {
			objOutStream.writeObject(cellTowerLocations);
		}

		System.out.println("HashMap saved.");

		// Test the performance of the hash map
		final long start = System.currentTimeMillis();
		final CoarseLocation loc = cellTowerLocations.get(new CellTower(
				(short) 234, (short) 15, 6283, 74).pack());
		System.out.println(loc.getLatitude());
		System.out.println(loc.getLongitude());
		final long end = System.currentTimeMillis();
		System.out.println("Took " + (end - start));
	}
}
