package org.example.registrationservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.example.registrationservice.clients.EventClient;
import org.example.registrationservice.exceptions.EventNotFoundException;
import org.example.registrationservice.exceptions.EventServiceException;
import org.example.registrationservice.models.dto.responses.EventResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventGatewayService {
    private final EventClient eventClient;

    @CircuitBreaker(name = "eventService", fallbackMethod = "getEventFallback")
    public EventResponse getEventById(Long eventId) {
        EventResponse event = eventClient.getEventById(eventId);
        if (event == null || event.id() == null || event.ticketPrice()==null || event.availableTickets()==null) {
            throw new EventServiceException("Dữ liệu không hợp lệ!");
        }

        if ( event.ticketPrice() < 0 || event.availableTickets() < 0 ) {
            throw new EventServiceException("Giá vé hoặc số lượng vé không hợp lệ!");
        }

        return event;
    }

    private EventResponse getEventFallback(Long eventId, Throwable error) {
        if  (error instanceof FeignException.NotFound) {
            throw new EventNotFoundException(eventId);
        }

        if ( error instanceof EventNotFoundException eventNotFoundException){
            throw eventNotFoundException;
        }

        if ( error instanceof EventServiceException eventServiceException) {
            throw eventServiceException;
        }
        throw new EventServiceException("Event Service không kết nối đến event có id: " + eventId, error);

    }
}
