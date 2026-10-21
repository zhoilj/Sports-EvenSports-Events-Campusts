package com.sports.modules.schedule.service.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScheduleEntryServiceImplTest {

    @Test
    void buildsStableCheckinCodeFromScheduleEntryId() {
        assertEquals("CK00000001", ScheduleEntryServiceImpl.buildCheckinCode(1L));
        assertEquals("CK00001234", ScheduleEntryServiceImpl.buildCheckinCode(1234L));
    }

    @Test
    void rejectsInvalidEntryIdForCheckinCode() {
        assertThrows(IllegalArgumentException.class, () -> ScheduleEntryServiceImpl.buildCheckinCode(null));
        assertThrows(IllegalArgumentException.class, () -> ScheduleEntryServiceImpl.buildCheckinCode(0L));
    }
}
