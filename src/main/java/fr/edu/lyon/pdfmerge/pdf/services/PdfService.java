package fr.edu.lyon.pdfmerge.pdf.services;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
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
public class PdfService
{

    /**
     * Creates a compound PDF document from a list of input documents.
     * <p>
     * The merged document is PDF/A-1b compliant, provided the source documents are as well. It
     * contains document properties title, creator and subject, currently hard-coded.
     *
     * @param sources list of source PDF document streams.
     * @return compound PDF document as a readable input stream.
     * @throws IOException if anything goes wrong during PDF merge.
     */
    public InputStream merge(final List<InputStream> sources) throws IOException
    {
        String title = "My title";
        String creator = "Alexander Kriegisch";
        String subject = "Subject with umlauts Ã„Ã–Ãœ";

        try (COSStream cosStream = new COSStream();
             ByteArrayOutputStream mergedPDFOutputStream = new ByteArrayOutputStream())
        {
            // If you're merging in a servlet, you can modify this example to use the outputStream only
            // as the response as shown here: http://stackoverflow.com/a/36894346/535646

            PDFMergerUtility pdfMerger = createPDFMergerUtility(sources, mergedPDFOutputStream);

            // PDF and XMP properties must be identical, otherwise document is not PDF/A compliant
            PDDocumentInformation pdfDocumentInfo = createPDFDocumentInfo(title, creator, subject);
            PDMetadata xmpMetadata = createXMPMetadata(cosStream, title, creator, subject);
            pdfMerger.setDestinationDocumentInformation(pdfDocumentInfo);
            pdfMerger.setDestinationMetadata(xmpMetadata);

            log.info("Merging " + sources.size() + " source documents into one PDF");
            pdfMerger.mergeDocuments(MemoryUsageSetting.setupMainMemoryOnly());
            log.info("PDF merge successful, size = {" + mergedPDFOutputStream.size() + "} bytes");

            return new ByteArrayInputStream(mergedPDFOutputStream.toByteArray());
        }
        catch (BadFieldValueException | TransformerException e)
        {
            throw new IOException("PDF merge problem", e);
        }
        finally
        {
            sources.forEach(IOUtils::closeQuietly);
        }
    }

    private PDFMergerUtility createPDFMergerUtility(List<InputStream> sources, ByteArrayOutputStream mergedPDFOutputStream)
    {
        log.info("Initialising PDF merge utility");
        PDFMergerUtility pdfMerger = new PDFMergerUtility();
        pdfMerger.addSources(sources);
        pdfMerger.setDestinationStream(mergedPDFOutputStream);
        return pdfMerger;
    }

    private PDDocumentInformation createPDFDocumentInfo(String title, String creator, String subject)
    {
        log.info("Setting document info (title, author, subject) for merged PDF");
        PDDocumentInformation documentInformation = new PDDocumentInformation();
        documentInformation.setTitle(title);
        documentInformation.setCreator(creator);
        documentInformation.setSubject(subject);
        return documentInformation;
    }

    private PDMetadata createXMPMetadata(COSStream cosStream, String title, String creator, String subject)
            throws BadFieldValueException, TransformerException, IOException
    {
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
             OutputStream cosXMPStream = cosStream.createOutputStream())
        {
            new XmpSerializer().serialize(xmpMetadata, xmpOutputStream, true);
            cosXMPStream.write(xmpOutputStream.toByteArray());
            return new PDMetadata(cosStream);
        }
    }
}