package br.com.wcautopecas.document;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wc.documentos")
public class DocumentosProperties {

    private String tabelaNotas = "notas_fiscais";
    private String filaDocumentos = "document-events-queue";
    private String filaEmail = "email-queue";
    private String bucketDocumentos = "wc-documents-bucket";
    private String bucketRelatorios = "wc-reports-bucket";
    /** E-mail do contador para o lote mensal (HU-B13.4); vazio = não configurado. */
    private String contadorEmail = "";
    private Duration validadeLink = Duration.ofDays(7);
    /** Máximo de notas por ZIP; acima disso o lote é dividido em partes. */
    private int maximoPorLote = 2000;
    private String ambiente = "homologacao";
    /** Endpoint do S3 visto pelo navegador do contador (local: http://localhost:4566); vazio = o mesmo do serviço. */
    private String s3EnderecoPublico = "";
}
