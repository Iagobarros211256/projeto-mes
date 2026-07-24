package com.biscoitos.manutencao.paradas.web.dto;

import java.time.Instant;

public record EncerrarParadaRequest(Instant dataHoraFim) {
}
