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
