package com.bookstore.dao;

import com.bookstore.entity.Employee;
import com.bookstore.entity.EmployeePhone;
import com.bookstore.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Employee and EmployeePhone entity operations.
 */
public class EmployeeDao {

    private static final Logger logger = LoggerFactory.getLogger(EmployeeDao.class);

    public Employee save(Employee employee) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            session.persist(employee);
            tx.commit();
            return employee;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to save employee: " + e.getMessage(), e);
            throw new RuntimeException("Error saving employee", e);
        }
    }

    public Optional<Employee> findById(Long id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return findById(session, id);
        }
    }

    public Optional<Employee> findById(Session session, Long id) {
        return Optional.ofNullable(session.get(Employee.class, id));
    }

    public List<Employee> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Employee e ORDER BY e.employeeId ASC", Employee.class)
                    .getResultList();
        }
    }

    public void addPhone(Long employeeId, String phoneNo) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            Employee employee = session.get(Employee.class, employeeId);
            if (employee != null) {
                EmployeePhone phone = new EmployeePhone(employee, phoneNo);
                session.persist(phone);
            }
            tx.commit();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            logger.error("Failed to add phone for employee " + employeeId + ": " + e.getMessage(), e);
            throw new RuntimeException("Error adding employee phone", e);
        }
    }

    public List<String> getPhones(Long employeeId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                    "SELECT ep.id.phoneNo FROM EmployeePhone ep WHERE ep.employee.employeeId = :eId", String.class)
                    .setParameter("eId", employeeId)
                    .getResultList();
        }
    }
}
