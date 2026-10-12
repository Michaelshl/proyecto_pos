package com.pos.cliente;

import com.pos.cliente.dto.ClienteRequest;
import com.pos.cliente.dto.ClienteResponse;
import com.pos.cliente.dto.ClienteSugerencia;
import com.pos.cliente.dto.ClienteUpdateRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ClienteService {

    private static final int MAX_SUGERENCIAS = 8;

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    // RF-020
    public ClienteResponse crear(ClienteRequest request) {
        String numero = request.getNumeroDocumento().trim();
        if (clienteRepository.existsById(numero)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ya existe un cliente con ese número de documento");
        }

        Cliente cliente = new Cliente();
        cliente.setNumeroDocumento(numero);
        cliente.setTipoDocumento(request.getTipoDocumento());
        cliente.setNombre(request.getNombre().trim());
        cliente.setApellido(apellidoSegunTipo(request.getTipoDocumento(), request.getApellido()));
        cliente.setEmail(vacioANulo(request.getEmail()));
        cliente.setTelefono(vacioANulo(request.getTelefono()));
        cliente.setDireccion(vacioANulo(request.getDireccion()));
        clienteRepository.save(cliente);
        return toResponse(cliente);
    }

    // RF-021
    public ClienteResponse actualizar(String numeroDocumento, ClienteUpdateRequest request) {
        Cliente cliente = obtener(numeroDocumento);
        cliente.setTipoDocumento(request.getTipoDocumento());
        cliente.setNombre(request.getNombre().trim());
        cliente.setApellido(apellidoSegunTipo(request.getTipoDocumento(), request.getApellido()));
        cliente.setEmail(vacioANulo(request.getEmail()));
        cliente.setTelefono(vacioANulo(request.getTelefono()));
        cliente.setDireccion(vacioANulo(request.getDireccion()));
        clienteRepository.save(cliente);
        return toResponse(cliente);
    }

    public ClienteResponse consultar(String numeroDocumento) {
        return toResponse(obtener(numeroDocumento));
    }

    // Lista corta para el autocompletado. Con menos de 2 caracteres no consulta.
    // Se quitan % y _ para que el texto escrito no funcione como comodín del LIKE.
    public List<ClienteSugerencia> sugerir(String texto) {
        String limpio = texto == null ? "" : texto.replaceAll("[%_\\\\]", "").trim();
        if (limpio.length() < 2) {
            return List.of();
        }
        return clienteRepository.sugerir(limpio, PageRequest.of(0, MAX_SUGERENCIAS)).stream()
                .map(c -> new ClienteSugerencia(c.getNumeroDocumento(), c.getNombre(), c.getApellido()))
                .toList();
    }

    private Cliente obtener(String numeroDocumento) {
        return clienteRepository.findById(numeroDocumento.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    // Una empresa (NIT) tiene razón social y no apellido: se guarda siempre como NULL, aunque llegue algo.
    // Una persona (CC, CE, TI, PP) debe tener apellido.
    private static String apellidoSegunTipo(String tipoDocumento, String apellido) {
        if ("NIT".equals(tipoDocumento)) {
            return null;
        }
        String limpio = vacioANulo(apellido);
        if (limpio == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El apellido es obligatorio");
        }
        return limpio;
    }

    // Un campo opcional vacío se guarda como NULL, no como texto en blanco.
    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private ClienteResponse toResponse(Cliente c) {
        return ClienteResponse.builder()
                .tipoDocumento(c.getTipoDocumento())
                .numeroDocumento(c.getNumeroDocumento())
                .nombre(c.getNombre())
                .apellido(c.getApellido())
                .email(c.getEmail())
                .telefono(c.getTelefono())
                .direccion(c.getDireccion())
                .build();
    }
}
