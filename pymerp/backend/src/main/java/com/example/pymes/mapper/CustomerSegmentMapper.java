package com.example.pymes.mapper;

import com.example.pymes.dto.CustomerSegmentRequest;
import com.example.pymes.dto.CustomerSegmentResponse;
import com.example.pymes.entity.CustomerSegment;
import org.springframework.stereotype.Component;

/**
 * Mapper for CustomerSegment entity and DTOs.
 */
@Component
public class CustomerSegmentMapper {

    /**
     * Convert entity to response DTO.
     */
    public CustomerSegmentResponse toResponse(CustomerSegment entity) {
        if (entity == null) {
            return null;
        }
        CustomerSegmentResponse response = new CustomerSegmentResponse();
        response.setId(entity.getId());
        response.setCode(entity.getCode());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        response.setColor(entity.getColor());
        response.setActive(entity.getActive());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    /**
     * Update entity from request DTO.
     */
    public void updateEntityFromRequest(CustomerSegment entity, CustomerSegmentRequest request) {
        if (request.getCode() != null) {
            entity.setCode(request.getCode().trim().toUpperCase());
        }
        if (request.getName() != null) {
            entity.setName(request.getName().trim());
        }
        entity.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        entity.setColor(request.getColor() != null ? request.getColor().trim() : null);
        if (request.getActive() != null) {
            entity.setActive(request.getActive());
        }
    }
}
