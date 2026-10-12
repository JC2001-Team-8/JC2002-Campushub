package com.jc2001.campustrade.controller;

import io.swagger.v3.oas.annotations.Hidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 已冻结接口契约的静态服务端点（**手工维护，非契约生成**）。
 *
 * <p><b>注意</b>：本类不属于已冻结契约的 83 个 operationId。生成器**不得删除本文件**
 * （已加入生成器保留清单）。
 *
 * <p>把 {@code docs/iteration1/design/openapi.yaml}（**唯一真源**）通过
 * {@code pom.xml} 的 resource 配置复制为 classpath 资源 {@code openapi/openapi.yaml}，
 * 并由本端点暴露在 {@code /openapi/openapi.yaml}。
 *
 * <p><b>用途</b>：Swagger UI 的「迭代一契约（冻结候选）」入口指向本地址，用于与运行时文档对照；
 * 前端也可从同一地址拉取契约生成客户端代码。
 *
 * <p><b>重要</b>：本端点**只读原样返回**契约文件，不做任何改写，
 * 以保证「契约唯一真源」不被运行时行为污染。
 */
@Hidden  // 不属于已冻结契约的 83 个 operationId，从 OpenAPI 文档中隐藏
@RestController
@RequestMapping("/openapi")
public class FrozenSpecController {

    private static final Logger log = LoggerFactory.getLogger(FrozenSpecController.class);

    /** classpath 中的契约路径（由 pom.xml 从 docs/iteration1/design/ 复制而来）。 */
    private static final String SPEC_CLASSPATH = "openapi/openapi.yaml";

    @GetMapping(value = "/openapi.yaml", produces = "application/yaml;charset=UTF-8")
    public ResponseEntity<Resource> frozenSpec(HttpServletRequest request) {
        ClassPathResource resource = new ClassPathResource(SPEC_CLASSPATH);
        if (!resource.exists()) {
            log.error("未找到冻结契约资源: classpath:{}（请检查 pom.xml 的 resources 配置）", SPEC_CLASSPATH);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/yaml;charset=UTF-8"))
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .body(resource);
    }
}
