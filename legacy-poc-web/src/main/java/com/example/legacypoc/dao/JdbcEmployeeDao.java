package com.example.legacypoc.dao;

import com.example.legacypoc.model.Employee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class JdbcEmployeeDao implements EmployeeDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcEmployeeDao.class);
    private static final String COLUMNS =
            "EMPLOYEE_ID, FIRST_NAME, LAST_NAME, EMAIL, DEPARTMENT, CREATED_AT";

    private final JdbcTemplate jdbcTemplate;

    public JdbcEmployeeDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Employee> findAll() {
        return execute("read employees", () -> jdbcTemplate.query(
                "SELECT " + COLUMNS + " FROM EMPLOYEE ORDER BY EMPLOYEE_ID", EMPLOYEE_ROW_MAPPER));
    }

    @Override
    public Employee findById(Long employeeId) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT " + COLUMNS + " FROM EMPLOYEE WHERE EMPLOYEE_ID = ?",
                    EMPLOYEE_ROW_MAPPER, employeeId);
        } catch (EmptyResultDataAccessException exception) {
            return null;
        } catch (DataAccessException exception) {
            LOGGER.error("Database operation failed while reading employee {}", employeeId, exception);
            throw exception;
        }
    }

    @Override
    public int insert(Employee employee) {
        return execute("create employee", () -> jdbcTemplate.update(
                "INSERT INTO EMPLOYEE (FIRST_NAME, LAST_NAME, EMAIL, DEPARTMENT) VALUES (?, ?, ?, ?)",
                employee.getFirstName(), employee.getLastName(), employee.getEmail(), employee.getDepartment()));
    }

    @Override
    public int update(Employee employee) {
        return execute("update employee", () -> jdbcTemplate.update(
                "UPDATE EMPLOYEE SET FIRST_NAME = ?, LAST_NAME = ?, EMAIL = ?, DEPARTMENT = ? WHERE EMPLOYEE_ID = ?",
                employee.getFirstName(), employee.getLastName(), employee.getEmail(), employee.getDepartment(),
                employee.getEmployeeId()));
    }

    @Override
    public int delete(Long employeeId) {
        return execute("delete employee", () -> jdbcTemplate.update(
                "DELETE FROM EMPLOYEE WHERE EMPLOYEE_ID = ?", employeeId));
    }

    @Override
    public boolean emailExists(String email) {
        return count("SELECT COUNT(*) FROM EMPLOYEE WHERE LOWER(EMAIL) = LOWER(?)", email) > 0;
    }

    @Override
    public boolean emailExistsForOtherEmployee(String email, Long employeeId) {
        return count("SELECT COUNT(*) FROM EMPLOYEE WHERE LOWER(EMAIL) = LOWER(?) AND EMPLOYEE_ID <> ?",
                email, employeeId) > 0;
    }

    private int count(String sql, Object... arguments) {
        Integer result = execute("check employee email", () ->
                jdbcTemplate.queryForObject(sql, Integer.class, arguments));
        return result == null ? 0 : result;
    }

    private <T> T execute(String operation, DataAccessSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (DataAccessException exception) {
            LOGGER.error("Database operation failed while attempting to {}", operation, exception);
            throw exception;
        }
    }

    private interface DataAccessSupplier<T> {
        T get();
    }

    private static final RowMapper<Employee> EMPLOYEE_ROW_MAPPER = new RowMapper<Employee>() {
        @Override
        public Employee mapRow(ResultSet resultSet, int rowNumber) throws SQLException {
            Employee employee = new Employee();
            employee.setEmployeeId(resultSet.getLong("EMPLOYEE_ID"));
            employee.setFirstName(resultSet.getString("FIRST_NAME"));
            employee.setLastName(resultSet.getString("LAST_NAME"));
            employee.setEmail(resultSet.getString("EMAIL"));
            employee.setDepartment(resultSet.getString("DEPARTMENT"));
            employee.setCreatedAt(resultSet.getTimestamp("CREATED_AT"));
            return employee;
        }
    };
}
