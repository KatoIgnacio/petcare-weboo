package cl.bello.petcare.dto;

public record UbicacionClinicaResponse(
		String nombre,
		String direccion,
		double latitud,
		double longitud,
		String urlMapa) {
}
