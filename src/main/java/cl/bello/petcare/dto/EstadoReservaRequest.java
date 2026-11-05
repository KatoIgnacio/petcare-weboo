package cl.bello.petcare.dto;

import cl.bello.petcare.entity.EstadoReserva;
import jakarta.validation.constraints.NotNull;

public record EstadoReservaRequest(@NotNull EstadoReserva estado) {
}

