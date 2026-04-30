package com.duke.auth.service.impl;

import com.duke.auth.dto.DeptDTO;
import com.duke.auth.entity.SysDept;
import com.duke.auth.entity.SysUserDept;
import com.duke.framework.exception.BusinessException;
import com.duke.auth.mapper.SysDeptMapper;
import com.duke.auth.mapper.SysUserDeptMapper;
import com.duke.auth.service.IDeptService;
import com.duke.auth.vo.DeptTreeVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptServiceImpl implements IDeptService {

    private final SysDeptMapper deptMapper;
    private final SysUserDeptMapper userDeptMapper;

    @Override
    public List<DeptTreeVO> tree() {
        List<SysDept> all = deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSortOrder));
        return buildTree(all, 0L);
    }

    @Override
    @Transactional
    public void create(DeptDTO dto) {
        SysDept dept = new SysDept();
        dept.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        dept.setDeptName(dto.getDeptName());
        dept.setDeptCode(dto.getDeptCode());
        dept.setLeader(dto.getLeader());
        dept.setPhone(dto.getPhone());
        dept.setEmail(dto.getEmail());
        dept.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        dept.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        // 构建祖级列表
        if (dept.getParentId() == 0L) {
            dept.setAncestors("0");
        } else {
            SysDept parent = deptMapper.selectById(dept.getParentId());
            if (parent == null) throw new BusinessException("父部门不存在");
            dept.setAncestors(parent.getAncestors() + "," + parent.getId());
        }
        deptMapper.insert(dept);
    }

    @Override
    @Transactional
    public void update(DeptDTO dto) {
        SysDept dept = deptMapper.selectById(dto.getId());
        if (dept == null) throw new BusinessException("部门不存在");
        if (StringUtils.hasText(dto.getDeptName())) dept.setDeptName(dto.getDeptName());
        if (StringUtils.hasText(dto.getDeptCode())) dept.setDeptCode(dto.getDeptCode());
        if (StringUtils.hasText(dto.getLeader())) dept.setLeader(dto.getLeader());
        if (StringUtils.hasText(dto.getPhone())) dept.setPhone(dto.getPhone());
        if (StringUtils.hasText(dto.getEmail())) dept.setEmail(dto.getEmail());
        if (dto.getSortOrder() != null) dept.setSortOrder(dto.getSortOrder());
        if (dto.getStatus() != null) dept.setStatus(dto.getStatus());
        deptMapper.updateById(dept);
    }

    @Override
    public void delete(Long id) {
        // 有子部门不允许删除
        if (deptMapper.selectCount(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id)) > 0) {
            throw new BusinessException("存在子部门，不允许删除");
        }
        // 有用户不允许删除
        if (userDeptMapper.selectCount(new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getDeptId, id)) > 0) {
            throw new BusinessException("部门下存在用户，不允许删除");
        }
        deptMapper.deleteById(id);
    }

    private List<DeptTreeVO> buildTree(List<SysDept> all, Long parentId) {
        return all.stream()
                .filter(d -> parentId.equals(d.getParentId()))
                .map(d -> {
                    DeptTreeVO vo = new DeptTreeVO();
                    vo.setId(d.getId());
                    vo.setParentId(d.getParentId());
                    vo.setDeptName(d.getDeptName());
                    vo.setDeptCode(d.getDeptCode());
                    vo.setLeader(d.getLeader());
                    vo.setSortOrder(d.getSortOrder());
                    vo.setStatus(d.getStatus());
                    vo.setChildren(buildTree(all, d.getId()));
                    return vo;
                })
                .collect(Collectors.toList());
    }
}
