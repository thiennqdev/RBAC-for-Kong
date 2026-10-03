# Database Migrations với Liquibase

## Cấu trúc thư mục mới (Multi-tenant)

```
db/changelog/
├── db.changelog-global.yaml    # Global tables (public schema)
├── db.changelog-tenant.yaml    # Tenant-specific tables
├── TEMPLATE.yaml               # Template với các ví dụ
├── README.md                   # Documentation này
└── changes/                    # Các migration files
    ├── 001-create-projects-table.yaml  # Global table
    └── tenant/                          # Tenant-specific changes (future)
        └── 001-create-notifications-table.yaml
```

## Phân loại Changelog

### 1. Global Changelog (`db.changelog-global.yaml`)
- **Mục đích**: Tables thuộc **public schema** (toàn hệ thống)
- **Ví dụ**: `projects`, system configuration, global lookup tables
- **Chạy khi**: Application startup (Spring Boot Liquibase integration)

### 2. Tenant Changelog (`db.changelog-tenant.yaml`)
- **Mục đích**: Tables thuộc **từng tenant schema riêng biệt**
- **Ví dụ**: `notifications`, `templates`, `user_preferences`
- **Chạy khi**: Tạo tenant mới (qua `LiquibaseMigrationRunner`)

## Quy tắc đặt tên

### File naming convention:
```
{sequence}-{description}.yaml

Ví dụ:
- 001-create-provider-tables.yaml
- 002-add-provider-indexes.yaml
- 003-create-credential-tables.yaml
- 004-add-foreign-keys.yaml
```

### ChangeSet ID convention:
```
{sequence}-{description}

Ví dụ:
- 001-create-provider-instance-table
- 002-add-index-provider-name
- 003-insert-seed-data
```

## Cách tạo migration mới

### 1. Xác định loại table
- **Global table** (public schema): projects, system config → dùng `db.changelog-global.yaml`
- **Tenant table** (tenant schema): notifications, templates → dùng `db.changelog-tenant.yaml`

### 2. Tạo changeset file
```bash
# Cho global table
cp TEMPLATE.yaml changes/00X-create-global-table.yaml

# Cho tenant table (tạo folder nếu chưa có)
mkdir -p changes/tenant
cp TEMPLATE.yaml changes/tenant/00X-create-tenant-table.yaml
```

### 3. Cập nhật changelog tương ứng

**Cho global table** - cập nhật `db.changelog-global.yaml`:
```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/001-create-projects-table.yaml
  - include:
      file: db/changelog/changes/00X-create-global-table.yaml  # ← Thêm dòng này
```

**Cho tenant table** - cập nhật `db.changelog-tenant.yaml`:
```yaml
databaseChangeLog:
  - include:
      file: db/changelog/changes/tenant/00X-create-tenant-table.yaml  # ← Thêm dòng này
```

## Best Practices

### 1. Một changeset = Một thay đổi logic
```yaml
# ✅ GOOD - Mỗi changeset làm một việc
- changeSet:
    id: 001-create-provider-table
    changes:
      - createTable: ...

- changeSet:
    id: 002-add-provider-index
    changes:
      - createIndex: ...

# ❌ BAD - Quá nhiều thay đổi trong một changeset
- changeSet:
    id: 001-setup-everything
    changes:
      - createTable: ...
      - createIndex: ...
      - addForeignKey: ...
      - insert: ...
```

### 2. Luôn thêm comment
```yaml
- changeSet:
    id: 001-create-provider-table
    author: developer-name
    comment: Create provider_instance table with audit fields  # ← Mô tả rõ ràng
    changes: ...
```

### 3. Sử dụng rollback khi có thể
```yaml
- changeSet:
    id: 001-create-provider-table
    changes:
      - createTable:
          tableName: provider_instance
    rollback:
      - dropTable:
          tableName: provider_instance
```

### 4. Sử dụng preconditions để tránh lỗi
```yaml
- changeSet:
    id: 001-create-provider-table
    preConditions:
      - onFail: MARK_RAN
      - not:
          - tableExists:
              tableName: provider_instance
    changes:
      - createTable: ...
```

### 5. Audit fields cho mọi table
```yaml
columns:
  # ... business columns ...
  - column:
      name: created_at
      type: TIMESTAMP
      defaultValueComputed: CURRENT_TIMESTAMP
      constraints:
        nullable: false
  - column:
      name: updated_at
      type: TIMESTAMP
      defaultValueComputed: CURRENT_TIMESTAMP
      constraints:
        nullable: false
  - column:
      name: created_by
      type: VARCHAR(255)
  - column:
      name: updated_by
      type: VARCHAR(255)
```

### 6. Indexes cho performance
```yaml
# Thêm index cho foreign keys
- createIndex:
    indexName: idx_provider_credential_provider_id
    tableName: provider_credential
    columns:
      - column:
          name: provider_instance_id

# Thêm index cho các cột thường query
- createIndex:
    indexName: idx_provider_instance_name
    tableName: provider_instance
    columns:
      - column:
          name: name
```

## Các lệnh Liquibase hữu ích

### Xem status
```bash
mvn liquibase:status
```

### Update database
```bash
mvn liquibase:update
```

### Rollback
```bash
# Rollback 1 changeset
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# Rollback đến một tag
mvn liquibase:rollback -Dliquibase.rollbackTag=version-1.0
```

### Generate changelog từ database hiện tại
```bash
mvn liquibase:generateChangeLog
```

### Validate changelog
```bash
mvn liquibase:validate
```

## Common Patterns

### 1. Create table với relationships
```yaml
- changeSet:
    id: 001-create-parent-child-tables
    changes:
      # Parent table
      - createTable:
          tableName: parent_table
          columns:
            - column:
                name: id
                type: BIGINT
                autoIncrement: true
                constraints:
                  primaryKey: true
            - column:
                name: name
                type: VARCHAR(255)
      
      # Child table
      - createTable:
          tableName: child_table
          columns:
            - column:
                name: id
                type: BIGINT
                autoIncrement: true
                constraints:
                  primaryKey: true
            - column:
                name: parent_id
                type: BIGINT
                constraints:
                  nullable: false
      
      # Foreign key
      - addForeignKeyConstraint:
          baseTableName: child_table
          baseColumnNames: parent_id
          constraintName: fk_child_parent
          referencedTableName: parent_table
          referencedColumnNames: id
          onDelete: CASCADE
```

### 2. Add column với default value
```yaml
- changeSet:
    id: 002-add-status-column
    changes:
      - addColumn:
          tableName: provider_instance
          columns:
            - column:
                name: status
                type: VARCHAR(50)
                defaultValue: "ACTIVE"
                constraints:
                  nullable: false
```

### 3. Migrate data
```yaml
- changeSet:
    id: 003-migrate-old-to-new-format
    changes:
      - sql:
          sql: |
            UPDATE provider_instance 
            SET new_field = CONCAT(old_field_1, '-', old_field_2)
            WHERE new_field IS NULL;
```

### 4. Create enum type (PostgreSQL)
```yaml
- changeSet:
    id: 004-create-channel-enum
    changes:
      - sql:
          sql: |
            CREATE TYPE channel_type AS ENUM (
              'EMAIL', 'SMS', 'PUSH', 'CHAT', 'WEBHOOK'
            );
      - modifyDataType:
          tableName: provider_instance
          columnName: channel
          newDataType: channel_type
    rollback:
      - sql:
          sql: |
            ALTER TABLE provider_instance 
            ALTER COLUMN channel TYPE VARCHAR(50);
            DROP TYPE channel_type;
```

## Troubleshooting

### Lỗi: Changeset already executed
- Kiểm tra bảng `databasechangelog` trong database
- Nếu cần chạy lại, xóa record tương ứng hoặc dùng `runAlways: true`

### Lỗi: Lock not released
```bash
mvn liquibase:releaseLocks
```

### Lỗi: Validation failed
- Kiểm tra YAML syntax
- Đảm bảo indentation đúng (dùng spaces, không dùng tabs)
- Validate với online YAML validator

## References

- [Liquibase Documentation](https://docs.liquibase.com/)
- [Liquibase YAML Format](https://docs.liquibase.com/concepts/changelogs/yaml-format.html)
- [Liquibase Best Practices](https://docs.liquibase.com/concepts/bestpractices.html)
