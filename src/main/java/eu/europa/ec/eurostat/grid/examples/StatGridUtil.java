/**
 * 
 */
package eu.europa.ec.eurostat.grid.examples;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.index.strtree.STRtree;

import eu.europa.ec.eurostat.jgiscotools.feature.Feature;
import eu.europa.ec.eurostat.jgiscotools.util.Util;

/**
 * A number of functions to assign country codes to grid cells.
 * 
 * @author julien Gaffuri
 *
 */
public class StatGridUtil {
	static Logger logger = Logger.getLogger(StatGridUtil.class.getName());


	/**
	 * Assign country codes to grid cells.
	 * If a grid cell intersects or is nearby the geometry of a country, then an attribute of the cell is assigned with this country code.
	 * For cells that are to be assigned to several countries, several country codes are assigned.
	 * 
	 * @param cells
	 * @param cellCountryAttribute
	 * @param countries
	 * @param toleranceDistance
	 * @param countryIdAttribute
	 */
	public static void assignCountries(Collection<Feature> cells, String cellCountryAttribute, Collection<Feature> countries, double toleranceDistance, String countryIdAttribute) {
		if(logger.isDebugEnabled()) logger.debug("Assign country...");

		//initialise cell country attribute
		for(Feature cell : cells)
			cell.setAttribute(cellCountryAttribute, "");

		//index cells
		STRtree index = new STRtree();
		for(Feature cell : cells)
			index.insert(cell.getDefaultGeometry().getEnvelopeInternal(), cell);

		for(Feature cnt : countries) {
			//get country cover and code
			Geometry cntCover = cnt.getDefaultGeometry();
			if(toleranceDistance != 0 ) cntCover = cntCover.buffer(toleranceDistance);
			String cntCode = cnt.getAttribute(countryIdAttribute).toString();

			//get country envelope, expanded by toleranceDistance
			Envelope cntCoverEnv = cntCover.getEnvelopeInternal();

			//get grid cells around country envelope
			for(Object cell_ : index.query(cntCoverEnv)) {
				Feature cell = (Feature)cell_;
				Geometry cellGeom = cell.getDefaultGeometry();

				if( ! cntCoverEnv.intersects(cellGeom.getEnvelopeInternal()) ) continue;
				if( ! cntCover.intersects(cellGeom) ) continue;

				String att = cell.getAttribute(cellCountryAttribute).toString();
				if("".equals(att))
					cell.setAttribute(cellCountryAttribute, cntCode);
				else
					cell.setAttribute(cellCountryAttribute, att+"-"+cntCode);
			}
		}

	}


	/**
	 * Remove cells which are not assigned to any country,
	 * that is the ones with attribute 'cellCountryAttribute' null or set to "".
	 * 
	 * @param cells
	 * @param cellCountryAttribute
	 */
	public static void filterCellsWithoutCountry(Collection<Feature> cells, String cellCountryAttribute) {
		if(logger.isDebugEnabled()) logger.debug("Filtering " + cells.size() + " cells...");
		Collection<Feature> cellsToRemove = new ArrayList<Feature>();
		for(Feature cell : cells) {
			Object cellCnt = cell.getAttribute(cellCountryAttribute);
			if(cellCnt==null || "".equals(cellCnt.toString())) cellsToRemove.add(cell);
		}
		cells.removeAll(cellsToRemove);
		if(logger.isDebugEnabled()) logger.debug(cellsToRemove.size() + " cells to remove. " + cells.size() + " cells left");
	}


	/**
	 * Compute for each cell the proportion of its area which is land area.
	 * The value is stored as a new attribute for each cell. This value is a percentage.
	 * 
	 * @param cells
	 * @param cellLandPropAttribute
	 * @param landGeometry
	 * @param decimalNB The number of decimal places to keep for the percentage
	 */
	public static void assignLandProportion(Collection<Feature> cells, String cellLandPropAttribute, Geometry landGeometry, int decimalNB) {
		if(logger.isDebugEnabled()) logger.debug("Assign land proportion...");

		//compute cell area once
		double cellArea = cells.iterator().next().getDefaultGeometry().getArea();

		for(Feature cell : cells) {
			Geometry inter = cell.getDefaultGeometry().intersection(landGeometry); //TODO test if other way around is quicker
			double prop = 100.0 * inter.getArea() / cellArea;
			prop = Util.round(prop, decimalNB);
			cell.setAttribute(cellLandPropAttribute, prop);
		}

	}

}
