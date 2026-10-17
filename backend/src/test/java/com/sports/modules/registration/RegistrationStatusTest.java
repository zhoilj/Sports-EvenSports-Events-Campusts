package com.sports.modules.registration;

import com.sports.modules.registration.entity.RegistrationStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RegistrationStatusTest {

    @Test
    void mapsApiStatusToDatabaseCode() {
        assertEquals(1, RegistrationStatus.fromApi("PENDING").getCode());
        assertEquals(2, RegistrationStatus.fromApi("approved").getCode());
        assertEquals(3, RegistrationStatus.fromApi("REJECTED").getCode());
        assertEquals(4, RegistrationStatus.fromApi("CANCELLED").getCode());
    }

    @Test
    void mapsDatabaseCodeToApiStatus() {
        assertEquals("PENDING", RegistrationStatus.fromCode(1).name());
        assertEquals("APPROVED", RegistrationStatus.fromCode(2).name());
        assertEquals("REJECTED", RegistrationStatus.fromCode(3).name());
        assertEquals("CANCELLED", RegistrationStatus.fromCode(4).name());
        assertNull(RegistrationStatus.fromCode(99));
    }

    @Test
    void rejectsUnknownApiStatus() {
        assertThrows(IllegalArgumentException.class, () -> RegistrationStatus.fromApi("UNKNOWN"));
    }
}
