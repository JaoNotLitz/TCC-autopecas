package br.com.wcautopecas.fiscal.xml;

import java.io.StringWriter;
import java.util.List;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * XMLDSig enveloped no padrão da SEFAZ (HU-B08.3): RSA-SHA1, digest SHA1, C14N, transforms
 * enveloped + C14N, {@code KeyInfo/X509Data/X509Certificate}. Assina {@code infNFe},
 * {@code infEvento} ou {@code infInut}: o elemento com atributo {@code Id}.
 */
@Component
public class AssinadorXml {

    private final XMLSignatureFactory fabrica = XMLSignatureFactory.getInstance("DOM");

    /** Assina o elemento {@code elemento} (pelo seu Id) e põe a {@code Signature} no fim de {@code onde}. */
    public void assinar(Document doc, String elemento, Element onde, CertificadoA1.Credencial cred) {
        try {
            NodeList nos = doc.getElementsByTagNameNS(NfeXmlBuilder.NS, elemento);
            Element alvo = (Element) nos.item(0);
            alvo.setIdAttribute("Id", true);
            String id = alvo.getAttribute("Id");

            List<Transform> transforms = List.of(
                    fabrica.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null),
                    fabrica.newTransform(CanonicalizationMethod.INCLUSIVE, (TransformParameterSpec) null));
            Reference ref = fabrica.newReference("#" + id, fabrica.newDigestMethod(DigestMethod.SHA1, null),
                    transforms, null, null);
            SignedInfo si = fabrica.newSignedInfo(
                    fabrica.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                    fabrica.newSignatureMethod(SignatureMethod.RSA_SHA1, null), List.of(ref));
            KeyInfoFactory kif = fabrica.getKeyInfoFactory();
            var keyInfo = kif.newKeyInfo(List.of(kif.newX509Data(List.of(cred.certificado()))));
            fabrica.newXMLSignature(si, keyInfo).sign(new DOMSignContext(cred.chave(), onde));
        } catch (Exception e) {
            throw new CertificadoA1.CertificadoException("falha ao assinar o XML: " + e.getMessage());
        }
    }

    /** Confere assinatura e digest (usado nos testes e no simulador da SEFAZ). */
    public boolean valida(Document doc) {
        try {
            NodeList sigs = doc.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
            if (sigs.getLength() == 0) {
                return false;
            }
            for (String tag : List.of("infNFe", "infEvento", "infInut")) {
                NodeList nos = doc.getElementsByTagNameNS(NfeXmlBuilder.NS, tag);
                if (nos.getLength() > 0) {
                    ((Element) nos.item(0)).setIdAttribute("Id", true);
                }
            }
            var ctx = new DOMValidateContext(new X509KeySelector(), sigs.item(0));
            ctx.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.FALSE); // SHA1 é o que a SEFAZ exige
            return fabrica.unmarshalXMLSignature(ctx).validate(ctx);
        } catch (Exception e) {
            return false;
        }
    }

    public static String texto(Document doc) {
        try {
            var t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            t.setOutputProperty(OutputKeys.INDENT, "no");
            StringWriter w = new StringWriter();
            t.transform(new DOMSource(doc), new StreamResult(w));
            return w.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
