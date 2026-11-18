package com.rentalert.kafka.migration;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.RowMapper;

public final class ColumnRowMapper implements RowMapper<Object[]> {

    private final String[] columns;
    private final boolean[] arrays;

    private ColumnRowMapper(String[] columns, boolean[] arrays) {
        this.columns = columns;
        this.arrays = arrays;
    }

    public static ColumnRowMapper of(List<String> columns, Set<String> arrayColumns) {
        String[] names = columns.toArray(String[]::new);
        boolean[] arrays = new boolean[names.length];
        for (int i = 0; i < names.length; i++) {
            arrays[i] = arrayColumns.contains(names[i]);
        }
        return new ColumnRowMapper(names, arrays);
    }

    @Override
    public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
        Object[] values = new Object[columns.length];
        for (int i = 0; i < columns.length; i++) {
            values[i] = arrays[i] ? rs.getString(columns[i]) : rs.getObject(columns[i]);
        }
        return values;
    }
}
