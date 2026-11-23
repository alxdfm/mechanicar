# Mechanicar

App Android em Kotlin para controlar e visualizar os gastos de manutenção do carro (ex.: **Chevrolet Corsa Hatch Maxx 2012 · ISO0D85**).

## O que o app faz

- **Resumo**: total investido, gastos por categoria e por ano, faixa de KM
- **Meu carro**: editar marca, modelo, ano, placa, cor e observações do veículo
- **Manutenções**: cada manutenção no mecânico com data, KM, origem (OS/orçamento), oficina e observações
- **Serviços**: itens feitos no dia, com quantidade, valor unitário e classificação (categoria)
- **Importação**: carrega planilha `.xlsx` ou `.csv`, ou os dados iniciais embutidos do Corsa

## Planilha de importação

Arquivos gerados a partir da planilha original `Gastos_Corsa.xlsx`:

| Arquivo | Uso |
|---|---|
| `import/gastos_corsa_importacao.xlsx` | Planilha pronta para importar no app (Excel) |
| `import/gastos_corsa_importacao.csv` | Mesmo conteúdo em CSV UTF-8 |
| `app/src/main/assets/gastos_corsa_importacao.csv` | Cópia embutida no app (“Carregar dados iniciais”) |

### Colunas

```text
data, km, origem, oficina, categoria, item, quantidade, valor_unitario, observacao
```

- Data: `DD/MM/AAAA` ou `AAAA-MM-DD`
- O app agrupa linhas com a mesma **data + origem + oficina** em uma manutenção
- Categorias: Revisão/Óleo e filtros, Arrefecimento, Freios, Suspensão, Motor, Transmissão/Câmbio, Pneus/Alinhamento, Elétrica/Iluminação, Escapamento, Combustível, Mão de obra

Dados iniciais: **13 manutenções**, **85 serviços**, total **R$ 17.349,43**.

## Como abrir no Android Studio

1. Abra esta pasta no Android Studio (Giraffe+ / AGP 8.7)
2. Aguarde o Gradle sync
3. Rode no emulador ou aparelho (API 26+)

```bash
./gradlew :app:assembleDebug
```

## APK via GitHub Actions

A cada push (e em pull requests), o workflow **Build APK** gera o APK debug e publica como artifact.

1. Abra a aba **Actions** do repositório
2. Entre no run do commit desejado
3. Baixe o artifact `gastos-corsa-apk`

Também dá para disparar manualmente em **Actions → Build APK → Run workflow**.

## Stack

- Kotlin + Jetpack Compose + Material 3
- Room (persistência local)
- Navigation Compose
- Importação CSV e `.xlsx` (leitor OOXML próprio, sem Apache POI)
