package fr.edu.lyon.pdfmerge.pdf.services;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.transform.TransformerException;

import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.util.Matrix;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PdfService {

	public List<InputStream> normalizeSources(final List<File> sources) {
		Tika tika = new Tika();
		List<InputStream> dest = new ArrayList<InputStream>();
		for (File file : sources) {
			try {
				String mimeType = "";
				mimeType = tika.detect(file);
				if (mimeType.equals("application/pdf")) {
					dest.add(new FileInputStream(file));
				} else {
					dest.add(createPDFFromImage(file));
				}
			} catch (IOException e) {
				log.error("io error", e);
			}
		}

		return dest;
	}

	/**
	 * Creates a compound PDF document from a list of input documents.
	 * <p>
	 * The merged document is PDF/A-1b compliant, provided the source documents are
	 * as well. It contains document properties title, creator and subject,
	 * currently hard-coded.
	 *
	 * @param sources list of source PDF document streams.
	 * @return compound PDF document as a readable input stream.
	 * @throws IOException   if anything goes wrong during PDF merge.
	 * @throws TikaException
	 */
	public InputStream merge(final List<InputStream> sources) throws IOException, TikaException {

		String title = "Merged PDF";
		String creator = "AC Lyon";
		String subject = "Subject";

		try (COSStream cosStream = new COSStream();
				ByteArrayOutputStream mergedPDFOutputStream = new ByteArrayOutputStream()) {
			// If you're merging in a servlet, you can modify this example to use the
			// outputStream only
			// as the response as shown here: http://stackoverflow.com/a/36894346/535646

			PDFMergerUtility pdfMerger = createPDFMergerUtility(sources, mergedPDFOutputStream);

			// PDF and XMP properties must be identical, otherwise document is not PDF/A
			// compliant
			PDDocumentInformation pdfDocumentInfo = createPDFDocumentInfo(title, creator, subject);
			PDMetadata xmpMetadata = createXMPMetadata(cosStream, title, creator, subject);
			pdfMerger.setDestinationDocumentInformation(pdfDocumentInfo);
			pdfMerger.setDestinationMetadata(xmpMetadata);

			log.info("Merging " + sources.size() + " source documents into one PDF");
			pdfMerger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
			log.info("PDF merge successful, size = {" + mergedPDFOutputStream.size() + "} bytes");

			return new ByteArrayInputStream(mergedPDFOutputStream.toByteArray());
		} catch (BadFieldValueException | TransformerException e) {
			throw new IOException("PDF merge problem", e);
		} finally {
			sources.forEach(org.apache.pdfbox.io.IOUtils::closeQuietly);
		}
	}

	private PDFMergerUtility createPDFMergerUtility(List<InputStream> sources,
			ByteArrayOutputStream mergedPDFOutputStream) {
		log.info("Initialising PDF merge utility");
		PDFMergerUtility pdfMerger = new PDFMergerUtility();
		pdfMerger.addSources(sources);
		pdfMerger.setDestinationStream(mergedPDFOutputStream);
		return pdfMerger;
	}

	private PDDocumentInformation createPDFDocumentInfo(String title, String creator, String subject) {
		log.info("Setting document info (title, author, subject) for merged PDF");
		PDDocumentInformation documentInformation = new PDDocumentInformation();
		documentInformation.setTitle(title);
		documentInformation.setCreator(creator);
		documentInformation.setSubject(subject);
		return documentInformation;
	}

	private PDMetadata createXMPMetadata(COSStream cosStream, String title, String creator, String subject)
			throws BadFieldValueException, TransformerException, IOException {
		log.info("Setting XMP metadata (title, author, subject) for merged PDF");
		XMPMetadata xmpMetadata = XMPMetadata.createXMPMetadata();

		// PDF/A-1b properties
		PDFAIdentificationSchema pdfaSchema = xmpMetadata.createAndAddPFAIdentificationSchema();
		pdfaSchema.setPart(1);
		pdfaSchema.setConformance("B");

		// Dublin Core properties
		DublinCoreSchema dublinCoreSchema = xmpMetadata.createAndAddDublinCoreSchema();
		dublinCoreSchema.setTitle(title);
		dublinCoreSchema.addCreator(creator);
		dublinCoreSchema.setDescription(subject);

		// XMP Basic properties
		XMPBasicSchema basicSchema = xmpMetadata.createAndAddXMPBasicSchema();
		Calendar creationDate = Calendar.getInstance();
		basicSchema.setCreateDate(creationDate);
		basicSchema.setModifyDate(creationDate);
		basicSchema.setMetadataDate(creationDate);
		basicSchema.setCreatorTool(creator);

		// Create and return XMP data structure in XML format
		try (ByteArrayOutputStream xmpOutputStream = new ByteArrayOutputStream();
				OutputStream cosXMPStream = cosStream.createOutputStream()) {
			new XmpSerializer().serialize(xmpMetadata, xmpOutputStream, true);
			cosXMPStream.write(xmpOutputStream.toByteArray());
			return new PDMetadata(cosStream);
		}
	}

	public InputStream createPDFFromImage(File image) throws IOException {
		BufferedImage awtImage = ImageIO.read(image);
		
		
		// check if horizontal or vertical
		Boolean isHorizontal = false;
		if (awtImage.getWidth() > awtImage.getHeight()) {
			isHorizontal = true;
		}

		// get actual height and width of pdf page 'cause pdfbox sees page always as
		// vertical and only saves the rotation
		// ....-------------------
		// ...|...................|
		// ...|.........A4........|...0.x
		// ...|......PDF.page.....|..0y-|----------------------------
		// ...|......vertical.....|.....|............A4..............|
		// ...|...._________......|.....|.........PDF.page...........|
		// ...|...(.........).....|.....|........horizontal..........|
		// ...|...(..image..).....|.....|...._______________.........|
		// ...|...(.........).....|.....|...(...............)........|
		// ...|...(.........).....|.....|...(....image......)........|
		// ...|...(.........).....|.....|...(_______________)........|
		// ...|...(_________).....|.....|----------------------------
		// 0x-|-------------------
		// ..0y
		int actualPDFWidth = 0;
		int actualPDFHeight = 0;
		if (isHorizontal) {
			actualPDFWidth = (int) PDRectangle.A4.getHeight();
			actualPDFHeight = (int) PDRectangle.A4.getWidth();
		} else {
			actualPDFWidth = (int) PDRectangle.A4.getWidth();
			actualPDFHeight = (int) PDRectangle.A4.getHeight();
		}

		PDDocument doc = new PDDocument();
		doc.addPage(new PDPage());
		PDPage page = doc.getPage(0);
		PDImageXObject pdImage = PDImageXObject.createFromFileByContent(image, doc);

		PDPageContentStream contentStream = new PDPageContentStream(doc, page);

		// scale image
		Dimension scaledDim = getScaledDimension(new Dimension(pdImage.getWidth(), pdImage.getHeight()),
				new Dimension(actualPDFWidth, actualPDFHeight)); // I'm using this function:
																	// https://stackoverflow.com/questions/23223716/scaled-image-blurry-in-pdfbox

		// if horizontal rotate 90°, calculate position and draw on page
		if (isHorizontal) {
			int x = (int) PDRectangle.A4.getWidth() - (((int) PDRectangle.A4.getWidth() - scaledDim.height) / 2);
			int y = ((int) PDRectangle.A4.getHeight() - scaledDim.width) / 2;
			AffineTransform at = new AffineTransform(scaledDim.getHeight(), 0, 0, scaledDim.getWidth(), x, y);
			at.rotate(Math.toRadians(90));
			Matrix m = new Matrix(at);
			contentStream.drawImage(pdImage, m);
		} else {
			int x = ((int) PDRectangle.A4.getWidth() - scaledDim.width) / 2;
			int y = ((int) PDRectangle.A4.getHeight() - scaledDim.height) / 2;
			contentStream.drawImage(pdImage, x, y, scaledDim.width, scaledDim.height);
		}

		contentStream.close();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		doc.save(out);
		doc.close();
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
		return in;		
	}

	public static Dimension getScaledDimension(Dimension imgSize, Dimension boundary) {

		int original_width = imgSize.width;
		int original_height = imgSize.height;
		int bound_width = boundary.width;
		int bound_height = boundary.height;
		int new_width = original_width;
		int new_height = original_height;

		// first check if we need to scale width
		if (original_width > bound_width) {
			// scale width to fit
			new_width = bound_width;
			// scale height to maintain aspect ratio
			new_height = (new_width * original_height) / original_width;
		}

		// then check if we need to scale even with the new height
		if (new_height > bound_height) {
			// scale height to fit instead
			new_height = bound_height;
			// scale width to maintain aspect ratio
			new_width = (new_height * original_width) / original_height;
		}

		return new Dimension(new_width, new_height);
	}
}
