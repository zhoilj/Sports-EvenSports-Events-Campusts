package com.sports.modules.registration.service.impl;

import com.sports.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrationServiceImplTest {

    private final RegistrationServiceImpl service = new RegistrationServiceImpl(null, null, null);

    @Test
    void parsesFrontEndAndDatabaseStatusValues() {
        assertEquals(1, ReflectionTestUtils.invokeMethod(service, "parseStatus", "PENDING"));
        assertEquals(2, ReflectionTestUtils.invokeMethod(service, "parseStatus", "APPROVED"));
        assertEquals(3, ReflectionTestUtils.invokeMethod(service, "parseStatus", "REJECTED"));
        assertEquals(4, ReflectionTestUtils.invokeMethod(service, "parseStatus", "CANCELLED"));
        assertEquals(2, ReflectionTestUtils.invokeMethod(service, "parseStatus", "2"));
    }

    @Test
    void mapsDatabaseStatusBackToFrontEndCodes() {
        assertEquals("PENDING", ReflectionTestUtils.invokeMethod(service, "getStatusCode", 1));
        assertEquals("APPROVED", ReflectionTestUtils.invokeMethod(service, "getStatusCode", 2));
        assertEquals("REJECTED", ReflectionTestUtils.invokeMethod(service, "getStatusCode", 3));
        assertEquals("CANCELLED", ReflectionTestUtils.invokeMethod(service, "getStatusCode", 4));
    }

    @Test
    void rejectsUnknownRegistrationStatus() {
        assertThrows(BusinessException.class,
                () -> ReflectionTestUtils.invokeMethod(service, "parseStatus", "NOT_A_STATUS"));
    }
}
