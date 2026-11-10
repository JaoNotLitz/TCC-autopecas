package br.com.wcautopecas.fiscal;

import java.time.Duration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "wc.fiscal")
public class FiscalProperties {

    private String tabelaNotas = "notas_fiscais";
    private String tabelaItens = "notas_items";
    private String tabelaEventos = "fiscal_events";
    private String filaVendas = "sales-events-queue";
    private String filaJobs = "fiscal-jobs-queue";
    private String filaEventos = "fiscal-events-queue";
    private String salesUrl = "http://sales-service:8083";
    private String customerUrl = "http://customer-service:8081";
    private String productUrl = "http://product-service:8082";
    private Duration timeoutChamadas = Duration.ofSeconds(2);
    /** Série única da loja (NF-e, NFC-e e devolução). */
    private String serie = "1";
    private String ambiente = "homologacao";
    /** UF do emitente; define o CFOP da devolução (1202 dentro do estado, 2202 fora). */
    private String ufEmitente = "SP";
    private Duration prazoCancelamento = Duration.ofHours(24);

    // EPB-08: geração do XML
    private String filaSefaz = "sefaz-queue";
    private String bucketDocumentos = "wc-documents-bucket";
    private String topicoSistema = "system-notifications-topic";
    private String secretCertificado = "wc-sefaz-certificate";
    private Duration cacheCertificado = Duration.ofHours(1);
    /** CSC de homologação para o QR Code da NFC-e (id do token e código). */
    private String cscId = "000001";
    private String csc = "WCAUTOPECAS0000000000000000000000000000";
    private String urlQrCode = "https://www.homologacao.nfce.fazenda.sp.gov.br/NFCeConsultaPublica/Paginas/ConsultaQRCode.aspx";
    private String urlChave = "https://www.homologacao.nfce.fazenda.sp.gov.br/consulta";
    private Emitente emitente = new Emitente();
    // EPB-09: integração com a SEFAZ (simulador no perfil local)
    private String filaDocumentos = "document-events-queue";
    private String filaEstoque = "inventory-updates-queue";
    private Sefaz sefaz = new Sefaz();

    @Data
    public static class Sefaz {
        /** Base dos webservices; cada serviço fica em {base}/ws/{Servico}. Local: o sefaz-mock. */
        private String url = "http://sefaz-mock:8080";
        /** URL própria por serviço (opcional), ex.: NFeAutorizacao4 → https://... */
        private java.util.Map<String, String> urls = new java.util.HashMap<>();
        private Duration timeout = Duration.ofSeconds(60);
        /** Esperas entre as consultas do recibo (HU-B09.3). */
        private java.util.List<Duration> esperasConsulta = java.util.List.of(Duration.ofSeconds(5),
                Duration.ofSeconds(10), Duration.ofSeconds(20));
        /** Visibility da mensagem antes da 2ª e da 3ª tentativa depois de erro de comunicação (HU-B09.5). */
        private java.util.List<Duration> esperasRetentativa = java.util.List.of(Duration.ofMinutes(1),
                Duration.ofMinutes(5));
        /** Atraso da mensagem devolvida à fila quando o lote segue em processamento. */
        private Duration atrasoReenvio = Duration.ofSeconds(60);
        private int maxTentativas = 3;

        public String endpoint(String servico) {
            return urls.getOrDefault(servico, url.replaceAll("/+$", "") + "/ws/" + servico);
        }
    }

    /** Dados da loja (fictícios), usados no grupo {@code emit}. */
    @Data
    public static class Emitente {
        private String cnpj = "12345678000195";
        private String ie = "111222333444";
        private String razaoSocial = "WC AUTOPECAS LTDA";
        private String nomeFantasia = "WC Autopeças";
        private String logradouro = "Avenida Paulista";
        private String numero = "1000";
        private String bairro = "Bela Vista";
        private String municipio = "Sao Paulo";
        private String codigoMunicipio = "3550308";
        private String uf = "SP";
        private String cep = "01310100";
        private String fone = "1130000000";
        /** 3 = regime normal (o grupo ICMS00 da nota usa a alíquota do produto). */
        private String crt = "3";
    }
}
