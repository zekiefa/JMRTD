# Changelog

Todas as alterações notáveis deste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/).

## [0.5.2-maven] — Branch `maven-build-setup`

Migração do projeto para Maven moderno e Java 25.

### Adicionado

- **`pom.xml` agregador** na raiz (`jmrtd-parent`) — `mvn clean package` compila os 4 módulos de uma vez
- POMs individuais em todos os módulos ativos: `jmrtd`, `wsq_imageio`, `jj2000_imageio`, `passportapplet`
- Nova classe `org.jmrtd.RFC5114Groups` — vendoriza os grupos Diffie-Hellman RFC 5114 (removidos do BouncyCastle 1.70+, mas exigidos pelo PACE/ICAO)
- `.gitignore` na raiz ignorando `target/`; `.gitignore` no módulo `jmrtd`
- Compilação com `maven-compiler-plugin` 3.13.0, `release 25`

### Alterado

- **Java 8 → Java 25** em todos os módulos
- Layout de fontes migrado para o padrão Maven (`src/` → `src/main/java/`)
- `net.sf.scuba:scuba-smartcards` 0.0.6 → **0.0.20**
  - `APDUWrapper.unwrap(ResponseAPDU)` perdeu o parâmetro `len`
  - Nova assinatura obrigatória `APDUWrapper.getType()`
  - Novo método abstrato `CardService.isConnectionLost(Exception)`
- `org.bouncycastle:bcprov-jdk15on` 1.52 → **bcprov-jdk18on 1.80**
  - `ECPoint.getEncoded()` → `getEncoded(false)`
  - `getX()/getY()` → `getAffineXCoord()/getAffineYCoord()`
  - `ECCurve.createPoint(x, y, boolean)` → `createPoint(x, y)`
  - `X9ECParameters` agora recebe `X9ECPoint`
  - ASN.1: `getObject()` → `getBaseObject().toASN1Primitive()`, `toASN1Object()` → `toASN1Primitive()`
  - `AlgorithmIdentifier(String)` → `AlgorithmIdentifier(ASN1ObjectIdentifier)`
- `com.klinec:jcardsim` 3.0.5.11 → **3.0.6.0** (escopo `provided` no passportapplet)
- Adicionada dependência faltante `org.ejbca.cvc:cert-cvc:1.4.13` (jmrtd)
- Adicionada dependência `edu.ucar:jj2000:5.2` (jj2000_imageio — implementação JJ2000 completa com pacotes `icc`/`colorspace`)

### Removido

- Arquivos de metadados do Eclipse (`.project`, `.classpath`, `.settings`, `.fbprefs`, `.externalToolBuilders`) dos módulos ativos
- Scripts de build ANT (`build.xml`, `build_android.xml`) dos módulos ativos
- Artefatos de build acidentalmente versionados em `wsq_imageio/target/`

### Observações

- Os projetos em `abandoned/` **não foram migrados** (legados Eclipse/Ant, fora do build agregador)
- Build validado: `BUILD SUCCESS` nos 4 módulos (~10s)
