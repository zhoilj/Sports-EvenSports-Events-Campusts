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
        assertEquals(Integer.valueOf(1), (Integer) ReflectionTestUtils.invokeMethod(service, "parseStatus", "PENDING"));
        assertEquals(Integer.valueOf(2), (Integer) ReflectionTestUtils.invokeMethod(service, "parseStatus", "APPROVED"));
        assertEquals(Integer.valueOf(3), (Integer) ReflectionTestUtils.invokeMethod(service, "parseStatus", "REJECTED"));
        assertEquals(Integer.valueOf(4), (Integer) ReflectionTestUtils.invokeMethod(service, "parseStatus", "CANCELLED"));
        assertEquals(Integer.valueOf(2), (Integer) ReflectionTestUtils.invokeMethod(service, "parseStatus", "2"));
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
