package com.sports.modules.registration.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.registration.dto.RelayTeamCreateDTO;
import com.sports.modules.registration.entity.RelayTeam;
import com.sports.modules.registration.vo.RelayTeamMemberVO;
import com.sports.modules.registration.vo.RelayTeamVO;

import java.util.List;

public interface RelayTeamService extends IService<RelayTeam> {
    Long createTeam(RelayTeamCreateDTO dto);
    IPage<RelayTeamVO> pageTeams(Integer current, Integer size, Long eventId, Long sportItemId, Integer status, String college);
    List<RelayTeamVO> getMyTeams();
    RelayTeamVO getTeamDetail(Long id);
    void reviewTeam(Long id, Integer status, String remark);
    void cancelTeam(Long id);
    void replaceMembers(Long id, List<Long> memberUserIds);
    List<RelayTeamMemberVO> getEligibleMembers(String keyword);
}
