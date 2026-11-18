package com.rentalert.kafka.migration;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.Builder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.transaction.support.TransactionTemplate;

public final class MigrationTable {

    private static final Pattern IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_.]*$");
    private static final String ARRAY_PLACEHOLDER = "CAST(? AS VARCHAR[])";

    private final String name;
    private final String sourceTable;
    private final List<String> columns;
    private final List<String> keyColumns;
    private final String sinceColumn;
    private final Map<String, String> selectExpressions;
    private final Map<String, String> insertPlaceholders;
    private final List<String> conflictTarget;
    private final String sequenceColumn;
    private final Set<String> arrayColumns;
    private final String whereSource;
    private final String fromSource;
    private final String selectSql;
    private final String countSql;
    private final String insertSql;
    private final RowMapper<Object[]> rowMapper;

    @Builder
    private MigrationTable(String name, String sourceTable, List<String> columns, List<String> keyColumns,
        String sinceColumn, Map<String, String> selectExpressions, Map<String, String> insertPlaceholders,
        List<String> conflictTarget, String sequenceColumn, Set<String> arrayColumns, String whereSource,
        String fromSource, String customSelectSql, String customCountSql, String customInsertSql,
        RowMapper<Object[]> customRowMapper) {
        this.name = requireIdentifier(name, "name");
        this.sourceTable = requireIdentifier(sourceTable == null ? name : sourceTable, "sourceTable");
        this.columns = requireColumns(columns, "columns");
        this.keyColumns = keyColumns == null ? List.of("id") : requireColumns(keyColumns, "keyColumns");
        this.sinceColumn = sinceColumn == null ? null : requireIdentifier(sinceColumn, "sinceColumn");
        this.selectExpressions = selectExpressions == null ? Map.of() : Map.copyOf(selectExpressions);
        this.insertPlaceholders = insertPlaceholders == null ? Map.of() : Map.copyOf(insertPlaceholders);
        this.conflictTarget = conflictTarget == null ? this.keyColumns : requireIdentifiers(conflictTarget, "conflictTarget");
        this.sequenceColumn = sequenceColumn != null ? requireIdentifier(sequenceColumn, "sequenceColumn")
            : this.keyColumns.equals(List.of("id")) ? "id" : null;
        this.arrayColumns = arrayColumns == null ? Set.of() : Set.copyOf(arrayColumns);
        this.whereSource = whereSource;
        this.fromSource = fromSource;
        requireSubset(this.keyColumns, "keyColumns");
        requireSubset(this.selectExpressions.keySet(), "selectExpressions");
        requireSubset(this.insertPlaceholders.keySet(), "insertPlaceholders");
        this.rowMapper = customRowMapper != null ? customRowMapper : ColumnRowMapper.of(this.columns, this.arrayColumns);
        this.selectSql = customSelectSql != null ? customSelectSql : buildSelect();
        this.countSql = customCountSql != null ? customCountSql : buildCount();
        this.insertSql = customInsertSql != null ? customInsertSql : buildInsert();
    }

    public String name() {
        return name;
    }

    String selectSql() {
        return selectSql;
    }

    String countSql() {
        return countSql;
    }

    String insertSql() {
        return insertSql;
    }

    public KeysetTableMigrator toMigrator(JdbcTemplate source, JdbcTemplate target, TransactionTemplate targetTransactions) {
        boolean single = keyColumns.size() == 1;
        return KeysetTableMigrator.builder()
            .table(name)
            .source(source)
            .sourceSelectSql(selectSql)
            .sourceCountSql(countSql)
            .sinceColumn(sinceColumn)
            .keyColumn(single ? keyColumns.get(0) : null)
            .keyColumns(single ? null : keyColumns)
            .startAfter(Long.MIN_VALUE)
            .rowMapper(rowMapper)
            .target(target)
            .targetTransactions(targetTransactions)
            .targetInsertSql(insertSql)
            .sequenceTable(sequenceColumn == null ? null : name)
            .sequenceColumn(sequenceColumn)
            .build();
    }

    private String buildSelect() {
        String list = columns.stream().map(column -> selectExpressions.getOrDefault(column, column))
            .collect(Collectors.joining(", "));
        String where = whereClause();
        if (keyColumns.size() == 1) {
            String key = keyColumns.get(0);
            return "SELECT " + list + " FROM " + from() + " WHERE " + key + " > ?" + where + " {since} ORDER BY " + key + " LIMIT ?";
        }
        return "SELECT " + list + " FROM " + from() + " WHERE 1 = 1 {after}" + where + " {since} ORDER BY "
            + String.join(", ", keyColumns) + " LIMIT ?";
    }

    private String buildCount() {
        return "SELECT count(*) FROM " + from() + " WHERE 1 = 1" + whereClause() + " {since}";
    }

    private String buildInsert() {
        String values = columns.stream().map(this::placeholder).collect(Collectors.joining(", "));
        String conflict = conflictTarget.isEmpty()
            ? " ON CONFLICT DO NOTHING"
            : " ON CONFLICT (" + String.join(", ", conflictTarget) + ") DO NOTHING";
        return "INSERT INTO " + name + " (" + String.join(", ", columns) + ") VALUES (" + values + ")" + conflict;
    }

    private String placeholder(String column) {
        return insertPlaceholders.getOrDefault(column, arrayColumns.contains(column) ? ARRAY_PLACEHOLDER : "?");
    }

    private String from() {
        return fromSource != null ? fromSource : sourceTable;
    }

    private String whereClause() {
        return whereSource == null ? "" : " AND (" + whereSource + ")";
    }

    private static String requireIdentifier(String value, String label) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException(label + " is not a valid identifier: " + value);
        }
        return value;
    }

    private static List<String> requireIdentifiers(List<String> values, String label) {
        values.forEach(value -> requireIdentifier(value, label));
        return List.copyOf(values);
    }

    private static List<String> requireColumns(List<String> values, String label) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(label + " must not be empty");
        }
        return requireIdentifiers(values, label);
    }

    private void requireSubset(Iterable<String> values, String label) {
        for (String value : values) {
            if (!columns.contains(value)) {
                throw new IllegalArgumentException(label + " refers to a column not in columns: " + value);
            }
        }
    }
}
