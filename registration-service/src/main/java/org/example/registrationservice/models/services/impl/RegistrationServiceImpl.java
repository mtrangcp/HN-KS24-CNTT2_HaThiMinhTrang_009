package org.example.registrationservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.registrationservice.models.constants.RegistrationStatus;
import org.example.registrationservice.models.dto.requests.CreateRegistrationDetailRequest;
import org.example.registrationservice.models.dto.requests.CreateRegistrationRequest;
import org.example.registrationservice.models.dto.responses.EventResponse;
import org.example.registrationservice.models.dto.responses.RegistrationDetailResponse;
import org.example.registrationservice.models.dto.responses.RegistrationResponse;
import org.example.registrationservice.models.entities.Registration;
import org.example.registrationservice.models.entities.RegistrationDetail;
import org.example.registrationservice.models.repositories.RegistrationDetailRepository;
import org.example.registrationservice.models.repositories.RegistrationRepository;
import org.example.registrationservice.models.services.RegistrationService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final RegistrationRepository registrationRepository;
    private final RegistrationDetailRepository registrationDetailRepository;
    private final EventGatewayService eventGatewayService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional
    public RegistrationResponse createRegistration(CreateRegistrationRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException(
                    "Danh sách sự kiện không được rỗng!"
            );
        }

        List<RegistrationDetail> details = new ArrayList<>();
        double totalAmount = 0;

        for (CreateRegistrationDetailRequest item : request.items()) {
            if (item == null
                    || item.eventId() == null
                    || item.quantity() == null
                    || item.quantity() <= 0) {
                throw new IllegalArgumentException(
                        "Sự kiện hoặc số lượng không hợp lệ!"
                );
            }

            EventResponse event =
                    eventGatewayService.getEventById(item.eventId());

            if (item.quantity() > event.availableTickets()) {
                throw new IllegalArgumentException(
                        "Không đủ vé cho sự kiện: " + item.eventId()
                );
            }

            double lineTotal = event.ticketPrice() * item.quantity();
            totalAmount += lineTotal;

            RegistrationDetail detail = RegistrationDetail.builder()
                    .eventId(event.id())
                    .quantity(item.quantity())
                    .ticketPrice(event.ticketPrice())
                    .lineTotal(lineTotal)
                    .build();

            details.add(detail);
        }

        Registration registration = Registration.builder()
                .participantName(request.participantName().trim())
                .participantEmail(request.participantEmail().trim())
                .totalAmount(totalAmount)
                .status(RegistrationStatus.PENDING)
                .build();

        registration = registrationRepository.save(registration);


        for (RegistrationDetail detail : details) {
            detail.setRegistration(registration);
        }

        List<RegistrationDetail> savedDetails =
                registrationDetailRepository.saveAll(details);


        List<RegistrationDetailResponse> detailResponses =
                new ArrayList<>();

        for (RegistrationDetail detail : savedDetails) {
            detailResponses.add(new RegistrationDetailResponse(
                    detail.getId(),
                    detail.getEventId(),
                    detail.getQuantity(),
                    detail.getTicketPrice(),
                    detail.getLineTotal()
            ));
        }

        String email = registration.getParticipantEmail();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        kafkaTemplate.send("registration-created", email);
                    }
                }
        );

        return new RegistrationResponse(
                registration.getId(),
                registration.getParticipantName(),
                registration.getParticipantEmail(),
                registration.getTotalAmount(),
                registration.getStatus(),
                detailResponses
        );
    }
}
