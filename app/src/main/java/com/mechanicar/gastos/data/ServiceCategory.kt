package com.mechanicar.gastos.data

enum class ServiceCategory(val label: String) {
    REVISAO_OLEO("Revisão/Óleo e filtros"),
    ARREFECIMENTO("Arrefecimento"),
    FREIOS("Freios"),
    SUSPENSAO("Suspensão"),
    MOTOR("Motor"),
    TRANSMISSAO("Transmissão/Câmbio"),
    PNEUS("Pneus/Alinhamento"),
    ELETRICA("Elétrica/Iluminação"),
    ESCAPAMENTO("Escapamento"),
    COMBUSTIVEL("Combustível"),
    MAO_DE_OBRA("Mão de obra"),
    OUTROS("Outros");

    companion object {
        fun fromLabel(raw: String?): ServiceCategory {
            val value = raw?.trim().orEmpty()
            if (value.isEmpty()) return OUTROS
            return entries.firstOrNull { it.label.equals(value, ignoreCase = true) }
                ?: entries.firstOrNull { value.contains(it.label, ignoreCase = true) }
                ?: OUTROS
        }
    }
}
