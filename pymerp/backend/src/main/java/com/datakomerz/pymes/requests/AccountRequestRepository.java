package com.datakomerz.pymes.requests;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRequestRepository extends JpaRepository<AccountRequest, UUID> {
}
