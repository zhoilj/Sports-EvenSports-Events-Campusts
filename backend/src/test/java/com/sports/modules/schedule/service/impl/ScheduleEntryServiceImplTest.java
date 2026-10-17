package com.sports.modules.schedule.service.impl;

import com.sports.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScheduleEntryServiceImplTest {

    @Test
    void namesTrackGroupsSequentially() {
        assertEquals("A组", ScheduleEntryServiceImpl.groupName(0));
        assertEquals("Z组", ScheduleEntryServiceImpl.groupName(25));
        assertEquals("第27组", ScheduleEntryServiceImpl.groupName(26));
    }

    @Test
    void acceptsOnlySupportedCheckinStates() {
        assertEquals("CHECKED_IN", ScheduleEntryServiceImpl.normalizeCheckinStatus("checked_in"));
        assertEquals("ABSENT", ScheduleEntryServiceImpl.normalizeCheckinStatus(" ABSENT "));
        assertThrows(BusinessException.class,
                () -> ScheduleEntryServiceImpl.normalizeCheckinStatus("APPROVED"));
    }
}
