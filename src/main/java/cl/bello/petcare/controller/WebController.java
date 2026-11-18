package cl.bello.petcare.controller;

import cl.bello.petcare.dto.AtencionRequest;
import cl.bello.petcare.dto.ClienteRequest;
import cl.bello.petcare.dto.EstadoReservaRequest;
import cl.bello.petcare.dto.MascotaRequest;
import cl.bello.petcare.dto.ReservaRequest;
import cl.bello.petcare.dto.ReservaResponse;
import cl.bello.petcare.dto.ReservaUpdateRequest;
import cl.bello.petcare.dto.VeterinarioRequest;
import cl.bello.petcare.entity.EstadoReserva;
import cl.bello.petcare.service.ClienteService;
import cl.bello.petcare.service.InformacionExternaClient;
import cl.bello.petcare.service.MascotaService;
import cl.bello.petcare.service.ReservaService;
import cl.bello.petcare.service.VeterinarioService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebController {

	private final ClienteService clienteService;
	private final MascotaService mascotaService;
	private final VeterinarioService veterinarioService;
	private final ReservaService reservaService;
	private final InformacionExternaClient informacionExternaClient;

	public WebController(
			ClienteService clienteService,
			MascotaService mascotaService,
			VeterinarioService veterinarioService,
			ReservaService reservaService,
			InformacionExternaClient informacionExternaClient) {
		this.clienteService = clienteService;
		this.mascotaService = mascotaService;
		this.veterinarioService = veterinarioService;
		this.reservaService = reservaService;
		this.informacionExternaClient = informacionExternaClient;
	}

	@GetMapping("/")
	public String dashboard(Model model) {
		List<ReservaResponse> reservas = reservaService.listar();
		model.addAttribute("seccion", "inicio");
		model.addAttribute("totalClientes", clienteService.listar().size());
		model.addAttribute("totalMascotas", mascotaService.listar().size());
		model.addAttribute("totalReservas", reservas.size());
		model.addAttribute("reservasPendientes", reservas.stream()
				.filter(reserva -> reserva.estado() == EstadoReserva.PENDIENTE)
				.count());
		model.addAttribute("reservasRecientes", reservas.stream().limit(5).toList());
		return "dashboard";
	}

	@GetMapping("/clientes")
	public String clientes(Model model) {
		model.addAttribute("seccion", "clientes");
		model.addAttribute("clientes", clienteService.listar());
		return "clientes/lista";
	}

	@GetMapping("/clientes/nuevo")
	public String nuevoCliente(Model model) {
		model.addAttribute("seccion", "clientes");
		model.addAttribute("modoEdicion", false);
		return "clientes/formulario";
	}

	@PostMapping("/clientes")
	public String crearCliente(
			@RequestParam String nombre,
			@RequestParam String email,
			@RequestParam String telefono,
			RedirectAttributes redirectAttributes) {
		try {
			var cliente = clienteService.crear(new ClienteRequest(nombre, email, telefono));
			redirectAttributes.addFlashAttribute("mensaje", "Cliente registrado correctamente");
			return "redirect:/clientes/" + cliente.id();
		} catch (RuntimeException error) {
			return redirigirConError("/clientes/nuevo", error, redirectAttributes);
		}
	}

	@GetMapping("/clientes/{id}")
	public String detalleCliente(@PathVariable Long id, Model model) {
		model.addAttribute("seccion", "clientes");
		model.addAttribute("cliente", clienteService.obtener(id));
		model.addAttribute("mascotas", mascotaService.listarPorCliente(id));
		return "clientes/detalle";
	}

	@GetMapping("/clientes/{id}/editar")
	public String editarCliente(@PathVariable Long id, Model model) {
		model.addAttribute("seccion", "clientes");
		model.addAttribute("modoEdicion", true);
		model.addAttribute("cliente", clienteService.obtener(id));
		return "clientes/formulario";
	}

	@PostMapping("/clientes/{id}")
	public String actualizarCliente(
			@PathVariable Long id,
			@RequestParam String nombre,
			@RequestParam String email,
			@RequestParam String telefono,
			RedirectAttributes redirectAttributes) {
		try {
			clienteService.actualizar(id, new ClienteRequest(nombre, email, telefono));
			redirectAttributes.addFlashAttribute("mensaje", "Datos del cliente actualizados");
			return "redirect:/clientes/" + id;
		} catch (RuntimeException error) {
			return redirigirConError("/clientes/" + id + "/editar", error, redirectAttributes);
		}
	}

	@PostMapping("/clientes/{id}/eliminar")
	public String eliminarCliente(
			@PathVariable Long id,
			RedirectAttributes redirectAttributes) {
		try {
			clienteService.eliminar(id);
			redirectAttributes.addFlashAttribute("mensaje", "Cliente y sus mascotas fueron desactivados");
			return "redirect:/clientes";
		} catch (RuntimeException error) {
			return redirigirConError("/clientes/" + id, error, redirectAttributes);
		}
	}

	@GetMapping("/clientes/{clienteId}/mascotas/nueva")
	public String nuevaMascota(@PathVariable Long clienteId, Model model) {
		model.addAttribute("seccion", "clientes");
		model.addAttribute("modoEdicion", false);
		model.addAttribute("cliente", clienteService.obtener(clienteId));
		return "mascotas/formulario";
	}

	@PostMapping("/clientes/{clienteId}/mascotas")
	public String crearMascota(
			@PathVariable Long clienteId,
			@RequestParam String nombre,
			@RequestParam String especie,
			@RequestParam String raza,
			@RequestParam LocalDate fechaNacimiento,
			@RequestParam BigDecimal peso,
			RedirectAttributes redirectAttributes) {
		try {
			var mascota = mascotaService.crear(clienteId,
					new MascotaRequest(nombre, especie, raza, fechaNacimiento, peso));
			redirectAttributes.addFlashAttribute("mensaje", "Mascota registrada correctamente");
			return "redirect:/mascotas/" + mascota.id();
		} catch (RuntimeException error) {
			return redirigirConError(
					"/clientes/" + clienteId + "/mascotas/nueva", error, redirectAttributes);
		}
	}

	@GetMapping("/mascotas/{id}")
	public String detalleMascota(@PathVariable Long id, Model model) {
		var mascota = mascotaService.obtener(id);
		model.addAttribute("seccion", "clientes");
		model.addAttribute("mascota", mascota);
		model.addAttribute("cliente", clienteService.obtener(mascota.clienteId()));
		model.addAttribute("historial", mascotaService.historial(id));
		return "mascotas/detalle";
	}

	@GetMapping("/mascotas/{id}/editar")
	public String editarMascota(@PathVariable Long id, Model model) {
		var mascota = mascotaService.obtener(id);
		model.addAttribute("seccion", "clientes");
		model.addAttribute("modoEdicion", true);
		model.addAttribute("mascota", mascota);
		model.addAttribute("cliente", clienteService.obtener(mascota.clienteId()));
		return "mascotas/formulario";
	}

	@PostMapping("/mascotas/{id}")
	public String actualizarMascota(
			@PathVariable Long id,
			@RequestParam String nombre,
			@RequestParam String especie,
			@RequestParam String raza,
			@RequestParam LocalDate fechaNacimiento,
			@RequestParam BigDecimal peso,
			RedirectAttributes redirectAttributes) {
		try {
			mascotaService.actualizar(id,
					new MascotaRequest(nombre, especie, raza, fechaNacimiento, peso));
			redirectAttributes.addFlashAttribute("mensaje", "Datos de la mascota actualizados");
			return "redirect:/mascotas/" + id;
		} catch (RuntimeException error) {
			return redirigirConError("/mascotas/" + id + "/editar", error, redirectAttributes);
		}
	}

	@PostMapping("/mascotas/{id}/eliminar")
	public String eliminarMascota(
			@PathVariable Long id,
			@RequestParam Long clienteId,
			RedirectAttributes redirectAttributes) {
		try {
			mascotaService.eliminar(id);
			redirectAttributes.addFlashAttribute("mensaje", "Mascota desactivada correctamente");
			return "redirect:/clientes/" + clienteId;
		} catch (RuntimeException error) {
			return redirigirConError("/mascotas/" + id, error, redirectAttributes);
		}
	}

	@GetMapping("/veterinarios")
	public String veterinarios(Model model) {
		model.addAttribute("seccion", "veterinarios");
		model.addAttribute("veterinarios", veterinarioService.listar());
		return "veterinarios/lista";
	}

	@PostMapping("/veterinarios")
	public String crearVeterinario(
			@RequestParam String nombre,
			@RequestParam String especialidad,
			@RequestParam String email,
			RedirectAttributes redirectAttributes) {
		try {
			veterinarioService.crear(new VeterinarioRequest(nombre, especialidad, email));
			redirectAttributes.addFlashAttribute("mensaje", "Veterinario registrado correctamente");
			return "redirect:/veterinarios";
		} catch (RuntimeException error) {
			return redirigirConError("/veterinarios", error, redirectAttributes);
		}
	}

	@GetMapping("/reservas")
	public String reservas(Model model) {
		model.addAttribute("seccion", "reservas");
		model.addAttribute("reservas", reservaService.listar());
		return "reservas/lista";
	}

	@GetMapping("/reservas/nueva")
	public String nuevaReserva(
			@RequestParam(required = false) Long mascotaId,
			Model model) {
		prepararFormularioReserva(model);
		model.addAttribute("modoEdicion", false);
		model.addAttribute("selectedMascotaId", mascotaId);
		return "reservas/formulario";
	}

	@PostMapping("/reservas")
	public String crearReserva(
			@RequestParam Long mascotaId,
			@RequestParam Long veterinarioId,
			@RequestParam LocalDateTime fechaHora,
			RedirectAttributes redirectAttributes) {
		try {
			var reserva = reservaService.crear(
					new ReservaRequest(mascotaId, veterinarioId, fechaHora));
			redirectAttributes.addFlashAttribute("mensaje", "Reserva registrada correctamente");
			return "redirect:/reservas/" + reserva.id();
		} catch (RuntimeException error) {
			return redirigirConError("/reservas/nueva", error, redirectAttributes);
		}
	}

	@GetMapping("/reservas/{id}")
	public String detalleReserva(@PathVariable Long id, Model model) {
		model.addAttribute("seccion", "reservas");
		model.addAttribute("reserva", reservaService.obtener(id));
		return "reservas/detalle";
	}

	@GetMapping("/reservas/{id}/editar")
	public String editarReserva(@PathVariable Long id, Model model) {
		prepararFormularioReserva(model);
		model.addAttribute("modoEdicion", true);
		model.addAttribute("reserva", reservaService.obtener(id));
		return "reservas/formulario";
	}

	@PostMapping("/reservas/{id}")
	public String actualizarReserva(
			@PathVariable Long id,
			@RequestParam Long veterinarioId,
			@RequestParam LocalDateTime fechaHora,
			RedirectAttributes redirectAttributes) {
		try {
			reservaService.actualizar(id, new ReservaUpdateRequest(veterinarioId, fechaHora));
			redirectAttributes.addFlashAttribute("mensaje", "Reserva actualizada correctamente");
			return "redirect:/reservas/" + id;
		} catch (RuntimeException error) {
			return redirigirConError("/reservas/" + id + "/editar", error, redirectAttributes);
		}
	}

	@PostMapping("/reservas/{id}/estado")
	public String cambiarEstado(
			@PathVariable Long id,
			@RequestParam EstadoReserva estado,
			RedirectAttributes redirectAttributes) {
		try {
			ReservaResponse reserva = reservaService.cambiarEstado(id, new EstadoReservaRequest(estado));
			redirectAttributes.addFlashAttribute("mensaje", "Reserva actualizada a " + estado.name());
			if (!"NO_APLICA".equals(reserva.resultadoNotificacion())) {
				redirectAttributes.addFlashAttribute(
						"notificacion", "Notificación: " + reserva.resultadoNotificacion());
			}
			return "redirect:/reservas/" + id;
		} catch (RuntimeException error) {
			return redirigirConError("/reservas/" + id, error, redirectAttributes);
		}
	}

	@GetMapping("/reservas/{id}/atencion/nueva")
	public String nuevaAtencion(@PathVariable Long id, Model model) {
		model.addAttribute("seccion", "reservas");
		model.addAttribute("reserva", reservaService.obtener(id));
		return "reservas/atencion";
	}

	@PostMapping("/reservas/{id}/atencion")
	public String registrarAtencion(
			@PathVariable Long id,
			@RequestParam String diagnostico,
			@RequestParam String tratamiento,
			@RequestParam String medicamentos,
			@RequestParam(required = false) String observaciones,
			RedirectAttributes redirectAttributes) {
		try {
			List<String> listaMedicamentos = Arrays.stream(medicamentos.split(","))
					.map(String::trim)
					.filter(valor -> !valor.isBlank())
					.toList();
			if (listaMedicamentos.isEmpty()) {
				throw new IllegalArgumentException("Debe indicar al menos un medicamento");
			}
			reservaService.registrarAtencion(id,
					new AtencionRequest(
							diagnostico,
							tratamiento,
							listaMedicamentos,
							observaciones));
			redirectAttributes.addFlashAttribute("mensaje", "Atención médica registrada");
			return "redirect:/reservas/" + id;
		} catch (RuntimeException error) {
			return redirigirConError(
					"/reservas/" + id + "/atencion/nueva", error, redirectAttributes);
		}
	}

	@GetMapping("/integraciones")
	public String integraciones(
			@RequestParam(required = false) String nombre,
			Model model) {
		model.addAttribute("seccion", "integraciones");
		model.addAttribute("nombreBuscado", nombre == null ? "" : nombre);
		model.addAttribute("busquedaRealizada", nombre != null && !nombre.isBlank());
		model.addAttribute("medicamentos", informacionExternaClient.buscarMedicamentos(nombre));
		model.addAttribute("ubicacion", informacionExternaClient.obtenerUbicacion().orElse(null));
		return "integraciones";
	}

	private void prepararFormularioReserva(Model model) {
		model.addAttribute("seccion", "reservas");
		model.addAttribute("mascotas", mascotaService.listar());
		model.addAttribute("veterinarios", veterinarioService.listar());
		model.addAttribute("fechaMinima", LocalDateTime.now()
				.plusHours(1)
				.truncatedTo(ChronoUnit.MINUTES));
	}

	private String redirigirConError(
			String ruta,
			RuntimeException error,
			RedirectAttributes redirectAttributes) {
		String mensaje = error.getMessage() == null
				? "No fue posible completar la operación"
				: error.getMessage();
		redirectAttributes.addFlashAttribute("error", mensaje);
		return "redirect:" + ruta;
	}
}
