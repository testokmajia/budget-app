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
    private final TeamRepository teamRepo;
    private final DepartmentRepository deptRepo;
    private final TestReportCommentRepository commentRepo;

    public TestReportService(TestReportRepository reportRepo, RequirementRepository reqRepo,
                             UserRepository userRepo, SystemInfoRepository sysRepo,
                             TeamRepository teamRepo, DepartmentRepository deptRepo,
                             TestReportCommentRepository commentRepo) {
        this.reportRepo = reportRepo;
        this.reqRepo = reqRepo;
        this.userRepo = userRepo;
        this.sysRepo = sysRepo;
        this.teamRepo = teamRepo;
        this.deptRepo = deptRepo;
        this.commentRepo = commentRepo;
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
        m.put("待确认", reportRepo.countByStatus("待确认"));
        m.put("待部门审核", reportRepo.countByStatus("待部门审核"));
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
        r.setReportCode(generateCode());
        r.setTitle(req.getTitle());
        r.setRequirementIds(req.getRequirementIds());
        r.setRequirementCodes(req.getRequirementCodes());
        r.setSystemNames(req.getSystemNames());
        r.setReviewPersons(req.getReviewPersons());
        r.setTestReportPath(req.getTestReportPath());
        if (StringUtils.hasText(req.getTestReportName())) {
            r.setTestReportName(req.getTestReportName());
        }
        r.setStatus("待确认");
        r.setSubmitterId(submitter.getId());
        r.setSubmitterName(submitter.getName());
        if (StringUtils.hasText(req.getPlannedTestDate()))
            r.setPlannedTestDate(LocalDate.parse(req.getPlannedTestDate()));
        if (StringUtils.hasText(req.getPlannedProductionDate()))
            r.setPlannedProductionDate(LocalDate.parse(req.getPlannedProductionDate()));
        reportRepo.save(r);
        addComment(r.getId(), submitter.getName(), "提交人", "提交", "创建测试报告，等待确认人确认");
        log.info("测试报告已创建: {} (编号: {}), 状态: 待确认", r.getTitle(), r.getReportCode());
        return toMap(r);
    }

    // ==================== 确认 ====================

    @Transactional
    public Map<String, Object> confirm(Long id, User operator, String comment, LocalDate confirmedDate) {
        TestReport r = reportRepo.findById(id).orElseThrow(() -> new RuntimeException("测试报告不存在"));
        if (!"待确认".equals(r.getStatus())) {
            throw new RuntimeException("当前状态不允许确认，仅待确认状态的报告可确认");
        }

        // 确认日期默认当天，校验必须早于计划投产日期
        if (confirmedDate == null) {
            confirmedDate = LocalDate.now();
        }
        if (r.getPlannedProductionDate() != null && confirmedDate.isAfter(r.getPlannedProductionDate())) {
            throw new RuntimeException("确认日期（" + confirmedDate + "）不能晚于计划投产日期（" + r.getPlannedProductionDate() + "）");
        }

        // 更新当前用户的确认状态
        List<Map<String, Object>> reviewers = parseReviewPersons(r.getReviewPersons());
        boolean found = false;
        for (Map<String, Object> reviewer : reviewers) {
            if (trim(operator.getName()).equals(trim((String) reviewer.get("name")))) {
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

        // 记录确认操作
        String confirmText = StringUtils.hasText(comment) ? comment : "确认测试报告";
        addComment(r.getId(), operator.getName(), "确认人", "确认", confirmText);

        // 检查是否全部确认
        boolean allConfirmed = reviewers.stream().allMatch(rv -> Boolean.TRUE.equals(rv.get("confirmed")));

        if (allConfirmed) {
            r.setConfirmedAt(LocalDateTime.now());
            // 收集已确认人员姓名（用于跳过已确认的部门负责人）
            Set<String> confirmedNames = new LinkedHashSet<>();
            for (Map<String, Object> rv : reviewers) {
                String name = trim((String) rv.get("name"));
                if (StringUtils.hasText(name)) confirmedNames.add(name);
            }
            // 生成部门审核信息，已确认的部门负责人自动通过
            addDeptReviewers(r, confirmedNames);
            addComment(r.getId(), "系统", "系统", "流转", "全部确认完成，进入部门审核阶段");
            log.info("测试报告 {} 全部确认完成", r.getTitle());
        }

        reportRepo.save(r);
        return toMap(r);
    }

    /** 驳回：重置所有确认状态 */
    @Transactional
    public Map<String, Object> reject(Long id, User operator, String comment) {
        TestReport r = reportRepo.findById(id).orElseThrow(() -> new RuntimeException("测试报告不存在"));
        if (!"待确认".equals(r.getStatus())) {
            throw new RuntimeException("当前状态不允许驳回，仅待确认状态的报告可驳回");
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
        // 记录驳回操作
        String rejectText = StringUtils.hasText(comment) ? comment : "驳回，重置所有确认状态";
        addComment(r.getId(), operator.getName(), "确认人", "驳回", rejectText);
        reportRepo.save(r);
        return toMap(r);
    }

    // ==================== 部门审核 ====================

    /** 从关联需求和部门管理表中提取部门负责人，写入 deptReviewers。
     * @param confirmedNames 已确认的确认人姓名集合，如果部门负责人在其中则自动通过该部门 */
    private void addDeptReviewers(TestReport r, Set<String> confirmedNames) {
        Set<String> depts = new LinkedHashSet<>();
        try {
            List<Long> ids = MAPPER.readValue(r.getRequirementIds(), new TypeReference<List<Long>>() {});
            for (Long reqId : ids) {
                Requirement req = reqRepo.findById(reqId).orElse(null);
                if (req != null && StringUtils.hasText(req.getDept())) {
                    depts.add(req.getDept());
                }
            }
        } catch (JsonProcessingException e) { /* ignore */ }

        // 查找各部门的负责人
        List<String> reviewerNames = new ArrayList<>();
        Map<String, String> deptStatusMap = new LinkedHashMap<>();
        List<Department> departments = deptRepo.findByEnabledTrue();
        List<User> allUsers = userRepo.findByEnabledTrue();

        for (String dept : depts) {
            String leaderName = null;
            boolean found = false;

            // 1. 从部门管理表（Department）查找
            for (Department dep : departments) {
                if (dept.equals(trim(dep.getName())) && StringUtils.hasText(dep.getLeader())) {
                    leaderName = trim(dep.getLeader());
                    found = true;
                    log.info("测试报告 {} 部门 [{}] 负责人: {}（来源：部门管理表）", r.getTitle(), dept, leaderName);
                    break;
                }
            }

            // 2. 从 Team 表查找
            if (!found) {
                List<Team> allTeams = teamRepo.findAllByOrderByIdAsc();
                for (Team team : allTeams) {
                    if (dept.equals(trim(team.getDepartment())) && StringUtils.hasText(team.getLeader())) {
                        leaderName = trim(team.getLeader());
                        found = true;
                        break;
                    }
                }
            }

            // 3. 从 User 表查找（同部门 + position含"负责人/主任/经理"）
            if (!found) {
                for (User u : allUsers) {
                    if (dept.equals(trim(u.getDepartment()))
                            && StringUtils.hasText(u.getPosition())
                            && (u.getPosition().contains("负责人") || u.getPosition().contains("主任")
                                || u.getPosition().contains("经理"))) {
                        leaderName = trim(u.getName());
                        found = true;
                        break;
                    }
                }
            }

            // 4. 兜底：从 User 表查找同部门 + 具有管理角色
            if (!found) {
                for (User u : allUsers) {
                    if (dept.equals(trim(u.getDepartment()))) {
                        boolean isLeader = u.getRoles().stream()
                            .anyMatch(role -> role.getName().equals("ROLE_LEADER")
                                || role.getName().equals("ROLE_ADMIN")
                                || role.getName().equals("ROLE_CLERK"));
                        if (isLeader) {
                            leaderName = trim(u.getName());
                            found = true;
                            break;
                        }
                    }
                }
            }

            if (found && leaderName != null) {
                if (!reviewerNames.contains(leaderName)) {
                    reviewerNames.add(leaderName);
                }
                // 关键逻辑：如果该部门负责人已经在确认人中确认过 → 自动通过
                if (confirmedNames != null && confirmedNames.contains(leaderName)) {
                    deptStatusMap.put(dept, "approved");
                    log.info("测试报告 {} 部门 [{}] 负责人 {} 已在确认人员中，自动通过部门审核", r.getTitle(), dept, leaderName);
                } else {
                    deptStatusMap.put(dept, "pending");
                }
            } else {
                deptStatusMap.put(dept, "pending");
                log.warn("测试报告 {} 的部门 [{}] 未找到负责人", r.getTitle(), dept);
            }
        }

        try {
            r.setDeptReviewers(MAPPER.writeValueAsString(reviewerNames));
            r.setDeptReviewStatus(MAPPER.writeValueAsString(deptStatusMap));
            log.info("测试报告 {} 部门审核人: {}, 状态: {}", r.getTitle(), reviewerNames, deptStatusMap);
        } catch (JsonProcessingException e) {
            log.warn("序列化部门审核信息失败");
        }

        // 检查是否全部部门已通过（含自动通过）
        boolean allApproved = deptStatusMap.values().stream().allMatch("approved"::equals);
        if (allApproved) {
            r.setStatus("已确认");
            addComment(r.getId(), "系统", "系统", "流转", "全部部门审核通过（含自动通过的已确认部门负责人），报告已确认");
            updateRequirementsStatus(r.getRequirementIds(), "测试通过");
            log.info("测试报告 {} 全部部门审核通过，关联需求状态已更新为测试通过", r.getTitle());
        } else {
            r.setStatus("待部门审核");
        }
    }

    /** 部门负责人审核测试报告 */
    @Transactional
    public Map<String, Object> deptReview(Long id, User operator, String comment, boolean approved) {
        TestReport r = reportRepo.findById(id).orElseThrow(() -> new RuntimeException("测试报告不存在"));
        if (!"待部门审核".equals(r.getStatus())) {
            throw new RuntimeException("当前状态不允许部门审核");
        }

        // 校验操作人是部门审核人
        List<String> deptReviewers = parseStringList(r.getDeptReviewers());
        String operatorName = trim(operator.getName());
        boolean isDeptReviewer = deptReviewers.stream().anyMatch(name -> trim(name).equals(operatorName));
        if (!isDeptReviewer) {
            throw new RuntimeException("您不是部门审核人，无法审核");
        }

        // 更新部门审核状态
        Map<String, String> deptStatusMap = parseDeptReviewStatus(r.getDeptReviewStatus());
        // 找到当前用户对应的部门
        String userDept = operator.getDepartment();
        String deptKey = userDept;
        if (!deptStatusMap.containsKey(deptKey)) {
            for (SystemInfo sys : sysRepo.findByEnabledTrue()) {
                if (trim(operator.getName()).equals(trim(sys.getLeader()))) {
                    deptKey = sys.getName();
                    break;
                }
            }
        }

        if (approved) {
            deptStatusMap.put(deptKey, "approved");
            String text = StringUtils.hasText(comment) ? comment : "审核通过。";
            String deptLabel = deptKey != null ? deptKey : "";
            addComment(r.getId(), operator.getName(), "部门负责人(" + deptLabel + ")", "部门审核通过", text);
        } else {
            deptStatusMap.put(deptKey, "rejected");
            String rejectText = StringUtils.hasText(comment) ? comment : "审核驳回，重置所有确认状态";
            String deptLabel = deptKey != null ? deptKey : "";
            addComment(r.getId(), operator.getName(), "部门负责人(" + deptLabel + ")", "部门审核驳回", rejectText);
            // 驳回：重置所有确认状态，退回到待确认
            List<Map<String, Object>> reviewers = parseReviewPersons(r.getReviewPersons());
            for (Map<String, Object> reviewer : reviewers) {
                reviewer.put("confirmed", false);
                reviewer.put("confirmedAt", null);
            }
            try {
                r.setReviewPersons(MAPPER.writeValueAsString(reviewers));
            } catch (JsonProcessingException e) { /* ignore */ }
            r.setStatus("待确认");
            r.setConfirmedAt(null);
            r.setDeptReviewers(null);
            try {
                r.setDeptReviewStatus(MAPPER.writeValueAsString(deptStatusMap));
            } catch (JsonProcessingException e) { /* ignore */ }
            reportRepo.save(r);
            return toMap(r);
        }

        try {
            r.setDeptReviewStatus(MAPPER.writeValueAsString(deptStatusMap));
        } catch (JsonProcessingException e) { /* ignore */ }

        // 检查是否全部部门审核通过
        boolean allApproved = deptStatusMap.values().stream().allMatch("approved"::equals);

        if (allApproved) {
            r.setStatus("已确认");
            addComment(r.getId(), "系统", "系统", "流转", "全部部门审核通过，报告已确认");
            updateRequirementsStatus(r.getRequirementIds(), "测试通过");
            log.info("测试报告 {} 全部部门审核通过，关联需求状态已更新为测试通过", r.getTitle());
        }

        reportRepo.save(r);
        return toMap(r);
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    private Map<String, String> parseDeptReviewStatus(String json) {
        if (!StringUtils.hasText(json)) return new LinkedHashMap<>();
        try {
            return MAPPER.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            return new LinkedHashMap<>();
        }
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

                // 1. 项目经理：优先从系统项的 pm 获取，兜底 assignedPm
                if (StringUtils.hasText(req.getSystemItems())) {
                    try {
                        List<Map<String, Object>> systems = MAPPER.readValue(req.getSystemItems(),
                                new TypeReference<List<Map<String, Object>>>() {});
                        for (Map<String, Object> sys : systems) {
                            String pm = (String) sys.get("pm");
                            String pd = (String) sys.get("pd");
                            String owner = (String) sys.get("owner");
                            String sysName = (String) sys.get("name");
                            if (StringUtils.hasText(pm) && seen.add("pm:" + pm)) {
                                reviewers.add(buildReviewer(pm, "项目经理", sysName));
                            }
                            if (StringUtils.hasText(pd) && seen.add("pd:" + pd)) {
                                reviewers.add(buildReviewer(pd, "产品经理", sysName));
                            }
                            if (StringUtils.hasText(owner) && seen.add("owner:" + owner)) {
                                reviewers.add(buildReviewer(owner, "系统负责人", sysName));
                            }
                        }
                    } catch (JsonProcessingException e) { /* ignore */ }
                }
                // 兜底：assignedPm（团队组长在审批时指派）
                if (StringUtils.hasText(req.getAssignedPm()) && seen.add("assignedPm:" + req.getAssignedPm())) {
                    reviewers.add(buildReviewer(req.getAssignedPm(), "项目经理", req.getPrimarySystemName()));
                }
                // 兜底：assignedPd（团队组长在审批时指派）
                if (StringUtils.hasText(req.getAssignedPd()) && seen.add("assignedPd:" + req.getAssignedPd())) {
                    reviewers.add(buildReviewer(req.getAssignedPd(), "产品经理", req.getPrimarySystemName()));
                }

                // 2. 业务提出人
                if (StringUtils.hasText(req.getSubmitterName()) && seen.add("submitter:" + req.getSubmitterName())) {
                    reviewers.add(buildReviewer(req.getSubmitterName(), "业务提出人", req.getDept()));
                }

                // 3. 部门负责人（从 Department 表查找）
                String dept = req.getDept();
                if (StringUtils.hasText(dept)) {
                    List<Department> departments = deptRepo.findByEnabledTrue();
                    for (Department dep : departments) {
                        if (dept.equals(trim(dep.getName())) && StringUtils.hasText(dep.getLeader())
                                && seen.add("deptLeader:" + trim(dep.getLeader()))) {
                            reviewers.add(buildReviewer(trim(dep.getLeader()), "部门负责人", dept));
                            break;
                        }
                    }
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

    private String generateCode() {
        String prefix = "TR-" + java.time.Year.now() + "-";
        return reportRepo.findMaxCodeByPrefix(prefix)
                .map(max -> {
                    String num = max.substring(prefix.length());
                    return prefix + String.format("%03d", Integer.parseInt(num) + 1);
                })
                .orElse(prefix + "001");
    }

    private Map<String, Object> toMap(TestReport r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getId());
        m.put("reportCode", r.getReportCode());
        m.put("title", r.getTitle());
        m.put("requirementIds", r.getRequirementIds());
        m.put("requirementCodes", r.getRequirementCodes());
        m.put("systemNames", r.getSystemNames());
        m.put("reviewPersons", r.getReviewPersons());
        m.put("testReportPath", r.getTestReportPath());
        m.put("testReportName", r.getTestReportName());
        m.put("status", r.getStatus());
        m.put("submitterId", r.getSubmitterId());
        m.put("submitterName", r.getSubmitterName());
        m.put("plannedTestDate", r.getPlannedTestDate());
        m.put("plannedProductionDate", r.getPlannedProductionDate());
        m.put("confirmedAt", r.getConfirmedAt());
        m.put("deptReviewStatus", r.getDeptReviewStatus());
        m.put("deptReviewers", r.getDeptReviewers());
        m.put("createdAt", r.getCreatedAt());
        return m;
    }

    /** 安全的 trim，null 安全 */
    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    // ==================== 审核记录 ====================

    /** 添加审核记录 */
    private void addComment(Long reportId, String author, String role, String action, String content) {
        TestReportComment c = new TestReportComment();
        c.setReportId(reportId);
        c.setAuthor(author);
        c.setRole(role);
        c.setAction(action);
        c.setContent(content);
        commentRepo.save(c);
    }

    /** 获取测试报告的审核记录列表 */
    public List<Map<String, Object>> getComments(Long reportId) {
        return commentRepo.findByReportIdOrderByCreatedAtAsc(reportId)
                .stream().map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("author", c.getAuthor());
                    m.put("role", c.getRole());
                    m.put("action", c.getAction());
                    m.put("content", c.getContent());
                    m.put("createdAt", c.getCreatedAt());
                    return m;
                }).collect(Collectors.toList());
    }
}
