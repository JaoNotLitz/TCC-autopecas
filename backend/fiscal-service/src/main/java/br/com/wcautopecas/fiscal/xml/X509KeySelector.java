package br.com.wcautopecas.fiscal.xml;

import java.security.Key;
import java.security.cert.X509Certificate;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;

/** Usa a chave pública do certificado que vem no próprio {@code KeyInfo}. */
class X509KeySelector extends KeySelector {

    @Override
    public KeySelectorResult select(KeyInfo keyInfo, Purpose purpose, AlgorithmMethod method, XMLCryptoContext ctx)
            throws KeySelectorException {
        for (Object item : keyInfo.getContent()) {
            if (item instanceof X509Data data) {
                for (Object o : data.getContent()) {
                    if (o instanceof X509Certificate cert) {
                        Key chave = cert.getPublicKey();
                        return () -> chave;
                    }
                }
            }
        }
        throw new KeySelectorException("certificado não encontrado no KeyInfo");
    }
}
