package com.techmanage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmanage.dto.TestReportRequest;
import com.techmanage.entity.*;
import com.techmanage.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TestReportService {

    private static final Logger log = LoggerFactory.getLogger(TestReportService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TestReportRepository reportRepo;
    private final RequirementRepository reqRepo;
    private final UserRepository userRepo;
    private final SystemInfoRepository sysRepo;

    public TestReportService(TestReportRepository reportRepo, RequirementRepository reqRepo,
                             UserRepository userRepo, SystemInfoRepository sysRepo) {
        this.reportRepo = reportRepo;
        this.reqRepo = reqRepo;
        this.userRepo = userRepo;
        this.sysRepo = sysRepo;
    }

    // ==================== 查询 ====================

    public Page<Map<String, Object>> list(String keyword, List<String> statuses,
            Long submitterId, String systemName, String requirementTitle,
            LocalDate createdAtFrom, LocalDate createdAtTo,
            LocalDate confirmedAtFrom, LocalDate confirmedAtTo,
            int page, int size) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);
        Specification<TestReport> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (StringUtils.hasText(keyword)) {
                String kw = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("title")), kw),
                    cb.like(cb.lower(root.get("requirementCodes")), kw)
                ));
            }
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("status").in(statuses));
            }
            if (submitterId != null) {
                predicates.add(cb.equal(root.get("submitterId"), submitterId));
            }
            if (StringUtils.hasText(systemName)) {
                String kw = "%" + systemName + "%";
                predicates.add(cb.like(root.get("systemNames"), kw));
            }
            if (StringUtils.hasText(requirementTitle)) {
                String kw = "%" + requirementTitle.toLowerCase() + "%";
                predicates.add(cb.like(cb.lower(root.get("requirementCodes")), kw));
            }
            if (createdAtFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdAtFrom.atStartOfDay()));
            }
            if (createdAtTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), createdAtTo.atTime(23, 59, 59)));
            }
            if (confirmedAtFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("confirmedAt"), confirmedAtFrom.atStartOfDay()));
            }
            if (confirmedAtTo != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("confirmedAt"), confirmedAtTo.atTime(23, 59, 59)));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return reportRepo.findAll(spec, pageable).map(this::toMap);
    }

    public Map<String, Object> getDetail(Long id) {
        TestReport r = reportRepo.findById(id).orElseThrow(() -> new RuntimeException("测试报告不存在"));
        return toMap(r);
    }

    public Map<String, Long> stats() {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("全部", reportRepo.count());
        m.put("草稿", reportRepo.countByStatus("草稿"));
        m.put("已确认", reportRepo.countByStatus("已确认"));
        return m;
    }

    /** 获取需求关联的测试报告列表 */
    public List<Map<String, Object>> getByRequirement(Long requirementId) {
        return reportRepo.findByRequirementIdContaining(String.valueOf(requirementId))
                .stream().map(this::toMap).collect(Collectors.toList());
    }

    // ==================== 创建 ====================

    @Transactional
    public Map<String, Object> create(TestReportRequest req, User submitter) {
        TestReport r = new TestReport();
        r.setTitle(req.getTitle());
        r.setRequirementIds(req.getRequirementIds());
        r.setRequirementCodes(req.getRequirementCodes());
        r.setSystemNames(req.getSystemNames());
        r.setReviewPersons(req.getReviewPersons());
        r.setTestReportPath(req.getTestReportPath());
        r.setStatus("草稿");
        r.setSubmitterId(submitter.getId());
        r.setSubmitterName(submitter.getName());
        if (StringUtils.hasText(req.getPlannedTestDate()))
            r.setPlannedTestDate(LocalDate.parse(req.getPlannedTestDate()));
        if (StringUtils.hasText(req.getPlannedProductionDate()))
            r.setPlannedProductionDate(LocalDate.parse(req.getPlannedProductionDate()));
        reportRepo.save(r);
        log.info("测试报告已创建: {}", r.getTitle());
        return toMap(r);
    }

    // ==================== 确认 ====================

    @Transactional
    public Map<String, Object> confirm(Long id, User operator, String comment, LocalDate confirmedDate) {
        TestReport r = reportRepo.findById(id).orElseThrow(() -> new RuntimeException("测试报告不存在"));
        if (!"草稿".equals(r.getStatus())) {
            throw new RuntimeException("当前状态不允许确认");
        }

        // 确认日期默认当天，校验必须早于计划投产日期
        if (confirmedDate == null) {
            confirmedDate = LocalDate.now();
        }
        if (r.getPlannedProductionDate() != null && !confirmedDate.isBefore(r.getPlannedProductionDate())) {
            throw new RuntimeException("确认日期（" + confirmedDate + "）必须早于计划投产日期（" + r.getPlannedProductionDate() + "）");
        }

        // 更新当前用户的确认状态
        List<Map<String, Object>> reviewers = parseReviewPersons(r.getReviewPersons());
        boolean found = false;
        for (Map<String, Object> reviewer : reviewers) {
            if (operator.getName().equals(reviewer.get("name"))) {
                reviewer.put("confirmed", true);
                reviewer.put("confirmedAt", LocalDateTime.now().toString());
                reviewer.put("confirmedDate", confirmedDate.toString());
                found = true;
                // 不break：同一人有多个角色时，一键全部确认
            }
        }
        if (!found) {
            throw new RuntimeException("您不在确认人员列表中");
        }

        try {
            r.setReviewPersons(MAPPER.writeValueAsString(reviewers));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化确认人员失败");
        }

        // 检查是否全部确认
        boolean allConfirmed = reviewers.stream().allMatch(rv -> Boolean.TRUE.equals(rv.get("confirmed")));

        if (allConfirmed) {
            r.setStatus("已确认");
            r.setConfirmedAt(LocalDateTime.now());
            // 所有关联需求 → 测试通过
            updateRequirementsStatus(r.getRequirementIds(), "测试通过");
            log.info("测试报告 {} 全部确认完成，关联需求状态已更新为测试通过", r.getTitle());
        }

        reportRepo.save(r);
        return toMap(r);
    }

    /** 驳回：重置所有确认状态 */
    @Transactional
    public Map<String, Object> reject(Long id, User operator, String comment) {
        TestReport r = reportRepo.findById(id).orElseThrow(() -> new RuntimeException("测试报告不存在"));
        if (!"草稿".equals(r.getStatus())) {
            throw new RuntimeException("当前状态不允许驳回");
        }

        List<Map<String, Object>> reviewers = parseReviewPersons(r.getReviewPersons());
        for (Map<String, Object> reviewer : reviewers) {
            reviewer.put("confirmed", false);
            reviewer.put("confirmedAt", null);
        }
        try {
            r.setReviewPersons(MAPPER.writeValueAsString(reviewers));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化确认人员失败");
        }
        r.setConfirmedAt(null);
        reportRepo.save(r);
        return toMap(r);
    }

    // ==================== 自动带出确认人员 ====================

    /** 根据选中的需求ID，自动生成确认人员列表 */
    public List<Map<String, Object>> getDefaultReviewers(String requirementIdsJson) {
        List<Map<String, Object>> reviewers = new ArrayList<>();
        Set<String> seen = new HashSet<>(); // 去重

        try {
            List<Long> ids = MAPPER.readValue(requirementIdsJson, new TypeReference<List<Long>>() {});
            for (Long reqId : ids) {
                Requirement req = reqRepo.findById(reqId).orElse(null);
                if (req == null) continue;

                // 项目经理（从涉及系统中提取）
                if (StringUtils.hasText(req.getSystemItems())) {
                    try {
                        List<Map<String, Object>> systems = MAPPER.readValue(req.getSystemItems(),
                                new TypeReference<List<Map<String, Object>>>() {});
                        for (Map<String, Object> sys : systems) {
                            String pm = (String) sys.get("pm");
                            String pd = (String) sys.get("pd");
                            String owner = (String) sys.get("owner");
                            String sysName = (String) sys.get("name");
                            // 项目经理
                            if (StringUtils.hasText(pm) && seen.add("pm:" + pm)) {
                                reviewers.add(buildReviewer(pm, "项目经理", sysName));
                            }
                            // 产品经理
                            if (StringUtils.hasText(pd) && seen.add("pd:" + pd)) {
                                reviewers.add(buildReviewer(pd, "产品经理", sysName));
                            }
                            // 系统负责人
                            if (StringUtils.hasText(owner) && seen.add("owner:" + owner)) {
                                reviewers.add(buildReviewer(owner, "系统负责人", sysName));
                            }
                        }
                    } catch (JsonProcessingException e) { /* ignore */ }
                }

                // 团队组长（从涉及系统的team字段获取，这里简单取系统负责人作为团队负责人）
                // 业务人员（提出人）
                if (StringUtils.hasText(req.getSubmitterName()) && seen.add("submitter:" + req.getSubmitterName())) {
                    reviewers.add(buildReviewer(req.getSubmitterName(), "业务人员", req.getDept()));
                }
                // 部门负责人：从部门表查询leader
                String dept = req.getDept();
                if (StringUtils.hasText(dept)) {
                    // 尝试从SystemInfo或User中查找部门负责人
                    // 简化处理：查找同部门角色为"部门负责人"的用户
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("解析需求ID列表失败");
        }

        return reviewers;
    }

    private Map<String, Object> buildReviewer(String name, String role, String system) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("name", name);
        r.put("role", role);
        r.put("system", system != null ? system : "");
        r.put("confirmed", false);
        r.put("confirmedAt", null);
        return r;
    }

    // ==================== 私有方法 ====================

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseReviewPersons(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    /** 批量更新关联需求的状态 */
    private void updateRequirementsStatus(String requirementIdsJson, String newStatus) {
        try {
            List<Long> ids = MAPPER.readValue(requirementIdsJson, new TypeReference<List<Long>>() {});
            for (Long id : ids) {
                reqRepo.findById(id).ifPresent(req -> {
                    req.setStatus(newStatus);
                    req.setCurrentNode(newStatus);
                    reqRepo.save(req);
                });
            }
        } catch (JsonProcessingException e) {
            log.warn("解析需求ID列表失败");
        }
    }

    private Map<String, Object> toMap(TestReport r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("title", r.getTitle());
        m.put("requirementIds", r.getRequirementIds());
        m.put("requirementCodes", r.getRequirementCodes());
        m.put("systemNames", r.getSystemNames());
        m.put("reviewPersons", r.getReviewPersons());
        m.put("testReportPath", r.getTestReportPath());
        m.put("status", r.getStatus());
        m.put("submitterId", r.getSubmitterId());
        m.put("submitterName", r.getSubmitterName());
        m.put("plannedTestDate", r.getPlannedTestDate());
        m.put("plannedProductionDate", r.getPlannedProductionDate());
        m.put("confirmedAt", r.getConfirmedAt());
        m.put("createdAt", r.getCreatedAt());
        return m;
    }
}
