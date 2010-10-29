package divestoclimb.scuba.equipment.storage;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.sax.Element;
import android.sax.EndElementListener;
import android.sax.RootElement;
import android.sax.StartElementListener;
import android.util.Xml;

import com.megginson.sax.DataWriter;

import divestoclimb.android.database.CursorSet;
import divestoclimb.lib.scuba.Cylinder;
import divestoclimb.util.Formatting;

public class XmlMapper {

	public static final File DEFAULT_PATH = new File(Environment.getExternalStorageDirectory(), "data");
	public static final String DEFAULT_FILENAME = "scubatanks.xml";

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMM");
	private static final NumberFormat NUMBER_FORMAT = Formatting.buildNormalizedFormat("#.####");

	private CylinderORMapper mORMapper;
	private DataWriter mCurrentDocument = null;

	/**
	 * Create an XmlMapper. Constructs a CylinderORMapper using the given context.
	 * @param ctx The context to use for the ORMapper
	 */
	public XmlMapper(Context ctx) {
		mORMapper = new CylinderORMapper(ctx);
	}

	/**
	 * Create an XmlMapper with a custom CylinderORMapper.
	 * @param mapper The ORMapper to use. Remember, the units set on this ORMapper will
	 * determine the units used in XML output and parsing. Creating a new instance with
	 * defaults is the safest bet.
	 */
	public XmlMapper(CylinderORMapper mapper) {
		mORMapper = mapper;
	}
	
	public Reader getDefaultReader() throws IOException {
		return new FileReader(new File(DEFAULT_PATH, DEFAULT_FILENAME));
	}
	
	public Writer getDefaultWriter() throws IOException {
		if(! DEFAULT_PATH.isDirectory() && ! DEFAULT_PATH.mkdirs()) {
			throw new IOException("Could not write to default location " + DEFAULT_PATH.getAbsolutePath());
		}
		return new FileWriter(new File(DEFAULT_PATH, DEFAULT_FILENAME));
	}
	
	/**
	 * Initialize the DataWriter if we're starting a new document
	 * @param out The Writer to direct output to
	 * @return true if a new DataWriter was created (in which case you must
	 * call closeWriter when done) or false if a document is already being written.
	 * @throws SAXException
	 */
	private boolean openWriter(Writer out) throws SAXException {
		// If a document is not already in progress, create one
		if(mCurrentDocument == null) {
			mCurrentDocument = new DataWriter(out);
			mCurrentDocument.startDocument();
			return true;
		}
		return false;
	}

	private void closeWriter() {
		try {
			mCurrentDocument.endDocument();
		} catch (SAXException e) { }
		mCurrentDocument = null;
	}
	
	public boolean writeCylinders(Writer out) {
		final CursorSet<Cylinder> cylinders;
		try {
			cylinders = new CursorSet<Cylinder>(mORMapper.fetchCylinders(), mORMapper);
		} catch(NullPointerException e) {
			// Nothing to write
			return true;
		}
		boolean opened = false;
		try {
			opened = openWriter(out);
			DataWriter dw = mCurrentDocument;
			dw.startElement("cylinders");
			for(Cylinder c : cylinders) {
				final AttributesImpl atts = new AttributesImpl();
				final int rawType = c.getType();
				final String type = rawType == Cylinder.TYPE_GENERIC? "generic": "specific";
				atts.addAttribute("", "type", "", "CDATA", type);
				atts.addAttribute("", "name", "", "CDATA", c.getName());
				atts.addAttribute("", "internal_volume_l", "", "CDATA", NUMBER_FORMAT.format(c.getInternalVolume()));
				atts.addAttribute("", "service_pressure_bar", "", "CDATA", NUMBER_FORMAT.format(c.getServicePressure()));
				if(rawType == Cylinder.TYPE_SPECIFIC) {
					if(c.getSerialNumber() != null) {
						atts.addAttribute("", "serial_number", "", "CDATA", c.getSerialNumber());
					}
				}
				dw.startElement("", "cylinder", "", atts);
				if(rawType == Cylinder.TYPE_SPECIFIC) {
					dw.startElement("hydro_tests");
					if(c.getLastHydro() != null) {
						final AttributesImpl hydroAtts = new AttributesImpl();
						hydroAtts.addAttribute("", "date", "", "CDATA", DATE_FORMAT.format(c.getLastHydro()));
						dw.emptyElement("", "hydro_test", "", hydroAtts);
					}
					dw.endElement("hydro_tests");
					dw.startElement("visual_inspections");
					if(c.getLastHydro() != null) {
						final AttributesImpl visualAtts = new AttributesImpl();
						visualAtts.addAttribute("", "date", "", "CDATA", DATE_FORMAT.format(c.getLastVisual()));
						dw.emptyElement("", "visual_inspection", "", visualAtts);
					}
					dw.endElement("visual_inspections");
				}
				dw.endElement("cylinder");
			}
			dw.endElement("cylinders");
			return true;
		} catch(SAXException e) {
			return false;
		} finally {
			if(opened) {
				closeWriter();
			}
		}
	}

	// For parsing, the current Cylinder object being parsed
	private Cylinder mCylinder;

	private class CylinderReader implements StartElementListener, EndElementListener {

		@Override
		public void start(Attributes attributes) {
			mCylinder = new Cylinder(mORMapper.getUnits(),
					Float.valueOf(attributes.getValue("internal_volume_l")).floatValue(),
					Integer.valueOf(attributes.getValue("service_pressure_bar")).intValue());
			final int type = attributes.getValue("type").equals("specific")? Cylinder.TYPE_SPECIFIC: Cylinder.TYPE_GENERIC;
			mCylinder.setType(type);
			mCylinder.setName(attributes.getValue("name"));
			if(type == Cylinder.TYPE_SPECIFIC) {
				mCylinder.setSerialNumber(attributes.getValue("serial_number"));
			}
			matchCylinder(mCylinder);
		}
		
		private void matchCylinder(Cylinder c) {
			// Attempt to find a matching cylinder in the database. If there's a match,
			// give this new one the existing one's ID so it gets replaced
			Cursor matches = null;
			// 1. Match serial number if exists
			if(c.getSerialNumber() != null) {
				matches = mORMapper.fetchCylindersBySerialNumber(c.getSerialNumber());
				if(matches != null && matches.getCount() == 1) {
					matches.moveToFirst();
					c.setId(matches.getLong(matches.getColumnIndexOrThrow(CylinderORMapper._ID)));
					return;
				}
			} else {
				// 2. Try match by name
				matches = mORMapper.fetchCylindersByName(c.getName());
				if(matches != null && matches.getCount() == 1) {
					matches.moveToFirst();
					c.setId(matches.getLong(matches.getColumnIndexOrThrow(CylinderORMapper._ID)));
				}
			}
		}

		@Override
		public void end() {
			mORMapper.save(mCylinder);
		}
	}
	
	private class TestReader implements StartElementListener {

		private String mType;

		public TestReader(String type) {
			mType = type;
		}

		@Override
		public void start(Attributes attributes) {
			try {
				final Date date = DATE_FORMAT.parse(attributes.getValue("date"));
				if(mType.equals("hydro")) {
					mCylinder.setLastHydro(date);
				} else if(mType.equals("visual")) {
					mCylinder.setLastVisual(date);
				}
			} catch(ParseException e) {
			}
		}
		
	}
	
	public boolean readCylinders(Reader in) {
		RootElement root = new RootElement("cylinders");
		Element cylinder = root.getChild("cylinder");

		CylinderReader cr = new CylinderReader();
		cylinder.setStartElementListener(cr);
		cylinder.setEndElementListener(cr);
		cylinder.getChild("hydro_tests").getChild("hydro_test")
			.setStartElementListener(new TestReader("hydro"));
		cylinder.getChild("visual_inspections").getChild("visual_inspection")
			.setStartElementListener(new TestReader("visual"));

		try {
			Xml.parse(in, root.getContentHandler());
			return true;
		} catch(SAXException e) {
			return false;
		} catch(IOException e) {
			return false;
		}
	}
}