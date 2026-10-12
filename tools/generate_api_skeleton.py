#!/usr/bin/env python3
# =====================================================================
# 迭代一 接口骨架生成器（一次性工具，不属于主工程运行时代码）
# =====================================================================
# 文件     : tools/generate_api_skeleton.py
# 版本     : v1.0
# 日期     : 2026-10-12
# 作者     : 后端接口骨架负责人（待填写姓名）
# 状态     : 草稿（待确认）
#
# 用途     : 读取**已冻结**的接口契约 docs/iteration1/design/openapi.yaml，
#            生成 83 个 operationId 对应的 Controller / DTO / Service 骨架。
#
# 为什么用生成器而不是手写：
#   契约已冻结（83 operationId / 99 schemas）。手写必然出现拼写、路径、
#   类型或 operationId 的偏差，而契约要求 1:1。生成可保证：
#     - operationId 完全一致（验收标准 4）
#     - HTTP 方法 / 路径 / 请求与响应类型完全一致（验收标准 5）
#     - 幂等参数必填性完全一致（验收标准 7）：16 必填 + 16 可选
#     - 不遗漏、不多余
#
# 重要约束：
#   1. 本脚本**只读取** openapi.yaml，绝不修改契约与数据库迁移脚本。
#   2. 生成结果为**纯 Java 11**，不依赖 Lombok（避免注解处理与新增依赖风险）。
#   3. 不生成任何业务逻辑：Service 实现统一抛 UnsupportedOperationException。
#
# 用法     : python3 tools/generate_api_skeleton.py
# =====================================================================

import os
import re
import sys
import shutil
from collections import OrderedDict

try:
    import yaml
except ImportError:
    sys.exit("需要 PyYAML：pip install pyyaml")

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SPEC_PATH = os.path.join(REPO, 'docs/iteration1/design/openapi.yaml')
SRC = os.path.join(REPO, 'src/main/java/com/jc2001/campustrade')
PKG = 'com.jc2001.campustrade'
GEN_MARK = 'GENERATED FROM openapi.yaml'

# 契约 servers[0].url 的基础路径（api-contract.md §5：基础路径 /api/v1）
BASE_PATH = '/api/v1'

# ---------------------------------------------------------------------
# tag -> Controller 类名映射
#   契约 tag 是一个「分组」，可能包含多个 Controller（更符合分层直觉）。
#   映射保持 operationId 不变、tag 不变，仅决定方法落在哪个类里。
# ---------------------------------------------------------------------
TAG_TO_CONTROLLER = {
    '01-认证与账户': 'AuthController',
    '02-个人资料': 'UserController',
    '03-字典与区域': 'DictionaryController',
    '04-商品': 'ItemController',
    '05-购物车': 'CartController',
    '06-订单与付款': 'OrderController',
    '07-评价': 'ReviewController',
    '08-交换包': 'SwapController',
    '09-交换提议': 'SwapController',
    '10-举报': 'ReportController',
    '11-管理员治理': 'AdminController',
    '12-通知': 'NotificationController',
    '13-系统定时任务': 'InternalController',
}

# tag -> Service 接口名 / 实现名
TAG_TO_SERVICE = {
    '01-认证与账户': 'AuthService',
    '02-个人资料': 'UserService',
    '03-字典与区域': 'DictionaryService',
    '04-商品': 'ItemService',
    '05-购物车': 'CartService',
    '06-订单与付款': 'OrderService',
    '07-评价': 'ReviewService',
    '08-交换包': 'SwapService',
    '09-交换提议': 'SwapService',
    '10-举报': 'ReportService',
    '11-管理员治理': 'AdminService',
    '12-通知': 'NotificationService',
    '13-系统定时任务': 'InternalTaskService',
}

# Controller -> 所属 Service（同一 Controller 内可能按领域拆分，此处按 tag 决定）
CONTROLLER_DESC = {
    'AuthController': '认证与账户（FR-101 ~ FR-104）',
    'UserController': '个人资料（FR-105 ~ FR-108）',
    'DictionaryController': '预设字典与区域（R-15 / R-06 / FR-206 / FR-207）',
    'ItemController': '商品（FR-201 ~ FR-211）',
    'CartController': '购物车（FR-301 ~ FR-302）',
    'OrderController': '订单与付款（FR-303 ~ FR-311）',
    'ReviewController': '评价（FR-312）',
    'SwapController': '交换包与交换提议（FR-402 ~ FR-412）',
    'ReportController': '举报（FR-211）',
    'AdminController': '管理员治理（FR-501 ~ FR-505）',
    'NotificationController': '站内通知（FR-507）',
    'InternalController': '系统定时任务（FR-601 ~ FR-604）—— 内部接口，仅系统调用',
}

# 13 个模块中文名（用于 Swagger 分组，与契约 tags 保持一致）
TAG_DESC = {}


def camel_to_pascal(name):
    return name[0].upper() + name[1:] if name else name


def load_spec():
    with open(SPEC_PATH, encoding='utf-8') as f:
        return yaml.safe_load(f)


def resolve_ref(ref):
    """'#/components/schemas/Foo' -> 'Foo'"""
    return ref.rsplit('/', 1)[-1]


# =====================================================================
# 步骤 1：解析契约
# =====================================================================

def collect_schemas(spec):
    """返回 (schemas, roles, enums)
    roles: schema -> 'request' | 'response'
    enums: schema -> list of enum values（纯字符串枚举）
    """
    schemas = spec['components']['schemas']
    roles = {}

    # 先判定哪些 schema 出现在 requestBody 中
    request_schemas = set()

    def walk_for_refs(node):
        """收集 requestBody 内所有 $ref"""
        found = set()
        if isinstance(node, dict):
            for k, v in node.items():
                if k == '$ref' and isinstance(v, str):
                    found.add(resolve_ref(v))
                else:
                    found |= walk_for_refs(v)
        elif isinstance(node, list):
            for it in node:
                found |= walk_for_refs(it)
        return found

    for path, item in spec['paths'].items():
        for method, op in item.items():
            if method not in ('get', 'post', 'put', 'patch', 'delete'):
                continue
            rb = op.get('requestBody')
            if rb:
                request_schemas |= walk_for_refs(rb)

    for name, sch in schemas.items():
        if 'enum' in sch:
            # 枚举一律放 response 包（作为共享值类型，双方都可引用）
            roles[name] = 'response'
        elif name in request_schemas:
            roles[name] = 'request'
        else:
            roles[name] = 'response'

    # 识别纯字符串枚举
    enums = {}
    for name, sch in schemas.items():
        if sch.get('type') == 'string' and 'enum' in sch:
            enums[name] = sch['enum']

    return schemas, roles, enums


# =====================================================================
# 步骤 2：Java 类型映射
# =====================================================================

def java_type_for_schema(sch, schemas, enums, pkg_for_ref, imports):
    """把 OpenAPI schema 片段映射为 Java 类型字符串。"""
    if sch is None:
        imports.add('java.util.Map')
        return 'Map<String, Object>'

    if '$ref' in sch:
        name = resolve_ref(sch['$ref'])
        imports.add(pkg_for_ref(name))
        return name

    if 'allOf' in sch and len(sch['allOf']) == 1:
        inner = sch['allOf'][0]
        if '$ref' in inner:
            name = resolve_ref(inner['$ref'])
            imports.add(pkg_for_ref(name))
            return name

    if 'oneOf' in sch or 'anyOf' in sch:
        # 契约中未使用；保守用 Object
        return 'Object'

    t = sch.get('type')

    if t == 'array':
        item_t = java_type_for_schema(sch.get('items'), schemas, enums, pkg_for_ref, imports)
        imports.add('java.util.List')
        return f'List<{item_t}>'

    if t == 'string':
        fmt = sch.get('format')
        if fmt == 'date-time':
            imports.add('java.time.Instant')
            return 'Instant'
        return 'String'

    if t == 'integer':
        return 'Long' if sch.get('format') == 'int64' else 'Integer'

    if t == 'number':
        if sch.get('format') == 'decimal-string' or isinstance(sch.get('example'), str):
            # 契约的 DecimalString：用 String 承载，避免浮点误差（R-13）
            return 'String'
        imports.add('java.math.BigDecimal')
        return 'BigDecimal'

    if t == 'boolean':
        return 'Boolean'

    if t == 'object':
        if 'properties' in sch and sch['properties']:
            # 匿名内联对象 -> 用 Map 占位（契约中仅出现在无 name 的零散响应）
            imports.add('java.util.Map')
            return 'Map<String, Object>'
        imports.add('java.util.Map')
        return 'Map<String, Object>'

    return 'Object'


# =====================================================================
# 步骤 3：生成 DTO
# =====================================================================

def generate_enum(name, values, description):
    lines = []
    lines.append(f'package {PKG}.dto.response;')
    lines.append('')
    lines.append('import io.swagger.v3.oas.annotations.media.Schema;')
    lines.append('')
    lines.append(f'// {GEN_MARK} —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程')
    lines.append('/**')
    lines.append(f' * {description or name}')
    lines.append(' *')
    lines.append(' * <p>取值严格对齐 {@code V1__init_schema.sql} 的 PostgreSQL ENUM（api-contract.md AD-09）。')
    lines.append(' */')
    lines.append(f'@Schema(description = "{esc(description or name)}")')
    lines.append(f'public enum {name} {{')
    lines.append('')
    for v in values:
        lines.append(f'    /** {v} */')
        lines.append(f'    {v},')
        lines.append('')
    # 去掉最后一个逗号后的空行
    while lines and lines[-1] == '':
        lines.pop()
    if lines[-1].endswith(','):
        lines[-1] = lines[-1][:-1] + ';'
    lines.append('')
    lines.append('}')
    lines.append('')
    return '\n'.join(lines)


def esc(s):
    """转义为可安全嵌入 Java 双引号字符串字面量的文本。

    必须处理：反斜杠（契约里有 `\\d` 等正则示例）、双引号、换行/回车/制表符
    （契约 description 是 Markdown 多行文本，直接内联会截断 Java 字面量）。
    """
    if s is None:
        return ''
    out = str(s)
    out = out.replace('\\', '\\\\')      # 反斜杠必须最先处理
    out = out.replace('"', '\\"')
    out = out.replace('\r\n', ' ')
    out = out.replace('\n', ' ')
    out = out.replace('\r', ' ')
    out = out.replace('\t', ' ')
    return out.strip()



def generate_dto_class(name, sch, role, schemas, enums, pkg_for_ref):
    """生成一个 DTO 类（显式 getter/setter，无 Lombok）。"""
    imports = set()
    desc = sch.get('description', '')
    required = set(sch.get('required') or [])
    props = sch.get('properties') or {}

    fields = []          # (javaType, fieldName, propSchema, isRequired, propDesc)
    for pname, psch in props.items():
        jt = java_type_for_schema(psch, schemas, enums, pkg_for_ref, imports)
        fields.append((jt, pname, psch or {}, pname in required, (psch or {}).get('description', '')))

    pkg = f'{PKG}.dto.{role}'

    L = []
    L.append(f'package {pkg};')
    L.append('')
    for imp in sorted(imports):
        L.append(f'import {imp};')
    L.append('import io.swagger.v3.oas.annotations.media.Schema;')
    # 校验注解
    val_imports = set()
    if any(f[3] for f in fields):
        val_imports.add('javax.validation.constraints.NotNull')
    val_imports.add('javax.validation.Valid')
    for _, _, psch, _, _ in fields:
        if 'maxLength' in psch or 'minLength' in psch:
            val_imports.add('javax.validation.constraints.Size')
        if 'minimum' in psch or 'maximum' in psch:
            if 'minimum' in psch:
                val_imports.add('javax.validation.constraints.Min')
            if 'maximum' in psch:
                val_imports.add('javax.validation.constraints.Max')
        if 'minItems' in psch or 'maxItems' in psch:
            val_imports.add('javax.validation.constraints.Size')
    for imp in sorted(val_imports):
        L.append(f'import {imp};')
    L.append('')
    L.append(f'// {GEN_MARK} —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程')
    L.append('/**')
    L.append(f' * {desc or name}')
    L.append(' *')
    L.append(' * <p>对应契约 {@code components.schemas.' + name + '}。')
    L.append(' * <p>不包含任何敏感字段（无 password_hash / deleted_at / created_by / updated_by）。')
    if role == 'request':
        L.append(' * <p>校验注解由契约的 required / maxLength / minLength / minimum / maximum 生成。')
    L.append(' */')
    L.append(f'@Schema(name = "{name}", description = "{esc(desc or name)}")')
    L.append(f'public class {name} {{')
    L.append('')

    # ---- 字段 ----
    for jt, fname, psch, isreq, pdesc in fields:
        if pdesc:
            L.append('    /**')
            for line in str(pdesc).split('\n'):
                L.append(f'     * {line}')
            L.append('     */')
        if isreq:
            L.append('    @NotNull')
        size_bits = []
        if 'minLength' in psch:
            size_bits.append(f'min = {psch["minLength"]}')
        if 'maxLength' in psch:
            size_bits.append(f'max = {psch["maxLength"]}')
        if 'minItems' in psch:
            size_bits.append(f'min = {psch["minItems"]}')
        if 'maxItems' in psch:
            size_bits.append(f'max = {psch["maxItems"]}')
        if size_bits:
            L.append(f'    @Size({", ".join(size_bits)})')
        if 'minimum' in psch:
            L.append(f'    @Min({psch["minimum"]})')
        if 'maximum' in psch:
            L.append(f'    @Max({psch["maximum"]})')
        ex = psch.get('example', None)
        schema_bits = [f'description = "{esc(pdesc or fname)}"']
        if ex is not None and not isinstance(ex, (dict, list)):
            if jt == 'String':
                schema_bits.append(f'example = "{esc(ex)}"')
        L.append(f'    @Schema({", ".join(schema_bits)})')
        L.append(f'    private {jt} {fname};')
        L.append('')

    # ---- 显式访问器（不用 Lombok，避免注解处理依赖）----
    L.append('    // -------------------------------------------------------------')
    L.append('    // 显式访问器（本工程不使用 Lombok，避免注解处理与新增依赖）')
    L.append('    // -------------------------------------------------------------')
    L.append('')
    L.append(f'    public {name}() {{')
    L.append('    }')
    L.append('')
    for jt, fname, _, _, _ in fields:
        cap = camel_to_pascal(fname)
        L.append(f'    public {jt} get{cap}() {{')
        L.append(f'        return {fname};')
        L.append('    }')
        L.append('')
        L.append(f'    public void set{cap}({jt} {fname}) {{')
        L.append(f'        this.{fname} = {fname};')
        L.append('    }')
        L.append('')
    # 便利 toString（便于联调排查）
    L.append('    @Override')
    L.append('    public String toString() {')
    L.append('        return "' + name + '{"')
    for jt, fname, _, _, _ in fields:
        L.append(f'                + "{fname}=" + {fname} + ", "')
    L.append("                + '}';")
    L.append('    }')
    L.append('}')
    L.append('')
    return '\n'.join(L)


# =====================================================================
# 步骤 4：解析 operation，生成 Controller / Service
# =====================================================================

HTTP_ANN = {
    'get': 'GetMapping',
    'post': 'PostMapping',
    'put': 'PutMapping',
    'patch': 'PatchMapping',
    'delete': 'DeleteMapping',
}


def safe_ident(name):
    """把参数名转成合法 Java 标识符。

    - `itemId`          -> `itemId`         （保留原有小驼峰，不得小写化）
    - `Idempotency-Key` -> `idempotencyKey` （分隔符转驼峰）
    - `X-Trace-Id`      -> `xTraceId`
    原 name 仍用于 @RequestHeader/@RequestParam 的 value。
    """
    parts = [x for x in re.split(r'[-_.\s]+', name) if x]
    if not parts:
        return 'param'
    first = parts[0]
    if first.isupper():
        ident = first.lower()
    elif first[:1].isupper():
        ident = first[:1].lower() + first[1:]
    else:
        ident = first
    for seg in parts[1:]:
        ident += seg[:1].upper() + seg[1:]
    ident = re.sub(r'[^A-Za-z0-9_$]', '', ident)
    if not ident or ident[0].isdigit():
        ident = 'p' + ident
    return ident


class Param:
    def __init__(self, kind, name, java_type, required, desc, default=None, is_idem=False):
        self.kind = kind          # path | query | header | body
        self.name = name
        self.ident = safe_ident(name)   # Java 参数名（name 可能含 '-'）
        self.java_type = java_type
        self.required = required
        self.desc = desc
        self.default = default
        self.is_idem = is_idem


def response_data_type(operation, schemas, enums, pkg_for_ref, imports):
    """由 200/201 响应推导 ApiResponse 的泛型参数类型。"""
    responses = operation.get('responses') or {}
    # 优先级：200 / 201 / 202 -> 204（无内容）
    picked = None
    for code in ('200', '201', '202'):
        if code in responses:
            picked = responses[code]
            break
    if picked is None:
        return 'Void'

    content = (picked or {}).get('content') or {}
    media = content.get('application/json')
    if not media:
        return 'Void'
    sch = media.get('schema') or {}

    # 解包 allOf: [ApiResponse, {properties: {data: ...}}]
    data_schema = None
    if 'allOf' in sch:
        for part in sch['allOf']:
            props = part.get('properties') or {}
            if 'data' in props:
                data_schema = props['data']
                break
        if data_schema is None:
            # 例如 allOf 仅引用 ErrorResponse 等
            data_schema = sch
    elif (sch.get('properties') or {}).get('data') is not None:
        data_schema = sch['properties']['data']
    else:
        data_schema = sch

    # 形态 A：data 为 allOf: [ {$ref: PageResult}, {properties: {content: {items: {$ref: X}}}} ]
    # （契约表达 PageResult<T> 的方式；OpenAPI 3.0 无泛型）
    if isinstance(data_schema, dict) and 'allOf' in data_schema:
        refs = [resolve_ref(x['$ref']) for x in data_schema['allOf']
                if isinstance(x, dict) and '$ref' in x]
        if 'PageResult' in refs:
            inner = find_page_element(data_schema)
            if inner:
                imports.add(pkg_for_ref(inner))
                imports.add('com.jc2001.campustrade.common.PageResult')
                return f'PageResult<{inner}>'
            return 'PageResult<Object>'

    # 处理 $ref 到 ApiResponse / PageResult
    if '$ref' in data_schema:
        ref = resolve_ref(data_schema['$ref'])
        if ref == 'ApiResponse':
            return 'Void'
        if ref == 'PageResult':
            # OpenAPI 3.0 无法表达 PageResult<T> 泛型：契约把 content/items 放在 allOf 内。
            # 这里从 allOf 分支的 content.items.$ref 反推真实元素类型，
            # 以生成 ApiResponse<PageResult<XxxSummary>> 而不是退化为 Object。
            inner = find_page_element(sch)
            if inner:
                imports.add(pkg_for_ref(inner))
                imports.add('com.jc2001.campustrade.common.PageResult')
                return f'PageResult<{inner}>'
            return 'PageResult<Object>'
        imports.add(pkg_for_ref(ref))
        return ref

    return java_type_for_schema(data_schema, schemas, enums, pkg_for_ref, imports)


def find_page_element(node):
    """在响应 schema 中查找 PageResult 的元素类型名（content.items.$ref 或 allOf 内）。"""
    found = []

    def walk(o):
        if isinstance(o, dict):
            if o.get('type') == 'array' and isinstance(o.get('items'), dict):
                it = o['items']
                if '$ref' in it:
                    found.append(resolve_ref(it['$ref']))
                elif 'allOf' in it:
                    for part in it['allOf']:
                        if isinstance(part, dict) and '$ref' in part:
                            found.append(resolve_ref(part['$ref']))
            for v in o.values():
                walk(v)
        elif isinstance(o, list):
            for v in o:
                walk(v)

    walk(node)
    # 取最后一个（最内层的 content.items），排除 PageResult 自身引用
    found = [f for f in found if f != 'PageResult']
    return found[-1] if found else None


def build_operations(spec, schemas, enums, pkg_for_ref):
    # 共享参数定义（components.parameters）：契约里 Idempotency-Key / 分页参数都是 $ref 引用，
    # 必须解析出来，否则幂等参数会全部丢失（实测会得到 0 必填 / 0 可选）。
    shared_params = (spec.get('components') or {}).get('parameters') or {}
    ops = []
    for path, item in spec['paths'].items():
        for method, op in item.items():
            if method not in HTTP_ANN:
                continue
            imports = set()
            oid = op['operationId']
            tags = op.get('tags') or ['未分组']
            tag = tags[0]

            params = []
            # 路径参数：从 path 模板提取，取 operation 级声明
            declared = {}
            for raw in (op.get('parameters') or []):
                if '$ref' in raw:
                    ref_name = resolve_ref(raw['$ref'])
                    target = shared_params.get(ref_name)
                    if target is None:
                        sys.exit(f'无法解析共享参数 $ref: {raw["$ref"]} (operationId={op.get("operationId")})')
                    declared[target['name']] = target
                elif 'name' in raw:
                    declared[raw['name']] = raw
            path_vars = re.findall(r'\{([^}]+)\}', path)
            for pv in path_vars:
                decl = declared.get(pv) or {}
                sch = decl.get('schema') or {'type': 'string'}
                jt = java_type_for_schema(sch, schemas, enums, pkg_for_ref, imports)
                params.append(Param('path', pv, jt, True, decl.get('description', '')))

            # query / header 参数
            for pname, decl in declared.items():
                loc = decl.get('in')
                if loc == 'path':
                    continue
                sch = decl.get('schema') or {}
                jt = java_type_for_schema(sch, schemas, enums, pkg_for_ref, imports)
                req = bool(decl.get('required'))
                is_idem = (pname == 'Idempotency-Key')
                if loc == 'query':
                    params.append(Param('query', pname, jt, req, decl.get('description', ''),
                                        default=sch.get('default')))
                elif loc == 'header':
                    params.append(Param('header', pname, jt, req, decl.get('description', ''),
                                        is_idem=is_idem))

            # requestBody
            rb = op.get('requestBody')
            if rb:
                content = (rb.get('content') or {})
                media = content.get('application/json')
                if media and media.get('schema'):
                    rb_sch = media['schema']
                    if '$ref' in rb_sch:
                        ref = resolve_ref(rb_sch['$ref'])
                        imports.add(pkg_for_ref(ref))
                        jt = ref
                    elif rb_sch.get('type') == 'string' and rb_sch.get('format') == 'binary':
                        imports.add('org.springframework.web.multipart.MultipartFile')
                        jt = 'MultipartFile'
                    else:
                        jt = java_type_for_schema(rb_sch, schemas, enums, pkg_for_ref, imports)
                    params.append(Param('body', 'body', jt, bool(rb.get('required')), ''))

            resp_type = response_data_type(op, schemas, enums, pkg_for_ref, imports)

            ops.append({
                'operationId': oid,
                'method': method,
                'path': path,
                'tag': tag,
                'summary': op.get('summary', oid),
                'description': (op.get('description') or ''),
                'params': params,
                'resp_type': resp_type,
                'roles': op.get('x-required-roles') or [],
                'idempotency': op.get('x-idempotency'),
                'state_transition': op.get('x-state-transition'),
                'locking': op.get('x-locking'),
                'internal': bool(op.get('x-internal')),
                'forbidden_restricted': op.get('x-forbidden-for-restricted'),
                'security_empty': (op.get('security') == []),
                'imports': imports,
            })
    return ops


def preauthorize_expr(op):
    """由 x-required-roles 推导 @PreAuthorize 表达式。"""
    roles = set(op['roles'])
    if op['internal'] or roles == {'SYSTEM'}:
        return 'hasRole(\'SYSTEM\')'
    if roles == {'ADMIN'}:
        return 'hasRole(\'ADMIN\')'
    if op['security_empty'] or roles == {'GUEST'}:
        return 'permitAll()'
    return 'isAuthenticated()'


def generate_controller(controller_name, ops):
    tags = OrderedDict((o['tag'], True) for o in ops)
    primary_tag = next(iter(tags))
    pkg = f'{PKG}.controller'

    imports = set()
    for o in ops:
        imports |= o['imports']

    L = []
    L.append(f'package {pkg};')
    L.append('')
    L.append('import com.jc2001.campustrade.common.ApiResponse;')
    L.append('import com.jc2001.campustrade.common.PageResult;')
    L.append('import io.swagger.v3.oas.annotations.Operation;')
    L.append('import io.swagger.v3.oas.annotations.Parameter;')
    L.append('import io.swagger.v3.oas.annotations.security.SecurityRequirement;')
    L.append('import io.swagger.v3.oas.annotations.security.SecurityRequirements;')
    L.append('import io.swagger.v3.oas.annotations.tags.Tag;')
    L.append('import org.springframework.http.ResponseEntity;')
    L.append('import org.springframework.security.access.prepost.PreAuthorize;')
    L.append('import org.springframework.web.bind.annotation.*;')
    if any(p.kind == 'body' and p.java_type == 'MultipartFile' for o in ops for p in o['params']):
        L.append('import org.springframework.web.multipart.MultipartFile;')
    # 导入所有被引用的类型（DTO / MultipartFile / java.* 等），按字典序稳定输出
    for imp in sorted(imports):
        if imp.startswith('java.') or imp.startswith('org.springframework.') \
                or imp.startswith(f'{PKG}.'):
            L.append(f'import {imp};')
    L.append('')
    L.append(f'// {GEN_MARK} —— 请勿手工修改；契约变更须走 api-freeze.md §4 流程')
    L.append('/**')
    L.append(f' * {CONTROLLER_DESC.get(controller_name, controller_name)}')
    L.append(' *')
    L.append(' * <p><b>本类为接口骨架</b>：方法与已冻结契约的 operationId 一一对应，')
    L.append(' * 路径 / HTTP 方法 / 请求与响应类型 / 权限 / 幂等要求均与契约一致；')
    L.append(' * <b>尚未实现业务逻辑</b>（统一抛 UnsupportedOperationException）。')
    L.append(' *')
    L.append(' * <p>权限依据：{@code docs/iteration1/design/api-permission-mapping.md}')
    L.append(' * <p>幂等依据：{@code docs/iteration1/design/idempotency-and-concurrency.md}')
    L.append(' * <p>契约真源：{@code docs/iteration1/design/openapi.yaml}（1.0.0-frozen-candidate）')
    L.append(' */')
    L.append(f'@Tag(name = "{primary_tag}", description = "{esc(TAG_DESC.get(primary_tag, CONTROLLER_DESC.get(controller_name, "")))}")')
    L.append('@RestController')
    # 类级基础路径必须显式声明 /api/v1：
    # 契约的 servers[0].url 已含 /api/v1，但 Spring 不会自动应用 servers 前缀，
    # 因此必须在此声明，否则所有接口会注册到根路径下（实测会 404）。
    L.append(f'@RequestMapping("{BASE_PATH}")')
    L.append(f'public class {controller_name} {{')
    L.append('')

    for o in ops:
        L.extend(generate_controller_method(o))
        L.append('')

    L.append('}')
    L.append('')
    return '\n'.join(L)


def generate_controller_method(o):
    L = []
    summary = esc(o['summary'])
    desc_lines = [l for l in (o['description'] or '').split('\n')]
    L.append('    /**')
    L.append(f'     * {summary}')
    L.append('     *')
    L.append(f'     * <p>operationId: <code>{o["operationId"]}</code>')
    L.append(f'     * <p>权限: {", ".join(o["roles"]) or "—"}')
    if o['state_transition']:
        L.append(f'     * <p>状态转换: {esc(o["state_transition"])}')
    if o['locking']:
        L.append(f'     * <p>并发: {esc(o["locking"])}')
    if o['idempotency']:
        L.append(f'     * <p>幂等: {o["idempotency"]}')
    if o['forbidden_restricted']:
        L.append('     * <p>被处罚用户禁止调用（E-03 → 403 USER_RESTRICTED_CANNOT_WRITE）')
    if o['internal']:
        L.append('     * <p><b>[INTERNAL]</b> 仅系统定时任务调用（X-Internal-Token），网关须禁止外部访问')
    if o['security_empty']:
        L.append('     * <p><b>[PUBLIC]</b> 无需认证')
    L.append('     *')
    L.append('     * <p>TODO 下一步：实现业务逻辑（当前抛 UnsupportedOperationException）')
    L.append('     */')

    # @Operation
    op_ann = [f'summary = "{summary}"', f'operationId = "{o["operationId"]}"']
    if o['description']:
        op_ann.append(f'description = "{esc(o["description"])[:400]}"')
    L.append(f'    @Operation({", ".join(op_ann)})')
    if o['internal']:
        L.append('    @SecurityRequirement(name = "internalToken")')
    elif o['security_empty']:
        # 公开接口：显式声明空安全需求，springdoc 才会输出 security: []（与契约一致）
        L.append('    @SecurityRequirements')
    else:
        L.append('    @SecurityRequirement(name = "bearerAuth")')

    # @PreAuthorize
    L.append(f'    @PreAuthorize("{preauthorize_expr(o)}")')

    # 方法注解 + 路径
    ann = HTTP_ANN[o['method']]
    path = o['path'].replace('/api/v1', '')
    if not path:
        path = '/'
    L.append(f'    @{ann}("{path}")')

    # 方法签名
    ret = f'ResponseEntity<ApiResponse<{o["resp_type"]}>>'
    method_name = o['operationId']
    args = []
    for p in o['params']:
        if p.kind == 'path':
            args.append(f'@Parameter(description = "{esc(p.desc)}") @PathVariable("{p.name}") {p.java_type} {p.ident}')
        elif p.kind == 'query':
            req = 'true' if p.required else 'false'
            dv = f', defaultValue = "{esc(p.default)}"' if p.default is not None else ''
            args.append(f'@Parameter(description = "{esc(p.desc)}") @RequestParam(value = "{p.name}", required = {req}{dv}) {p.java_type} {p.ident}')
        elif p.kind == 'header':
            req = 'true' if p.required else 'false'
            note = ''
            args.append(f'@Parameter(description = "{esc(p.desc)}") @RequestHeader(value = "{p.name}", required = {req}) {p.java_type} {p.ident}{note}')
        elif p.kind == 'body':
            args.append(f'@RequestBody {p.java_type} body')
    has_idem = any(p.is_idem for p in o['params'])
    sig = f'    public {ret} {method_name}('
    if has_idem:
        L.append('    // 幂等键 Idempotency-Key 见方法参数（R-17）')
    if args:
        L.append(sig)
        for i, a in enumerate(args):
            comma = ',' if i < len(args) - 1 else ''
            L.append(f'            {a}{comma}')
        L.append('    ) {')
    else:
        L.append(sig + ') {')
    L.append(f'        throw new UnsupportedOperationException("TODO: implement {o["operationId"]}");')
    L.append('    }')
    return L


def generate_service_interface(service_name, ops):
    pkg = f'{PKG}.service'
    imports = set()
    for o in ops:
        imports |= o['imports']

    L = []
    L.append(f'package {pkg};')
    L.append('')
    for imp in sorted(imports):
        L.append(f'import {imp};')
    L.append('')
    L.append(f'// {GEN_MARK} —— 请勿手工修改')
    L.append('/**')
    L.append(f' * {service_name}（Service 接口骨架）。')
    L.append(' *')
    L.append(' * <p><b>本步骤不实现业务逻辑</b>：实现类统一抛 UnsupportedOperationException。')
    L.append(' *')
    L.append(' * <p><b>返回原始领域类型</b>（如 OrderDetail / PageResult&lt;ItemSummary&gt;），')
    L.append(' * 由 Controller 负责包成 ApiResponse —— 包络属接口层职责，不下沉到 Service。')
    L.append(' *')
    L.append(' * <p><b>下一步实现必须遵守</b>（见 idempotency-and-concurrency.md）：')
    L.append(' * <ul>')
    L.append(' *   <li>状态转换使用条件更新或乐观锁，<b>禁止「先查后改」</b>（R-18）</li>')
    L.append(' *   <li>占用/释放必须与 items.status 投影<b>同一事务</b>（R-16 / DD-01）</li>')
    L.append(' *   <li>部分唯一索引冲突（SQLState 23505）映射为对应域 409，而非 500</li>')
    L.append(' *   <li>归属校验失败返回 403，不返回 404（E-02）</li>')
    L.append(' * </ul>')
    L.append(' */')
    L.append(f'public interface {service_name} {{')
    L.append('')
    for o in ops:
        L.append('    /**')
        L.append(f'     * {esc(o["summary"])}')
        L.append(f'     *')
        L.append(f'     * <p>对应 operationId: <code>{o["operationId"]}</code>')
        L.append('     * <p>TODO 下一步：实现')
        L.append('     */')
        args = []
        for p in o['params']:
            args.append(f'{p.java_type} {p.ident}')
        sig = f'    {o["resp_type"]} {o["operationId"]}(' + ', '.join(args) + ');'
        if len(sig) > 118:
            head = f'    {o["resp_type"]} {o["operationId"]}('
            L.append(head)
            for i, a in enumerate(args):
                comma = ',' if i < len(args) - 1 else ''
                L.append(f'            {a}{comma}')
            L.append('    );')
        else:
            L.append(sig)
        L.append('')
    L.append('}')
    L.append('')
    return '\n'.join(L)


def generate_service_impl(service_name, ops):
    pkg = f'{PKG}.service.impl'
    imports = set()
    for o in ops:
        imports |= o['imports']

    L = []
    L.append(f'package {pkg};')
    L.append('')
    L.append(f'import {PKG}.service.{service_name};')
    L.append('import org.springframework.stereotype.Service;')
    for imp in sorted(imports):
        L.append(f'import {imp};')
    L.append('')
    L.append(f'// {GEN_MARK} —— 请勿手工修改')
    L.append('/**')
    L.append(f' * {service_name} 空实现。')
    L.append(' *')
    L.append(' * <p><b>所有方法均抛 UnsupportedOperationException</b>，用于标识「尚未实现」，')
    L.append(' * 避免骨架被误认为可用。')
    L.append(' *')
    L.append(' * <p>不注入 Repository、不访问数据库、不含事务逻辑（本步骤范围限定）。')
    L.append(' */')
    L.append('@Service')
    L.append(f'public class {service_name}Impl implements {service_name} {{')
    L.append('')
    for o in ops:
        L.append('    /**')
        L.append(f'     * {esc(o["summary"])}（未实现）')
        L.append('     *')
        L.append(f'     * <p>operationId: <code>{o["operationId"]}</code>')
        L.append('     */')
        L.append('    @Override')
        args = []
        for p in o['params']:
            args.append(f'{p.java_type} {p.ident}')
        L.append(f'    public {o["resp_type"]} {o["operationId"]}(' + ', '.join(args) + ') {')
        L.append(f'        throw new UnsupportedOperationException(')
        L.append(f'                "TODO: implement {o["operationId"]}");')
        L.append('    }')
        L.append('')
    L.append('}')
    L.append('')
    return '\n'.join(L)


# =====================================================================
# 主流程
# =====================================================================

def write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)


def main():
    spec = load_spec()
    schemas, roles, enums = collect_schemas(spec)

    def pkg_for_ref(name):
        role = roles.get(name, 'response')
        return f'{PKG}.dto.{role}.{name}'

    # ---- 清理上一次生成物 ----
    for sub in ('dto/request', 'dto/response'):
        d = os.path.join(SRC, sub)
        if os.path.isdir(d):
            shutil.rmtree(d)
    # 保留清单：这些文件**手工维护**，不属于契约生成物，生成器绝不删除/覆盖。
    # （曾因无条件删除导致 HealthController / FrozenSpecController 丢失）
    KEEP = {
        'controller/HealthController.java',
        'controller/FrozenSpecController.java',
    }
    for d in ('controller', 'service', 'service/impl'):
        p = os.path.join(SRC, d)
        if os.path.isdir(p):
            for fn in os.listdir(p):
                rel = f'{d}/{fn}'
                if fn.endswith('.java') and fn != 'package-info.java' and rel not in KEEP:
                    os.remove(os.path.join(p, fn))

    # ---- 生成 DTO ----
    n_dto = 0
    n_enum = 0
    for name, sch in schemas.items():
        role = roles[name]
        if name in enums:
            write(os.path.join(SRC, f'dto/response/{name}.java'),
                  generate_enum(name, enums[name], sch.get('description', '')))
            n_enum += 1
            continue
        write(os.path.join(SRC, f'dto/{role}/{name}.java'),
              generate_dto_class(name, sch, role, schemas, enums, pkg_for_ref))
        n_dto += 1

    # ---- 解析 operations ----
    ops = build_operations(spec, schemas, enums, pkg_for_ref)

    # ---- 按 Controller 归组 ----
    by_ctrl = OrderedDict()
    for o in ops:
        ctrl = TAG_TO_CONTROLLER.get(o['tag'])
        if ctrl is None:
            sys.exit(f'未映射的 tag: {o["tag"]}')
        by_ctrl.setdefault(ctrl, []).append(o)

    for ctrl, cops in by_ctrl.items():
        write(os.path.join(SRC, f'controller/{ctrl}.java'), generate_controller(ctrl, cops))

    # ---- 按 Service 归组 ----
    by_svc = OrderedDict()
    for o in ops:
        svc = TAG_TO_SERVICE.get(o['tag'])
        if svc is None:
            sys.exit(f'未映射的 service tag: {o["tag"]}')
        by_svc.setdefault(svc, []).append(o)

    for svc, sops in by_svc.items():
        write(os.path.join(SRC, f'service/{svc}.java'), generate_service_interface(svc, sops))
        write(os.path.join(SRC, f'service/impl/{svc}Impl.java'), generate_service_impl(svc, sops))

    # ---- 打印统计 ----
    print('=' * 62)
    print('生成完成')
    print('=' * 62)
    print(f'  schemas           : {len(schemas)}')
    print(f'  DTO 类            : {n_dto}')
    print(f'  枚举类            : {n_enum}')
    print(f'  operations        : {len(ops)}')
    print(f'  controllers       : {len(by_ctrl)}')
    print(f'  services          : {len(by_svc)}')
    print()
    print('  Controller            operations')
    for ctrl, cops in by_ctrl.items():
        print(f'    {ctrl:24s} {len(cops)}')
    print()
    print('  Service               operations')
    for svc, sops in by_svc.items():
        print(f'    {svc:24s} {len(sops)}')
    print()
    idem_req = [o['operationId'] for o in ops
                if any(p.is_idem and p.required for p in o['params'])]
    idem_opt = [o['operationId'] for o in ops
                if any(p.is_idem and not p.required for p in o['params'])]
    print(f'  幂等必填 (Idempotency-Key required) : {len(idem_req)}')
    print(f'  幂等可选                            : {len(idem_opt)}')
    print()
    # 写一份对账清单供校验脚本使用
    manifest = os.path.join(REPO, 'target/api-skeleton-manifest.txt')
    os.makedirs(os.path.dirname(manifest), exist_ok=True)
    with open(manifest, 'w', encoding='utf-8') as f:
        for o in ops:
            f.write(f'{o["operationId"]}\t{o["method"].upper()}\t{o["path"]}\t'
                    f'{TAG_TO_CONTROLLER[o["tag"]]}\n')
    print(f'  对账清单已写入: {os.path.relpath(manifest, REPO)}')


if __name__ == '__main__':
    main()
