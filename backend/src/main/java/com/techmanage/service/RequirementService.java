package com.techmanage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techmanage.dto.RequirementRequest;
import com.techmanage.dto.RequirementResponse;
import com.techmanage.entity.*;
import com.techmanage.repository.RequirementCommentRepository;
import com.techmanage.repository.RequirementRepository;
import com.techmanage.repository.SystemInfoRepository;
import com.techmanage.repository.TeamRepository;
import com.techmanage.repository.UserRepository;
import com.techmanage.util.TeamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RequirementService {

    private static final Logger log = LoggerFactory.getLogger(RequirementService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 审批节点定义（0-based）
     * 0=提出人, 1=部门负责人, 2=架构管理岗, 3=团队组长,
     * 4=产品经理, 5=多方确认
     */
    private static final String[] NODES = {
        "提出人", "部门负责人", "架构管理岗", "团队组长",
        "产品经理", "多方确认"
    };

    private final RequirementRepository requirementRepository;
    private final RequirementCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final SystemInfoRepository systemInfoRepository;
    private final TeamRepository teamRepository;

    public RequirementService(RequirementRepository requirementRepository,
                              RequirementCommentRepository commentRepository,
                              UserRepository userRepository,
                              SystemInfoRepository systemInfoRepository,
                              TeamRepository teamRepository) {
        this.requirementRepository = requirementRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.systemInfoRepository = systemInfoRepository;
        this.teamRepository = teamRepository;
    }

    /**
     * 数据迁移：修正旧数据的 nodeIndex（项目启动时自动执行）
     * 旧流程（7节点）：提出人(0)→部门负责人(1)→架构管理岗(2)→团队组长(3)→项目经理(4)→产品经理(5)→多方确认(6)
     * 新流程（6节点）：提出人(0)→部门负责人(1)→架构管理岗(2)→团队组长(3)→产品经理(4)→多方确认(5)
     */
    @PostConstruct
    @Transactional
    public void migrateNodeIndices() {
        try {
            List<Requirement> all = requirementRepository.findAll();
            int migrated = 0;
            for (Requirement r : all) {
                boolean changed = false;
                // 旧 nodeIndex=4（项目经理）：PD已指派→改为4（产品经理）；PD未指派→退回3（团队组长）
                if (r.getNodeIndex() != null && r.getNodeIndex() == 4
                        && "项目经理".equals(r.getCurrentNode())) {
                    if (StringUtils.hasText(getPrimarySystemPd(r))) {
                        r.setNodeIndex(4);
                        r.setCurrentNode(NODES[4]); // 产品经理
                    } else {
                        r.setNodeIndex(3);
                        r.setCurrentNode(NODES[3]); // 团队组长
                    }
                    changed = true;
                }
                // 旧 nodeIndex=5（产品经理）→ 4
                else if (r.getNodeIndex() != null && r.getNodeIndex() == 5
                        && "产品经理".equals(r.getCurrentNode())) {
                    r.setNodeIndex(4);
                    r.setCurrentNode(NODES[4]);
                    changed = true;
                }
                // 旧 nodeIndex=6（多方确认）→ 5
                if (r.getNodeIndex() != null && r.getNodeIndex() == 6
                        && "多方确认".equals(r.getCurrentNode())) {
                    r.setNodeIndex(5);
                    r.setCurrentNode(NODES[5]);
                    changed = true;
                }
                if (changed) {
                    requirementRepository.save(r);
                    migrated++;
                }
            }
            if (migrated > 0) {
                log.info("数据迁移完成：已修正 {} 条需求的节点索引", migrated);
            }
        } catch (Exception e) {
            log.warn("数据迁移失败（可忽略，通常因为表尚未创建）: {}", e.getMessage());
        }
    }

    // ==================== 查询 ====================

    public Page<RequirementResponse> list(String keyword, String status, String priority,
                                          String dept, Long submitterId, String sysOwner,
                                          LocalDate dateFrom, LocalDate dateTo,
                                          int page, int size, String sortBy, String sortDir) {
        Sort sort = Sort.by("desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy != null ? sortBy : "createdAt");
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Requirement> spec = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (StringUtils.hasText(keyword)) {
                String kw = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("requirementCode")), kw),
                    cb.like(cb.lower(root.get("title")), kw)
                ));
            }
            if (StringUtils.hasText(status)) predicates.add(cb.equal(root.get("status"), status));
            if (StringUtils.hasText(priority)) predicates.add(cb.equal(root.get("priority"), priority));
            if (StringUtils.hasText(dept)) predicates.add(cb.equal(root.get("dept"), dept));
            if (submitterId != null) predicates.add(cb.equal(root.get("submitterId"), submitterId));
            if (StringUtils.hasText(sysOwner)) predicates.add(cb.like(root.get("systemItems"), "%" + sysOwner + "%"));
            if (dateFrom != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay()));
            if (dateTo != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo.atTime(23, 59, 59)));
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        return requirementRepository.findAll(spec, pageable).map(this::toResponse);
    }

    public RequirementResponse getDetail(Long id) {
        Requirement req = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("需求不存在: id=" + id));
        RequirementResponse resp = toResponse(req);
        List<RequirementComment> comments = commentRepository.findByRequirementIdOrderByCreatedAtAsc(id);
        resp.setComments(comments.stream().map(c -> new RequirementResponse.CommentItem(
                c.getAuthor(), c.getRole(), c.getAction(), c.getContent(), c.getCreatedAt()
        )).collect(Collectors.toList()));
        return resp;
    }

    public java.util.Map<String, Long> stats() {
        java.util.Map<String, Long> map = new java.util.LinkedHashMap<>();
        map.put("全部", requirementRepository.count());
        map.put("草稿", requirementRepository.countByStatus("草稿"));
        map.put("审批中", requirementRepository.countByStatus("审批中"));
        map.put("需求已确认", requirementRepository.countByStatus("需求已确认"));
        map.put("实施中", requirementRepository.countByStatus("实施中"));
        map.put("测试通过", requirementRepository.countByStatus("测试通过"));
        map.put("已投产", requirementRepository.countByStatus("已投产"));
        map.put("已关闭", requirementRepository.countByStatus("已关闭"));
        map.put("已驳回", requirementRepository.countByStatus("已驳回"));
        return map;
    }

    // ==================== 核心操作 ====================

    /**
     * 创建需求 → 流转至部门负责人，支持上传需求说明书
     */
    @Transactional
    public RequirementResponse create(User submitter, RequirementRequest request) {
        Requirement req = new Requirement();
        req.setRequirementCode(generateCode());
        req.setTitle(request.getTitle());
        req.setContent(request.getContent());
        req.setPriority(request.getPriority() != null ? request.getPriority() : "普通");
        req.setSubmitterId(submitter.getId());
        req.setSubmitterName(submitter.getName());
        req.setDept(submitter.getDepartment());
        req.setStatus("审批中");
        req.setCurrentNode(NODES[1]); // 部门负责人
        req.setNodeIndex(1);
        if (StringUtils.hasText(request.getExpectedDate())) {
            req.setExpectedDate(LocalDate.parse(request.getExpectedDate()));
        }
        // 支持新建时上传需求说明书
        if (StringUtils.hasText(request.getSpecDocumentPath())) {
            req.setSpecDocumentPath(request.getSpecDocumentPath());
        }
        requirementRepository.save(req);
        addComment(req.getId(), submitter.getName(), "提出人", "提交", "提交需求申请。");
        log.info("需求已创建: {}", req.getRequirementCode());
        return toResponse(req);
    }

    /**
     * 通用审批通过 → 流转至下一节点
     * nodeIndex 是当前节点在 NODES 中的 0-based 索引
     */
    @Transactional
    public RequirementResponse approve(Long id, User operator, String comment) {
        Requirement req = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("需求不存在"));

        String currentNode = req.getCurrentNode();
        int currentIndex = req.getNodeIndex();
        String text = StringUtils.hasText(comment) ? comment : "审批通过。";
        addComment(id, operator.getName(), currentNode, "通过", text);

        int nextIndex = currentIndex + 1; // 直接推进到下一个节点
        if (nextIndex >= NODES.length) {
            req.setStatus("需求已确认");
            req.setCurrentNode(NODES[NODES.length - 1]);
            req.setNodeIndex(NODES.length - 1);
        } else {
            req.setCurrentNode(NODES[nextIndex]);
            req.setNodeIndex(nextIndex);
        }

        requirementRepository.save(req);
        log.info("需求 {} 审批通过，流转至: {}", req.getRequirementCode(), req.getCurrentNode());
        return getDetail(id);
    }

    /**
     * 驳回至主责系统的产品经理
     */
    @Transactional
    public RequirementResponse reject(Long id, User operator, String comment) {
        Requirement req = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("需求不存在"));
        String text = StringUtils.hasText(comment) ? comment : "驳回。";
        addComment(id, operator.getName(), req.getCurrentNode(), "驳回", text);

        // 驳回至主责系统的产品经理
        String pdName = getPrimarySystemPd(req);
        if (StringUtils.hasText(pdName)) {
            req.setCurrentNode(NODES[4]); // 产品经理
            req.setNodeIndex(4);
            req.setStatus("审批中");
            addComment(id, "系统", "系统", "转交", "驳回至主责系统产品经理：" + pdName);
        } else {
            // 没有PD则驳回至提出人
            req.setStatus("已驳回");
            req.setCurrentNode(NODES[0]);
            req.setNodeIndex(0);
        }
        requirementRepository.save(req);
        return getDetail(id);
    }

    /** 获取主责系统的产品经理 */
    private String getPrimarySystemPd(Requirement req) {
        if (!StringUtils.hasText(req.getSystemItems())) return null;
        try {
            List<RequirementResponse.SystemItem> items = MAPPER.readValue(
                    req.getSystemItems(), new TypeReference<List<RequirementResponse.SystemItem>>() {});
            String primary = req.getPrimarySystemName();
            for (RequirementResponse.SystemItem item : items) {
                if (item.name().equals(primary) && StringUtils.hasText(item.pd())) {
                    return item.pd();
                }
            }
            // 找不到主责系统的PD，返回第一个有PD的
            for (RequirementResponse.SystemItem item : items) {
                if (StringUtils.hasText(item.pd())) return item.pd();
            }
        } catch (JsonProcessingException e) { /* ignore */ }
        return null;
    }

    /**
     * 架构管理岗提交评估：选择涉及系统 + 指定主责系统
     */
    @Transactional
    public RequirementResponse evaluate(Long id, User operator, String systemItemsJson,
                                         String primarySystemName, String comment) {
        Requirement req = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("需求不存在"));
        if (!"架构管理岗".equals(req.getCurrentNode())) {
            throw new RuntimeException("当前节点不是架构管理岗");
        }
        req.setSystemItems(systemItemsJson);
        req.setPrimarySystemName(primarySystemName);
        String text = StringUtils.hasText(comment) ? comment : "系统评估完成，主责系统：" + primarySystemName;
        addComment(id, operator.getName(), "架构管理岗", "评估", text);
        // 流转至团队组长
        req.setCurrentNode(NODES[3]);
        req.setNodeIndex(3);
        requirementRepository.save(req);
        return getDetail(id);
    }

    /**
     * 团队组长审批：按涉及系统指派项目经理和产品经理（一步完成）
     * 如果涉及多个团队的系统，不同团队负责人分别指派各自的系统
     * 项目经理默认取系统负责人
     */
    @Transactional
    public RequirementResponse teamLeaderAssign(Long id, User operator, String systemItemsJson, String comment) {
        Requirement req = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("需求不存在"));
        if (!"团队组长".equals(req.getCurrentNode())) {
            throw new RuntimeException("当前节点不是团队组长");
        }

        // 获取当前用户所属团队
        String userTeam = TeamUtils.findTeamNameForUser(
                teamRepository.findAllByOrderByIdAsc(), operator.getName());

        // 合并PM/PD到系统列表
        if (StringUtils.hasText(systemItemsJson)) {
            try {
                List<RequirementResponse.SystemItem> incoming = MAPPER.readValue(
                        systemItemsJson, new TypeReference<List<RequirementResponse.SystemItem>>() {});

                List<RequirementResponse.SystemItem> existing = new ArrayList<>();
                if (StringUtils.hasText(req.getSystemItems())) {
                    existing = new ArrayList<>(MAPPER.readValue(
                            req.getSystemItems(), new TypeReference<List<RequirementResponse.SystemItem>>() {}));
                }

                // 合并：更新匹配的系统（按名称匹配）
                for (RequirementResponse.SystemItem inc : incoming) {
                    for (int j = 0; j < existing.size(); j++) {
                        if (existing.get(j).name().equals(inc.name())) {
                            // PM为空时默认取系统负责人
                            String pm = StringUtils.hasText(inc.pm()) ? inc.pm() : existing.get(j).owner();
                            // PD保留传入值，为空则保留原有值
                            String pd = StringUtils.hasText(inc.pd()) ? inc.pd() : existing.get(j).pd();
                            existing.set(j, new RequirementResponse.SystemItem(
                                    existing.get(j).name(), existing.get(j).team(),
                                    existing.get(j).owner(), existing.get(j).modification(),
                                    pm, pd));
                        }
                    }
                }

                req.setSystemItems(MAPPER.writeValueAsString(existing));

                // 更新全局assignedPm和assignedPd
                List<String> pms = existing.stream()
                        .map(RequirementResponse.SystemItem::pm)
                        .filter(p -> p != null && !p.isEmpty())
                        .distinct().toList();
                List<String> pds = existing.stream()
                        .map(RequirementResponse.SystemItem::pd)
                        .filter(p -> p != null && !p.isEmpty())
                        .distinct().toList();
                req.setAssignedPm(String.join("、", pms));
                if (!pds.isEmpty()) req.setAssignedPd(String.join("、", pds));

            } catch (JsonProcessingException e) {
                log.warn("解析系统列表失败");
            }
        }

        String teamLabel = userTeam != null ? userTeam : "全部";
        String text = StringUtils.hasText(comment) ? comment
                : "已指派" + teamLabel + "的项目经理和产品经理";
        addComment(id, operator.getName(), "团队组长", "指派", text);

        // 检查是否所有系统都已指派PM和PD
        boolean allAssigned = checkAllSystemsAssigned(req);
        if (allAssigned) {
            req.setCurrentNode(NODES[4]); // 产品经理
            req.setNodeIndex(4);
            addComment(id, "系统", "系统", "完成", "全部系统均已指派项目经理和产品经理。");
        }
        // 否则留在节点3，等待其他团队指派

        requirementRepository.save(req);
        return getDetail(id);
    }

    /** 检查所有涉及系统是否都已指派PM和PD */
    private boolean checkAllSystemsAssigned(Requirement req) {
        if (!StringUtils.hasText(req.getSystemItems())) return true;
        try {
            List<RequirementResponse.SystemItem> items = MAPPER.readValue(
                    req.getSystemItems(), new TypeReference<List<RequirementResponse.SystemItem>>() {});
            return items.stream().allMatch(item ->
                    StringUtils.hasText(item.pm()) && StringUtils.hasText(item.pd()));
        } catch (JsonProcessingException e) {
            return true;
        }
    }

    /**
     * 产品经理上传需求说明书，并设置确认人员
     */
    @Transactional
    public RequirementResponse uploadSpec(Long id, User operator, String specDocumentPath,
                                           String specReviewersJson, String comment) {
        Requirement req = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("需求不存在"));
        if (!"产品经理".equals(req.getCurrentNode())) {
            throw new RuntimeException("当前节点不是产品经理");
        }
        req.setSpecDocumentPath(specDocumentPath);
        req.setSpecReviewers(specReviewersJson);
        String text = StringUtils.hasText(comment) ? comment : "需求说明书已上传，待多方确认。";
        addComment(id, operator.getName(), "产品经理", "上传", text);
        // 流转至多方确认
        req.setCurrentNode(NODES[5]);
        req.setNodeIndex(5);
        requirementRepository.save(req);
        return getDetail(id);
    }

    /**
     * 多方确认需求说明书：每个确认人员逐一确认，全部确认后状态变为需求已确认
     */
    @Transactional
    public RequirementResponse confirmSpec(Long id, User operator, String comment) {
        Requirement req = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("需求不存在"));
        if (!"多方确认".equals(req.getCurrentNode())) {
            throw new RuntimeException("当前节点不是多方确认");
        }

        // 检查当前用户是否在确认人员列表中
        String operatorName = operator.getName();
        List<java.util.Map<String, Object>> reviewers = parseSpecReviewers(req.getSpecReviewers());
        boolean found = false;

        for (java.util.Map<String, Object> r : reviewers) {
            if (operatorName.equals(r.get("name"))) {
                r.put("confirmed", true);
                r.put("confirmedAt", java.time.LocalDateTime.now().toString());
                found = true;
                // 不break：同一人有多个角色时，一键全部确认
            }
        }

        if (!found) {
            throw new RuntimeException("您不在确认人员列表中，无法确认");
        }

        // 保存更新后的确认状态
        try {
            req.setSpecReviewers(MAPPER.writeValueAsString(reviewers));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化确认人员失败");
        }

        String role = "";
        for (java.util.Map<String, Object> r : reviewers) {
            if (operatorName.equals(r.get("name"))) {
                role = String.valueOf(r.getOrDefault("role", ""));
                break;
            }
        }
        String text = StringUtils.hasText(comment) ? comment : "确认需求说明书。";
        addComment(id, operator.getName(), role, "确认", text);

        // 检查是否全部确认
        boolean allConfirmed = reviewers.stream().allMatch(r -> Boolean.TRUE.equals(r.get("confirmed")));

        if (allConfirmed) {
            req.setStatus("需求已确认");
            addComment(id, "系统", "系统", "完成", "全部确认人员已确认，需求说明书确认完成。");
            log.info("需求 {} 全部确认完成", req.getRequirementCode());
        } else {
            long confirmedCount = reviewers.stream().filter(r -> Boolean.TRUE.equals(r.get("confirmed"))).count();
            log.info("需求 {} 确认进度: {}/{}", req.getRequirementCode(), confirmedCount, reviewers.size());
        }

        requirementRepository.save(req);
        return getDetail(id);
    }

    @SuppressWarnings("unchecked")
    private List<java.util.Map<String, Object>> parseSpecReviewers(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            return MAPPER.readValue(json, new TypeReference<List<java.util.Map<String, Object>>>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    /**
     * 获取默认确认人员：系统负责人 + 产品经理 + 业务人员（提出人）
     */
    public List<java.util.Map<String, Object>> getDefaultReviewers(Long id) {
        Requirement req = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("需求不存在"));
        List<java.util.Map<String, Object>> reviewers = new ArrayList<>();

        // 从涉及系统中提取系统负责人
        if (StringUtils.hasText(req.getSystemItems())) {
            try {
                List<RequirementResponse.SystemItem> items = MAPPER.readValue(
                        req.getSystemItems(), new TypeReference<List<RequirementResponse.SystemItem>>() {});
                for (RequirementResponse.SystemItem item : items) {
                    java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
                    r.put("name", item.owner());
                    r.put("role", "系统负责人");
                    r.put("system", item.name());
                    r.put("confirmed", false);
                    r.put("confirmedAt", null);
                    reviewers.add(r);
                }
            } catch (JsonProcessingException e) {
                log.warn("解析涉及系统失败");
            }
        }

        // 产品经理
        if (StringUtils.hasText(req.getAssignedPd())) {
            java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
            r.put("name", req.getAssignedPd());
            r.put("role", "产品经理");
            r.put("system", "");
            r.put("confirmed", false);
            r.put("confirmedAt", null);
            reviewers.add(r);
        }

        // 业务人员（提出人）
        java.util.Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("name", req.getSubmitterName());
        r.put("role", "业务人员");
        r.put("system", "");
        r.put("confirmed", false);
        r.put("confirmedAt", null);
        reviewers.add(r);

        return reviewers;
    }

    /**
     * 已驳回需求重新提交
     */
    @Transactional
    public RequirementResponse resubmit(Long id, User operator, RequirementRequest request) {
        Requirement req = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("需求不存在"));
        if (!"已驳回".equals(req.getStatus())) {
            throw new RuntimeException("只有已驳回的需求可以重新提交");
        }
        if (StringUtils.hasText(request.getTitle())) req.setTitle(request.getTitle());
        if (StringUtils.hasText(request.getContent())) req.setContent(request.getContent());
        if (StringUtils.hasText(request.getPriority())) req.setPriority(request.getPriority());
        req.setStatus("审批中");
        req.setCurrentNode(NODES[1]);
        req.setNodeIndex(1);
        addComment(id, operator.getName(), "提出人", "提交", "修改后重新提交。");
        requirementRepository.save(req);
        return getDetail(id);
    }

    // ==================== 确认后流程 ====================

    /**
     * PM启动实施：填写计划测试/投产时间，状态→实施中
     */
    @Transactional
    public RequirementResponse startImplementation(Long id, User operator,
                                                    String plannedTestDate, String plannedProductionDate) {
        Requirement req = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("需求不存在"));
        if (!"需求已确认".equals(req.getStatus())) {
            throw new RuntimeException("只有已确认的需求才能启动实施");
        }
        if (StringUtils.hasText(plannedTestDate))
            req.setPlannedTestDate(LocalDate.parse(plannedTestDate));
        if (StringUtils.hasText(plannedProductionDate))
            req.setPlannedProductionDate(LocalDate.parse(plannedProductionDate));
        req.setStatus("实施中");
        addComment(id, operator.getName(), "项目经理", "启动",
                "启动实施，计划测试：" + plannedTestDate + "，计划投产：" + plannedProductionDate);
        requirementRepository.save(req);
        return getDetail(id);
    }

    /**
     * PM/PD填写正式投产日期，状态→已投产
     */
    @Transactional
    public RequirementResponse setProduction(Long id, User operator, String productionDate) {
        Requirement req = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("需求不存在"));
        if (!"测试通过".equals(req.getStatus())) {
            throw new RuntimeException("只有测试通过的需求才能填写投产日期");
        }
        if (StringUtils.hasText(productionDate))
            req.setProductionDate(LocalDate.parse(productionDate));
        req.setStatus("已投产");
        addComment(id, operator.getName(), req.getCurrentNode(), "投产",
                "正式投产日期：" + productionDate);
        requirementRepository.save(req);
        return getDetail(id);
    }

    /**
     * 业务提出人确认关闭，状态→已关闭
     */
    @Transactional
    public RequirementResponse close(Long id, User operator) {
        Requirement req = requirementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("需求不存在"));
        if (!"已投产".equals(req.getStatus())) {
            throw new RuntimeException("只有已投产的需求才能关闭");
        }
        req.setStatus("已关闭");
        addComment(id, operator.getName(), "业务人员", "确认",
                "确认需求已完成，关闭需求。");
        requirementRepository.save(req);
        return getDetail(id);
    }

    // ==================== 私有方法 ====================

    private String generateCode() {
        String prefix = "REQ-" + Year.now() + "-";
        return requirementRepository.findMaxCodeByPrefix(prefix)
                .map(max -> {
                    String num = max.substring(prefix.length());
                    return prefix + String.format("%03d", Integer.parseInt(num) + 1);
                })
                .orElse(prefix + "001");
    }

    private void addComment(Long requirementId, String author, String role, String action, String content) {
        RequirementComment c = new RequirementComment();
        c.setRequirementId(requirementId);
        c.setAuthor(author);
        c.setRole(role);
        c.setAction(action);
        c.setContent(content);
        commentRepository.save(c);
    }

    private RequirementResponse toResponse(Requirement r) {
        RequirementResponse resp = new RequirementResponse();
        resp.setId(r.getId());
        resp.setRequirementCode(r.getRequirementCode());
        resp.setTitle(r.getTitle());
        resp.setContent(r.getContent());
        resp.setSubmitterId(r.getSubmitterId());
        resp.setSubmitterName(r.getSubmitterName());
        resp.setDept(r.getDept());
        resp.setPriority(r.getPriority());
        resp.setStatus(r.getStatus());
        resp.setCurrentNode(r.getCurrentNode());
        resp.setNodeIndex(r.getNodeIndex());
        resp.setExpectedDate(r.getExpectedDate());
        resp.setAssignedDev(r.getAssignedDev());
        resp.setAssignedPm(r.getAssignedPm());
        resp.setAssignedPd(r.getAssignedPd());
        resp.setPrimarySystemName(r.getPrimarySystemName());
        resp.setSpecDocumentPath(r.getSpecDocumentPath());
        resp.setSpecReviewers(r.getSpecReviewers());
        resp.setAttachmentPath(r.getAttachmentPath());
        resp.setPlannedTestDate(r.getPlannedTestDate());
        resp.setPlannedProductionDate(r.getPlannedProductionDate());
        resp.setProductionDate(r.getProductionDate());
        resp.setCreatedAt(r.getCreatedAt());
        resp.setUpdatedAt(r.getUpdatedAt());

        try {
            if (StringUtils.hasText(r.getSystemItems())) {
                resp.setSystemItems(MAPPER.readValue(r.getSystemItems(),
                        new TypeReference<List<RequirementResponse.SystemItem>>() {}));
            }
        } catch (JsonProcessingException e) { log.warn("解析systemItems失败"); }
        try {
            if (StringUtils.hasText(r.getDeployments())) {
                resp.setDeployments(MAPPER.readValue(r.getDeployments(),
                        new TypeReference<List<RequirementResponse.DeploymentItem>>() {}));
            }
        } catch (JsonProcessingException e) { log.warn("解析deployments失败"); }

        return resp;
    }
}
