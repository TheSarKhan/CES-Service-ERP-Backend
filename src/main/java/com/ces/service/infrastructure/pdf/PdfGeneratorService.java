package com.ces.service.infrastructure.pdf;

import com.ces.service.common.exception.BusinessException;
import com.ces.service.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.Map;

/**
 * Renders dynamic Thymeleaf HTML templates to PDF via Flying Saucer
 * ({@link ITextRenderer}) — SRS §M10.2.
 *
 * <p>Templates are supplied as raw HTML strings (stored per branch in
 * {@code document_templates.template_html}), so a dedicated string-based template
 * engine is used rather than the application's file-based one.</p>
 */
@Service
public class PdfGeneratorService {

    private final TemplateEngine stringTemplateEngine;

    public PdfGeneratorService() {
        StringTemplateResolver resolver = new StringTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCacheable(false);

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        this.stringTemplateEngine = engine;
    }

    /**
     * Process the given Thymeleaf HTML template with the supplied variables and
     * render the resulting XHTML to a PDF byte array.
     *
     * @param templateHtml raw Thymeleaf/HTML template
     * @param data         template variables (e.g. company, wo, vehicle, customer)
     * @return the generated PDF bytes
     */
    public byte[] generate(String templateHtml, Map<String, Object> data) {
        Context ctx = new Context();
        if (data != null) {
            ctx.setVariables(data);
        }
        String html = stringTemplateEngine.process(templateHtml, ctx);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "PDF generation failed: " + e.getMessage());
        }
    }
}
