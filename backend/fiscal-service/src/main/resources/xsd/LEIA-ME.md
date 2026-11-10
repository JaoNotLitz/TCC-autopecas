# Schemas da NF-e

Schemas oficiais do layout 4.00 (pacote `PL_009_V4`, Portal da NF-e), copiados do pacote
redistribuído pelo projeto `nfephp-org/sped-nfe`. O `ValidadorXsd` compila `nfe_v4.00.xsd`, que inclui
`leiauteNFe_v4.00.xsd`, `tiposBasico_v4.00.xsd` e `xmldsig-core-schema_v1.01.xsd`.

O XML é montado com DOM (`NfeXmlBuilder`) e conferido contra estes schemas, em vez de usar classes
JAXB geradas: o resultado é o mesmo (XSD oficial como contrato) sem um passo de geração de código no build.
