package br.com.wcautopecas.fiscal.xml;

import java.io.StringReader;
import java.net.URL;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXParseException;

/**
 * Validação contra o {@code nfe_v4.00.xsd} oficial (pacote PL_009, em {@code resources/xsd})
 * (HU-B08.4). O {@link Schema} é compilado uma vez; cada validação usa um {@link Validator} novo.
 */
@Component
public class ValidadorXsd {

    private final Schema schema;

    public ValidadorXsd() {
        try {
            URL xsd = getClass().getResource("/xsd/nfe_v4.00.xsd");
            SchemaFactory f = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            this.schema = f.newSchema(xsd);
        } catch (Exception e) {
            throw new IllegalStateException("não foi possível carregar o XSD da NF-e", e);
        }
    }

    /** Primeira mensagem de erro do validador, ou vazio se o XML passa. */
    public Optional<String> validar(String xml) {
        try {
            Validator v = schema.newValidator();
            v.validate(new StreamSource(new StringReader(xml)));
            return Optional.empty();
        } catch (SAXParseException e) {
            return Optional.of(e.getMessage());
        } catch (Exception e) {
            return Optional.of(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
        }
    }
}
