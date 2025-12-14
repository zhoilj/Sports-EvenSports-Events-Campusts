package com.sports.modules.result.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.sports.modules.result.entity.CompetitionResult;
import com.sports.modules.result.vo.CompetitionResultVO;

import java.util.List;

/**
 * 比赛成绩 Service
 *
 * @author sports
 */
public interface CompetitionResultService extends IService<CompetitionResult> {

    /**
     * 分页查询成绩
     */
    IPage<CompetitionResult> pageResults(Integer current, Integer size, Long eventId, Long sportItemId, Long userId);

    /**
     * 分页查询成绩（返回VO）
     */
    IPage<CompetitionResultVO> pageResultsVO(Integer current, Integer size, Long eventId, Long sportItemId, Long userId);

    /**
     * 根据赛事和项目获取成绩排名
     */
    List<CompetitionResult> getRankingByEventAndItem(Long eventId, Long sportItemId);

    /**
     * 获取我的成绩列表
     */
    List<CompetitionResult> getMyResults(Long userId);

    /**
     * 获取我的成绩列表（返回VO）
     */
    List<CompetitionResultVO> getMyResultsVO(Long userId);

    /**
     * 批量录入成绩
     */
    void batchSaveResults(List<CompetitionResult> results);
}