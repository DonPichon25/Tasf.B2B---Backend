package com.grupo5e.morapack.api.mapper;

import com.grupo5e.morapack.api.dto.VueloDTO;
import com.grupo5e.morapack.core.model.Vuelo;
import org.springframework.stereotype.Component;

@Component
public class VueloMapper {
    
    /**
     * Convierte una entidad Vuelo a DTO
     * Evita lazy loading de relaciones
     */
    public VueloDTO toDTO(Vuelo vuelo) {
        if (vuelo == null) {
            return null;
        }
        
        return VueloDTO.builder()
                .id(vuelo.getId())
                .codigo(vuelo.getIdentificadorVuelo())
                .frecuenciaPorDia(vuelo.getFrecuenciaPorDia())
                .horaSalida(vuelo.getHoraSalida())
                .horaLlegada(vuelo.getHoraLlegada())
                // Solo IDs y códigos, no objetos completos
                .aeropuertoOrigenId(vuelo.getAeropuertoOrigen() != null ? 
                    vuelo.getAeropuertoOrigen().getId() : null)
                .aeropuertoOrigenCodigo(vuelo.getAeropuertoOrigen() != null ? 
                    vuelo.getAeropuertoOrigen().getCodigoIATA() : null)
                .aeropuertoDestinoId(vuelo.getAeropuertoDestino() != null ? 
                    vuelo.getAeropuertoDestino().getId() : null)
                .aeropuertoDestinoCodigo(vuelo.getAeropuertoDestino() != null ? 
                    vuelo.getAeropuertoDestino().getCodigoIATA() : null)
                .capacidadMaxima(vuelo.getCapacidadMaxima())
                .capacidadUsada(vuelo.getCapacidadUsada())
                .tiempoTransporte(vuelo.getTiempoTransporte())
                .costo(vuelo.getCosto())
                .estado(vuelo.getEstado())
                .latitudActual(vuelo.getLatitudActual())
                .longitudActual(vuelo.getLongitudActual())
                .build();
    }
    
    /**
     * Convierte un DTO a entidad Vuelo
     * NOTA: No establece relaciones (aeropuertos), debe hacerse por separado
     */
    public Vuelo toEntity(VueloDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Vuelo vuelo = new Vuelo();
        vuelo.setId(dto.getId());
        vuelo.setFrecuenciaPorDia(dto.getFrecuenciaPorDia());
        vuelo.setHoraSalida(dto.getHoraSalida());
        vuelo.setHoraLlegada(dto.getHoraLlegada());
        vuelo.setCapacidadMaxima(dto.getCapacidadMaxima());
        vuelo.setCapacidadUsada(dto.getCapacidadUsada());
        vuelo.setTiempoTransporte(dto.getTiempoTransporte());
        vuelo.setCosto(dto.getCosto());
        vuelo.setEstado(dto.getEstado());
        vuelo.setLatitudActual(dto.getLatitudActual());
        vuelo.setLongitudActual(dto.getLongitudActual());
        
        return vuelo;
    }
}

