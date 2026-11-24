package cvrecruiter.parsing;

import cvrecruiter.common.NotFoundException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/** Kết xuất trang PDF thành ảnh PNG bằng PDFBox. */
public class PdfBoxCvRenderer implements CvRenderer {

    /** Đủ nét để đọc, không quá nặng. */
    private static final float RENDER_DPI = 110f;

    @Override
    public int pageCount(Path cvFile) {
        try (PDDocument doc = Loader.loadPDF(cvFile.toFile())) {
            return doc.getNumberOfPages();
        } catch (IOException e) {
            throw new UncheckedIOException("Khong doc duoc file CV.", e);
        }
    }

    @Override
    public byte[] renderPage(Path cvFile, int pageNumber) {
        try (PDDocument doc = Loader.loadPDF(cvFile.toFile())) {
            if (pageNumber < 1 || pageNumber > doc.getNumberOfPages()) {
                throw new NotFoundException("Trang khong ton tai trong CV.");
            }
            BufferedImage image = new PDFRenderer(doc).renderImageWithDPI(pageNumber - 1, RENDER_DPI);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Khong ket xuat duoc trang CV.", e);
        }
    }
}
