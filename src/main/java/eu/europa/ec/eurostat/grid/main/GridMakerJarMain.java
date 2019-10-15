/**
 * 
 */
package eu.europa.ec.eurostat.grid.main;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Geometry;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.europa.ec.eurostat.eurogeostat.CountriesUtil;
import eu.europa.ec.eurostat.eurogeostat.datamodel.Feature;
import eu.europa.ec.eurostat.eurogeostat.io.GeoJSONUtil;
import eu.europa.ec.eurostat.eurogeostat.io.SHPUtil;
import eu.europa.ec.eurostat.eurogeostat.io.SHPUtil.SHPData;
import eu.europa.ec.eurostat.grid.GridCell.GridCellGeometryType;
import eu.europa.ec.eurostat.grid.StatGrid;

/**
 * 
 * The class for the executable program.
 * 
 * @author julien Gaffuri
 *
 */
public class GridMakerJarMain {

	public static void main(String[] args) throws Exception {

		Options options = new Options();
		options.addOption(Option.builder("res").longOpt("resolution").desc("Optional. The grid resolution (pixel size). Note that the unit of measure is expected to be the same as the one of the coordinate reference system. Default: '100 000'.")
				.hasArg().argName("value").build());
		options.addOption(Option.builder("epsg").longOpt("epsgCode").desc("Optional. The EPSG code of the grid coordinate reference system. Default: '3035', corresponding to ETRS89-LAEA coordinate reference system.")
				.hasArg().argName("code").build());

		options.addOption(Option.builder("cnt").longOpt("countryCode").desc("The 2 letters code of the country to build the grid accross.")
				.hasArg().argName("code").build());
		//TODO envelope
		options.addOption(Option.builder("if").longOpt("inputFileFormat").desc("Optional. Input file format. Default: 'geojson'.")
				.hasArg().argName("shp or geojson").build());
		options.addOption(Option.builder("i").longOpt("inputFile").desc("Input file containing the geometry of the region to build the grid accross.")
				.hasArg().argName("file path").build());

		options.addOption(Option.builder("tol").longOpt("toleranceDistance").desc("Optional. A tolerance distance to keep the cells that are not too far from the specified region. Default: '0'.")
				.hasArg().argName("value").build());
		options.addOption(Option.builder("gt").longOpt("gridCellGeometryType").desc("Optional. The type of grid cell geometry: The surface representation (a square) or its center point. Default: 'SURFACE'.")
				.hasArg().argName("SURFACE or CENTER_POINT").build());

		options.addOption(Option.builder("of").longOpt("outputFileFormat").desc("Optional. Output file format. Default: 'geojson'.")
				.hasArg().argName("shp or geojson").build());
		options.addOption(Option.builder("o").longOpt("outputFile").desc("Optional. Output file (SHP format). Default: 'out.shp'.")
				.hasArg().argName("file path").build());

		options.addOption(Option.builder("h").desc("Show this help message").build());

		CommandLine cmd = null;
		try { cmd = new DefaultParser().parse( options, args); } catch (ParseException e) {
			System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
			return;
		}

		//help statement
		if(cmd.hasOption("h")) {
			new HelpFormatter().printHelp("java -jar searoute.jar", options);
			return;
		}

		System.out.println("Read parameters...");
		String param;

		StatGrid sg = new StatGrid();

		//res
		param = cmd.getOptionValue("res");
		try { if(param != null) sg.setResolution(Integer.parseInt(param));
		} catch (Exception e) { System.err.println("Failed reading parameter 'res'. The default value will be used."); }

		//epsg
		CoordinateReferenceSystem crs = null;
		param = cmd.getOptionValue("epsg");
		try { if(param != null) sg.setEPSGCode(param);
		} catch (Exception e) { System.err.println("Failed reading parameter 'epsg'. The default value will be used."); }
		crs = CRS.parseWKT(WKT_3035);

		//cnt
		param = cmd.getOptionValue("cnt");
		try { if(param != null) {
			Feature cnt = CountriesUtil.getEuropeanCountry(param, true);
			Geometry geomToCover = cnt.getDefaultGeometry();
			sg.setGeometryToCover(geomToCover);
		}
		} catch (Exception e) { System.err.println("Failed finding country with parameter 'cnt': " + param); }

		//if
		String inputFileFormat = cmd.getOptionValue("if");
		if(inputFileFormat == null)
			inputFileFormat = "geojson";
		else if(!"geojson".equals(inputFileFormat) && !"shp".equals(inputFileFormat)) {
			System.out.println("Unexpected input file format: "+inputFileFormat+". 'geojson' will be used.");
			inputFileFormat = "geojson";
		}

		//i
		param = cmd.getOptionValue("i");
		try { if(param != null) {

			ArrayList<Feature> fs = null;
			if("shp".equals(inputFileFormat)) {
				SHPData sh = SHPUtil.loadSHP(param);
				fs = sh.fs;
				crs = sh.ft.getCoordinateReferenceSystem();
				System.out.println(crs);
				System.out.println(SHPUtil.getCRS(param));
			} else {
				fs = GeoJSONUtil.load(param);
				crs = GeoJSONUtil.loadCRS(param);
			}

			if(fs.size() == 0)
				System.err.println("Input file is empty: " + param);
			else {
				if(fs.size() > 1)
					System.err.println("Input file contains more than one geometry (nb="+fs.size()+"): " + param);
				Geometry geomToCover = fs.iterator().next().getDefaultGeometry();
				sg.setGeometryToCover(geomToCover);
			}
		}
		} catch (Exception e) { System.err.println("Failed reading input geometry from file: " + param); }

		//tol
		param = cmd.getOptionValue("tol");
		try { if(param != null) sg.setToleranceDistance(Double.parseDouble(param));
		} catch (Exception e) { System.err.println("Failed reading parameter 'tol'. The default value will be used."); }

		//gt
		param = cmd.getOptionValue("gt");
		try { if(param != null) {
			if(param == GridCellGeometryType.SURFACE.toString()) sg.setGridCellGeometryType(GridCellGeometryType.SURFACE);
			if(param == GridCellGeometryType.CENTER_POINT.toString()) sg.setGridCellGeometryType(GridCellGeometryType.CENTER_POINT);
			else throw new Exception();
		}
		} catch (Exception e) { System.err.println("Failed reading parameter 'gt'. The default value will be used."); }

		//of
		String outputFileFormat = cmd.getOptionValue("of");
		if(outputFileFormat == null)
			outputFileFormat = "geojson";
		else if(!"geojson".equals(outputFileFormat) && !"shp".equals(outputFileFormat)) {
			System.out.println("Unexpected output file format: "+outputFileFormat+". 'geojson' will be used.");
			outputFileFormat = "geojson";
		}

		//output file
		String outFile = cmd.getOptionValue("o");
		if(outFile == null) outFile = Paths.get("").toAbsolutePath().toString() + "/out."+outputFileFormat;


		System.out.println("Build grid...");
		Collection<Feature> cells = sg.getCells();
		System.out.println(cells.size() + " grid cells built.");

		System.out.println("Save as " + outFile + "...");
		//save
		if("shp".equals(inputFileFormat))
			SHPUtil.saveSHP(cells, outFile, crs);
		else
			GeoJSONUtil.save(cells, outFile, crs);

	}


	private static final String WKT_3035 = "PROJCS[\"ETRS89 / ETRS-LAEA\",\r\n" + 
			"    GEOGCS[\"ETRS89\",\r\n" + 
			"        DATUM[\"European_Terrestrial_Reference_System_1989\",\r\n" + 
			"            SPHEROID[\"GRS 1980\",6378137,298.257222101,\r\n" + 
			"                AUTHORITY[\"EPSG\",\"7019\"]],\r\n" + 
			"            AUTHORITY[\"EPSG\",\"6258\"]],\r\n" + 
			"        PRIMEM[\"Greenwich\",0,\r\n" + 
			"            AUTHORITY[\"EPSG\",\"8901\"]],\r\n" + 
			"        UNIT[\"degree\",0.01745329251994328,\r\n" + 
			"            AUTHORITY[\"EPSG\",\"9122\"]],\r\n" + 
			"        AUTHORITY[\"EPSG\",\"4258\"]],\r\n" + 
			"    UNIT[\"metre\",1,\r\n" + 
			"        AUTHORITY[\"EPSG\",\"9001\"]],\r\n" + 
			"    PROJECTION[\"Lambert_Azimuthal_Equal_Area\"],\r\n" + 
			"    PARAMETER[\"latitude_of_center\",52],\r\n" + 
			"    PARAMETER[\"longitude_of_center\",10],\r\n" + 
			"    PARAMETER[\"false_easting\",4321000],\r\n" + 
			"    PARAMETER[\"false_northing\",3210000],\r\n" + 
			"    AUTHORITY[\"EPSG\",\"3035\"],\r\n" + 
			"    AXIS[\"X\",EAST],\r\n" + 
			"    AXIS[\"Y\",NORTH]]";

}
