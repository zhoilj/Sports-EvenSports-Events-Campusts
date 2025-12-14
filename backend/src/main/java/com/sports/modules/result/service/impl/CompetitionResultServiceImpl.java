package com.sports.modules.result.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.sports.modules.event.entity.Event;
import com.sports.modules.event.service.EventService;
import com.sports.modules.result.entity.CompetitionResult;
import com.sports.modules.result.mapper.CompetitionResultMapper;
import com.sports.modules.result.service.CompetitionResultService;
import com.sports.modules.result.vo.CompetitionResultVO;
import com.sports.modules.sportitem.entity.SportItem;
import com.sports.modules.sportitem.service.SportItemService;
import com.sports.modules.system.entity.SysUser;
import com.sports.modules.system.service.SysUserService;
import com.sports.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 比赛成绩 Service 实现
 *
 * @author sports
 */
@Service
@RequiredArgsConstructor
public class CompetitionResultServiceImpl extends ServiceImpl<CompetitionResultMapper, CompetitionResult> implements CompetitionResultService {

    private final EventService eventService;
    private final SportItemService sportItemService;
    private final SysUserService sysUserService;

    @Override
    public IPage<CompetitionResult> pageResults(Integer current, Integer size, Long eventId, Long sportItemId, Long userId) {
        Page<CompetitionResult> page = new Page<>(current, size);

        LambdaQueryWrapper<CompetitionResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(eventId != null, CompetitionResult::getEventId, eventId)
                .eq(sportItemId != null, CompetitionResult::getSportItemId, sportItemId)
                .eq(userId != null, CompetitionResult::getUserId, userId)
                .orderByAsc(CompetitionResult::getRanking);

        return this.page(page, wrapper);
    }

    @Override
    public IPage<CompetitionResultVO> pageResultsVO(Integer current, Integer size, Long eventId, Long sportItemId, Long userId) {
        IPage<CompetitionResult> resultPage = pageResults(current, size, eventId, sportItemId, userId);
        return convertToVOPage(resultPage);
    }

    @Override
    public List<CompetitionResult> getRankingByEventAndItem(Long eventId, Long sportItemId) {
        LambdaQueryWrapper<CompetitionResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CompetitionResult::getEventId, eventId)
                .eq(CompetitionResult::getSportItemId, sportItemId)
                .orderByAsc(CompetitionResult::getRanking);

        return this.list(wrapper);
    }

    @Override
    public List<CompetitionResult> getMyResults(Long userId) {
        LambdaQueryWrapper<CompetitionResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CompetitionResult::getUserId, userId)
                .orderByDesc(CompetitionResult::getCreateTime);

        return this.list(wrapper);
    }

    @Override
    public List<CompetitionResultVO> getMyResultsVO(Long userId) {
        List<CompetitionResult> results = getMyResults(userId);
        return convertToVOList(results);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSaveResults(List<CompetitionResult> results) {
        Long recorderId = SecurityUtils.getUserId();
        for (CompetitionResult result : results) {
            result.setRecorderId(recorderId);
        }
        this.saveBatch(results);
    }

    /**
     * 批量转换为VO（优化N+1查询）
     */
    private IPage<CompetitionResultVO> convertToVOPage(IPage<CompetitionResult> page) {
        List<CompetitionResult> records = page.getRecords();
        List<CompetitionResultVO> voList = convertToVOList(records);
        
        Page<CompetitionResultVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 批量转换为VO列表（优化N+1查询）
     */
    private List<CompetitionResultVO> convertToVOList(List<CompetitionResult> results) {
        if (results.isEmpty()) {
            return List.of();
        }

        // 收集所有需要查询的ID
        Set<Long> eventIds = results.stream().map(CompetitionResult::getEventId).collect(Collectors.toSet());
        Set<Long> sportItemIds = results.stream().map(CompetitionResult::getSportItemId).collect(Collectors.toSet());
        Set<Long> userIds = results.stream().map(CompetitionResult::getUserId).collect(Collectors.toSet());
        Set<Long> recorderIds = results.stream()
                .map(CompetitionResult::getRecorderId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        userIds.addAll(recorderIds);

        // 批量查询关联数据
        Map<Long, Event> eventMap = eventService.listByIds(eventIds).stream()
                .collect(Collectors.toMap(Event::getId, e -> e));
        Map<Long, SportItem> sportItemMap = sportItemService.listByIds(sportItemIds).stream()
                .collect(Collectors.toMap(SportItem::getId, s -> s));
        Map<Long, SysUser> userMap = sysUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u));

        // 转换
        return results.stream().map(result -> {
            CompetitionResultVO vo = new CompetitionResultVO();
            BeanUtils.copyProperties(result, vo);

            Event event = eventMap.get(result.getEventId());
            if (event != null) {
                vo.setEventName(event.getName());
            }

            SportItem sportItem = sportItemMap.get(result.getSportItemId());
            if (sportItem != null) {
                vo.setSportItemName(sportItem.getName());
            }

            SysUser user = userMap.get(result.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setRealName(user.getRealName());
                vo.setStudentNo(user.getStudentNo());
                vo.setCollege(user.getCollege());
            }

            if (result.getRecorderId() != null) {
                SysUser recorder = userMap.get(result.getRecorderId());
                if (recorder != null) {
                    vo.setRecorderName(recorder.getRealName());
                }
            }

            return vo;
        }).collect(Collectors.toList());
    }
}