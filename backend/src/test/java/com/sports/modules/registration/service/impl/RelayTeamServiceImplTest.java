package com.sports.modules.registration.service.impl;

import com.sports.common.exception.BusinessException;
import com.sports.modules.sportitem.entity.SportItem;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RelayTeamServiceImplTest {

    private final RelayTeamServiceImpl service = new RelayTeamServiceImpl(null, null, null, null);

    @Test
    void automaticallyAddsCaptainAndPreservesRelayOrder() {
        SportItem item = new SportItem();
        item.setIsRelay(1);
        item.setIsTeamEvent(1);
        item.setRelayMemberCount(4);
        item.setRelayReserveCount(1);

        List<Long> roster = ReflectionTestUtils.invokeMethod(
                service, "normalizeRoster", List.of(20L, 30L, 40L, 50L), 10L, item);

        assertEquals(List.of(10L, 20L, 30L, 40L, 50L), roster);
    }

    @Test
    void rejectsDuplicateMembersAndRosterOverCapacity() {
        SportItem item = new SportItem();
        item.setIsRelay(1);
        item.setIsTeamEvent(1);
        item.setRelayMemberCount(4);
        item.setRelayReserveCount(0);

        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(
                service, "normalizeRoster", List.of(10L, 20L, 20L), 10L, item));
        assertThrows(BusinessException.class, () -> ReflectionTestUtils.invokeMethod(
                service, "normalizeRoster", List.of(20L, 30L, 40L, 50L, 60L), 10L, item));
    }
}
